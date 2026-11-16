package com.senghol.knotx.connection

import android.os.Handler
import android.os.Looper
import com.senghol.knotx.BuildConfig
import com.senghol.knotx.utils.objects.DLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit


// ─────────────────────────────────────────────
// EVENTS
// ─────────────────────────────────────────────
sealed class ConnectionEvent {
    object Open : ConnectionEvent()
    data class Message(val text: String) : ConnectionEvent()
    data class Closed(val code: Int, val reason: String) : ConnectionEvent()
    data class Failure(val throwable: Throwable) : ConnectionEvent()
}

// ─────────────────────────────────────────────
// SOCKET MANAGER
// ─────────────────────────────────────────────
object SocketManager {

    // ── OkHttp client ──────────────────────────
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // We handle retry ourselves
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // ── State ───────────────────────────────────
    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var socketGeneration = 0L
    @Volatile private var isConnecting = false
    @Volatile private var isSocketOpen = false
    @Volatile private var isReconnecting = false

    // Controlled by app lifecycle (ProcessLifecycleOwner)
    @Volatile private var isAppActive = false

    // Controlled by auth/permission logic (login/logout)
    @Volatile private var allowConnection = true

    // External task guards — prevent disconnect while these are running
    var isLocationRunning: Boolean = false
    var isCallRunning: Boolean = false

    private var retryCount = 0
    private const val maxRetryCount = 2               // ~63 s total before capping

    private val handler = Handler(Looper.getMainLooper())
    private val connectionLock = Any()

    // ── Public event stream ─────────────────────
    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    // ─────────────────────────────────────────────
    // PUBLIC CONTROL API
    // ─────────────────────────────────────────────

    /**
     * Call this when the user logs in (allowed = true)
     * or logs out / is not permitted to connect (allowed = false).
     */
    fun setConnectionAllowed(allowed: Boolean) {
        allowConnection = allowed

        if (!allowed) {
            DLog.i("🚫 Connection disabled → killing socket")
            disconnectInternal(reason = "Connection disabled")
        } else {
            DLog.i("✅ Connection enabled")
            if (isAppActive) connect()
        }
    }

    /**
     * Driven by ProcessLifecycleOwner ON_START / ON_STOP.
     * The caller owns the 40-second disconnect timer; we just
     * react to the final active=false call here.
     */
    fun setAppActive(active: Boolean) {


        if (active) {
            isAppActive = true
            connect()
        } else {
            if (isLocationRunning || isCallRunning) {
                DLog.w("⚠️ Skip disconnect → location=$isLocationRunning call=$isCallRunning")
                return
            }
            isAppActive = false
            disconnectInternal(reason = "App background")
        }
    }

    /**
     * Called on app foreground. If we were away for a while, the socket may be
     * dead without us knowing, so drop it and open a fresh one.
     */
    fun onForeground(awayMs: Long) {
        DLog.i(
            "onForeground awayMs=$awayMs open=$isSocketOpen connecting=$isConnecting " +
                    "reconnecting=$isReconnecting retry=$retryCount allow=$allowConnection"
        )
        isAppActive = true
        if (awayMs > 5_000L) {
            forceReconnect()   // also clears any stuck flags
        } else {
            connect()
        }
    }

    /**
     * Network was lost entirely. Kill the socket silently (generation bump
     * suppresses retry loops); NetworkMonitor will trigger a fresh connect
     * when a validated network returns.
     */
    fun onNetworkLost() {
        synchronized(connectionLock) {
            socketGeneration++
            isConnecting = false
            isSocketOpen = false
            isReconnecting = false
            clearSocket()
        }
    }

    /**
     * Default network changed (Wi-Fi <-> mobile) or came back after a loss.
     * The old socket is unusable, so open a fresh one.
     */
    fun onNetworkChanged() {
        if (!allowConnection || !isAppActive) return
        forceReconnect()
    }

    /**
     * Called by NetworkMonitor when a validated network becomes available.
     * Safe to call at any time — no-ops if preconditions aren't met.
     */
    fun ensureConnected() {
        DLog.d(
            "ensureConnected() → allow=$allowConnection active=$isAppActive " +
                    "open=$isSocketOpen connecting=$isConnecting"
        )
        if (!allowConnection || !isAppActive) return
        connect()
    }

    /**
     * Tear down current socket and immediately open a fresh one.
     * Useful after auth token refresh or server-side session reset.
     */
    fun forceReconnect() {
        DLog.i("🔄 forceReconnect()")

        synchronized(connectionLock) {
            socketGeneration++
            isConnecting = false
            isSocketOpen = false
            isReconnecting = false
            webSocket?.close(1000, "Force reconnect")
            clearSocket()
        }

        if (!allowConnection || !isAppActive) return
        retryCount = 0
        connect()
    }

    /**
     * Send a text message. Returns false if the socket is not ready.
     */
    fun send(message: String): Boolean {
        return allowConnection && webSocket?.send(message) ?: false
    }

    // ─────────────────────────────────────────────
    // INTERNAL – CONNECT
    // ─────────────────────────────────────────────

    fun connect() {
        if (!isAppActive) {
            DLog.d("connect() blocked: isAppActive=false")
            return
        }
        if (!allowConnection) {
            DLog.d("connect() blocked: allowConnection=false")
            return
        }

        val generation: Long

        synchronized(connectionLock) {
            if (isSocketOpen) {
                DLog.d("connect() no-op: socket already OPEN")
                return
            }
            if (isConnecting) {
                DLog.d("connect() no-op: already CONNECTING")
                return
            }

            generation = ++socketGeneration
            isConnecting = true
            clearSocket()                         // cancel any stale socket
        }

        DLog.i("🔌 Opening WebSocket (gen=$generation)")

        val request = Request.Builder()
            .url(BuildConfig.WS_URL + "/ws")
            .build()

        val ws = client.newWebSocket(request, createListener(generation))

        // ── Connect timeout watchdog ─────────────
        handler.postDelayed({
            synchronized(connectionLock) {
                if (generation == socketGeneration && isConnecting && !isSocketOpen) {
                    DLog.w("⏱ WS connect timeout (gen=$generation)")
                    isConnecting = false
                    ws.cancel()
                    scheduleReconnect()
                }
            }
        }, 15_000L)
    }

    // ─────────────────────────────────────────────
    // INTERNAL – DISCONNECT
    // ─────────────────────────────────────────────

    private fun disconnectInternal(reason: String) {
        synchronized(connectionLock) {
            socketGeneration++
            isConnecting = false
            isSocketOpen = false
            isReconnecting = false
            retryCount = 0
            DLog.i("🔒 disconnectInternal: $reason")
            webSocket?.close(1000, reason)
            clearSocket()
        }
    }

    // ─────────────────────────────────────────────
    // INTERNAL – WEBSOCKET LISTENER
    // ─────────────────────────────────────────────

    private fun createListener(generation: Long) = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!isGenerationCurrent(generation, webSocket)) return

            if (!allowConnection) {
                webSocket.close(1000, "Blocked after open")
                return
            }

            synchronized(connectionLock) {
                isSocketOpen = true
                isConnecting = false
                this@SocketManager.webSocket = webSocket
                retryCount = 0
            }

            DLog.i("✅ WebSocket OPEN (gen=$generation)")
            _events.tryEmit(ConnectionEvent.Open)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!isGenerationCurrent(generation, webSocket)) return
            _events.tryEmit(ConnectionEvent.Message(text))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            DLog.w("❌ WebSocket FAILURE: ${t.message}")
            _events.tryEmit(ConnectionEvent.Failure(t))

            // Emit event first, then guard generation
            if (!isGenerationCurrent(generation, webSocket)) return

            synchronized(connectionLock) {
                isSocketOpen = false
                isConnecting = false
                clearSocket()
            }

            scheduleReconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)   // complete the close handshake
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            DLog.i("🔐 WebSocket CLOSED: code=$code reason=$reason")
            _events.tryEmit(ConnectionEvent.Closed(code, reason))

            if (!isGenerationCurrent(generation, webSocket)) return

            synchronized(connectionLock) {
                isSocketOpen = false
                isConnecting = false
                clearSocket()
            }

            // Our own closes bump socketGeneration first, so they never reach here.
            // Anything that does reach here was closed by the server or network.
            scheduleReconnect()
        }
    }

    // ─────────────────────────────────────────────
    // INTERNAL – RECONNECT (exponential backoff)
    // ─────────────────────────────────────────────

    private fun scheduleReconnect() {

        DLog.i("📡 scheduleReconnect() called")

        DLog.i(
            "➡️ appActive=$isAppActive " +
                    "allowConnection=$allowConnection " +
                    "isConnecting=$isConnecting " +
                    "isReconnecting=$isReconnecting " +
                    "retryCount=$retryCount"
        )

        if (!isAppActive || !allowConnection) {
            DLog.w("⛔ Reconnect blocked -> app inactive or connection not allowed")
            return
        }

        synchronized(connectionLock) {

            DLog.i("🔒 Entered reconnect lock")

            if (isConnecting) {
                DLog.w("⚠️ Reconnect skipped -> already connecting")
                return
            }

            if (isReconnecting) {
                DLog.w("⚠️ Reconnect skipped -> reconnect already scheduled")
                return
            }

            isReconnecting = true

            DLog.i("✅ Marked reconnecting=true")
        }

        val currentGeneration = socketGeneration

        DLog.i("🧬 Current socket generation: $currentGeneration")

        // Exponential backoff: 1s, 2s, 4s, 8s … capped at 32s
        val delay = minOf(
            1000L * (1L shl minOf(retryCount, maxRetryCount)),
            32_000L
        )

        DLog.i("⏳ Reconnect in ${delay}ms (attempt ${retryCount + 1})")

        handler.postDelayed({

            DLog.i("⏰ Reconnect timer fired")

            synchronized(connectionLock) {

                DLog.i(
                    "🔍 Checking generation -> current=$currentGeneration latest=$socketGeneration"
                )

                if (currentGeneration != socketGeneration) {

                    DLog.w("⚠️ Reconnect cancelled -> stale generation")

                    isReconnecting = false

                    return@postDelayed
                }

                isReconnecting = false

                DLog.i("✅ Cleared reconnecting flag")
            }

            retryCount++

            DLog.i("🔁 Starting reconnect attempt #$retryCount")

            connect()

        }, delay)
    }
    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    /** Cancel and null the stored socket reference — must be called inside lock. */
    private fun clearSocket() {
        try { webSocket?.cancel() } catch (_: Exception) {}
        webSocket = null
    }

    /**
     * Returns true if this listener's generation is still the active one.
     * Cancels the stale socket as a side effect if not.
     */
    private fun isGenerationCurrent(generation: Long, webSocket: WebSocket): Boolean {
        return if (generation != socketGeneration) {
            webSocket.cancel()
            false
        } else {
            true
        }
    }
}