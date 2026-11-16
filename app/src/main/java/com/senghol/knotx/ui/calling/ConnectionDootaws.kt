package com.senghol.knotx.ui.calling

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.OutcomeReceiver
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Connection
import android.telecom.Connection.CAPABILITY_HOLD
import android.telecom.Connection.CAPABILITY_SUPPORT_HOLD
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.DisconnectCause


import android.telecom.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.senghol.knotx.utils.objects.EndTone
import com.senghol.knotx.utils.objects.OutgoingTone
import java.util.UUID
import java.util.concurrent.Executors

class ConnectionDootaws : ConnectionService() {

    companion object {


        // In ConnectionDootaws companion object
        private var endpointChangeCallback: ((List<CallEndpoint>, CallEndpoint?) -> Unit)? = null

        fun setEndpointChangeCallback(cb: (List<CallEndpoint>, CallEndpoint?) -> Unit) {
            endpointChangeCallback = cb
        }

        fun clearEndpointChangeCallback() {
            endpointChangeCallback = null
        }
        // Public read-only current connection
        var currentConnection: Connection? = null
            private set

        // Private backing field for state listener
        private var _stateListener: ((Int) -> Unit)? = null

        // Public read-only access to listener
        val stateListener: ((Int) -> Unit)? get() = _stateListener

        private val availableEndpoints = mutableListOf<CallEndpoint>()
        private var currentEndpoint: CallEndpoint? = null


        private var ringtone: Ringtone? = null

        fun playRingtone(context: Context) {
            if (ringtone != null) return

            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

            val uri = "android.resource://${context.packageName}/raw/bakwas_ringtone_dagar".toUri()

            val rt = RingtoneManager.getRingtone(context, uri)

            rt.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)  // 🔥 IMPORTANT
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_RING)          // 🔥 CRITICAL
                .build()

            rt.isLooping = true
            rt.play()

            ringtone = rt
        }

        private var vibrator: Vibrator? = null

        fun startVibration(context: Context) {
            val audioManager =
                context.getSystemService(AUDIO_SERVICE) as AudioManager

            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

            val pattern = longArrayOf(0, 1000, 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm =
                    context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vm.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator =
                    context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }

        private var toneGenerator: ToneGenerator? = null
        private var ringHandler: Handler? = null
        private var ringRunnable: Runnable? = null


        @RequiresApi(Build.VERSION_CODES.S)
        fun startOutgoingTone() {
            if (toneGenerator != null) return



            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            ringHandler = Handler(Looper.getMainLooper())

            ringRunnable = object : Runnable {
                override fun run() {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 2000)
                    ringHandler?.postDelayed(this, 3000)
                }
            }

            ringHandler?.post(ringRunnable!!)
        }
        fun stopOutgoingTone() {
            ringHandler?.removeCallbacksAndMessages(null)
            ringHandler = null
            ringRunnable = null

            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        }

        fun playCallEndTone() {

            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
                toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)

                Handler(Looper.getMainLooper()).postDelayed({
                    toneGen.release()
                }, 200)

            } catch (e: Exception) {
                Log.e("VOIP", "Failed to play end tone", e)
            }
        }

        fun stopVibration() {
            vibrator?.cancel()
            vibrator = null
        }


        fun stopRingtone() {
            val caller = Thread.currentThread().stackTrace
                .drop(3) // skip getStackTrace, currentThread, stopRingtone itself
                .take(3) // show top 3 frames for context
                .joinToString("\n") { "  at ${it.className.substringAfterLast('.')}.${it.methodName}(${it.fileName}:${it.lineNumber})" }

            ringtone?.stop()
            ringtone = null
            vibrator?.cancel()
            vibrator = null
        }

        fun getAvailableEndpoints(): List<CallEndpoint> = availableEndpoints
        fun getCurrentEndpoint(): CallEndpoint? = currentEndpoint

        // Safe public setters
        fun updateConnection(connection: Connection?) {
            currentConnection = connection
        }

        fun setStateListener(listener: (Int) -> Unit) {
            
            _stateListener = listener
            currentConnection?.state?.let { listener(it) }
        }

        fun clearAll() {
            stopRingtone()
            stopVibration()
            OutgoingTone.stop()
            currentConnection = null
            _stateListener = null
        }
        fun endCallRemotely() {
            currentConnection?.apply {
                setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                destroy()
            }
            _stateListener?.invoke(Connection.STATE_DISCONNECTED)
            currentConnection = null
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {

        val extras = request.extras
            ?.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)
            ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR)
            )

        val callerName = extras.getString("CALLER_NAME") ?: "Unknown"
        val roomId = extras.getString("ROOM_ID") ?: "Unknown"
        val isVideoCall = extras.getBoolean("IS_VIDEO_CALL", false)
        val outgoing = extras.getBoolean("IS_OUTGOING", false)
        val userId = extras.getString("USER_ID")
            ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR)
            )

        playRingtone(applicationContext)
        startVibration(applicationContext)
        // ✅ UPDATED: Decide UI vs Notification
        startIncomingCallUI(
            context = this,
            userId = userId,
            isVideo = isVideoCall,
            outgoing = false,
            callerName = callerName,
            roomId= roomId

        )

        // Create ringing connection
        val connection = VoipConnection(isVideoCall, roomId, applicationContext).apply {
            connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD
            setRinging()
            setAudioModeIsVoip(true)
        }

        updateConnection(connection)
        stateListener?.invoke(Connection.STATE_RINGING)

        return connection
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateOutgoingConnection(
        phoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {



        val extras = request.extras

        val isVideoCall = extras?.getBoolean("IS_VIDEO_CALL") ?: false
        val userId = request.address?.schemeSpecificPart ?: "Unknown"

        val roomId = request.extras?.getString("ROOM_ID") ?: UUID.randomUUID().toString()
        val connection = VoipConnection(
            isVideoCall = isVideoCall,
            roomId = roomId,
            context = applicationContext
        ).apply {
            connectionCapabilities =
                CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD

            setDialing() // 🔥 REQUIRED
        }

        updateConnection(connection)
        stateListener?.invoke(Connection.STATE_DIALING)
// 🔥 START outgoing tone

//        OutgoingTone.startConnecting(applicationContext)
        return connection
    }

    // ---------------- STATE CHECKS ----------------

    private fun isAppInForeground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = am.runningAppProcesses ?: return false
        val pkg = context.packageName

        return processes.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == pkg
        }
    }

    private fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isKeyguardLocked
    }

    // ---------------- UI DECISION ----------------
    private fun startCallActivity(
        userId: String,
        isVideo: Boolean,
        outgoing: Boolean,
        callerName: String
    ) {
        val intent = Intent(this, CallActivity2::class.java).apply {
            putExtra("userNumber", userId)
            putExtra("video", isVideo)
            putExtra("outgoing", outgoing)
            putExtra("callerName", callerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("VOIP", "Failed to start CallActivity2", e)
        }
    }
    private fun startIncomingCallUI(
        context: Context,
        userId: String,
        isVideo: Boolean,
        outgoing: Boolean,
        callerName: String,
        roomId: String
    ) {
        val locked = isDeviceLocked(context)
        val foreground = isAppInForeground(context)

        if (locked || !foreground) {

            IncomingCallForegroundService.start(
                context = context.applicationContext,
                callerName = callerName,
                isVideoCall = isVideo,
                userId = userId,
                roomId = roomId
            )
        } else {

            startCallActivity(
                userId = userId,
                isVideo = isVideo,
                outgoing = outgoing,
                callerName = callerName
            )
        }
    }
    interface CallEndpointListener {
        fun onAvailableEndpointsChanged(endpoints: List<CallEndpoint>)
        fun onCurrentEndpointChanged(endpoint: CallEndpoint)
    }

    class VoipConnection(
        private val isVideoCall: Boolean,
        private val roomId: String,
        private val context: Context
    ) : Connection(){

        private var defaultRouteApplied = false

        override fun onAnswer() {

            if (Build.VERSION.SDK_INT >= 34) {
                setActive()
            }


            stopRingtone()
            stopVibration()
            OutgoingTone.stop()
            // 🔥 CLEANUP
            IncomingCallForegroundService.cancelCallNotification(context, roomId)
            IncomingCallForegroundService.stop(context)

//            applyDefaultRouteIfNeeded()
            stateListener?.invoke(STATE_ACTIVE)
        }

        override fun onSilence() {
            super.onSilence()
            stopRingtone()
            stopVibration()
            OutgoingTone.stop()
        }

        override fun onStateChanged(state: Int) {
            super.onStateChanged(state)

            when (state) {
                STATE_ACTIVE -> {
                    stopRingtone()
                    stopVibration()
                    OutgoingTone.stop()
                    IncomingCallForegroundService.cancelCallNotification(context, roomId)
                    IncomingCallForegroundService.stop(context)
                }
                STATE_DISCONNECTED -> {
                    stopRingtone()
                    stopVibration()
                    OutgoingTone.stop()
                }
                STATE_RINGING -> { /* keep tones playing */ }
                else -> {
                    stopRingtone()
                    OutgoingTone.stop()
                }
            }
        }

        override fun onAnswer(videoState: Int) {
            stopRingtone()
            stopVibration()

            setVideoState(videoState)
            OutgoingTone.stop()

            IncomingCallForegroundService.cancelCallNotification(context, roomId)
            IncomingCallForegroundService.stop(context)
            if (Build.VERSION.SDK_INT >= 34) {
                setActive()
            }

//            applyDefaultRouteIfNeeded()
            stateListener?.invoke(STATE_ACTIVE)
        }


        override fun onReject() {

            stopRingtone()
            stopVibration()

            defaultRouteApplied = false
            OutgoingTone.stop()

//            CallTone.Play.end()
            // 🔥 CLEANUP
            IncomingCallForegroundService.cancelCallNotification(context, roomId)
            IncomingCallForegroundService.stop(context)

            setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            destroy()

            stateListener?.invoke(STATE_DISCONNECTED)
            updateConnection(null)
        }

        override fun onDisconnect() {

            defaultRouteApplied = false
            stopRingtone()
            stopVibration()

            OutgoingTone.stop()
            EndTone.play()
            // 🔥 CLEANUP
            IncomingCallForegroundService.cancelCallNotification(context, roomId)
            IncomingCallForegroundService.stop(context)

            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()

            stateListener?.invoke(STATE_DISCONNECTED)
            updateConnection(null)
        }

        override fun onHold() {
            setOnHold()
            stateListener?.invoke(STATE_HOLDING)
        }

        override fun onUnhold() {
            setActive()
            stateListener?.invoke(STATE_ACTIVE)
        }


        var endpointListener: CallEndpointListener? = null

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
            availableEndpoints.clear()
            availableEndpoints.addAll(endpoints)
            endpointChangeCallback?.invoke(endpoints, currentEndpoint)
            applyDefaultRouteIfNeeded()
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        override fun onCallEndpointChanged(endpoint: CallEndpoint) {
            currentEndpoint = endpoint
            endpointChangeCallback?.invoke(availableEndpoints, endpoint)
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun applyDefaultRouteIfNeeded() {
            if (defaultRouteApplied) return
            if (availableEndpoints.isEmpty()) return
            val targetType =
                if (isVideoCall) CallEndpoint.TYPE_SPEAKER
                else CallEndpoint.TYPE_EARPIECE
            val endpoint = availableEndpoints
                .firstOrNull { it.endpointType == targetType }
            if (endpoint == null) {
                Log.w("VOIP_DR", "❌ Target endpoint not found yet")
                return
            }
            // 🔥 Only override if needed
            if (currentEndpoint?.endpointType == targetType) {
                defaultRouteApplied = true
                return
            }
            requestCallEndpointChange(
                endpoint,
                Executors.newSingleThreadExecutor(),
                object : OutcomeReceiver<Void, CallEndpointException> {
                    override fun onResult(result: Void?) {
                        defaultRouteApplied = true
                    }

                    override fun onError(error: CallEndpointException) {
                        Log.e("VOIP_DR", "❌ Route failed", error)
                        defaultRouteApplied = false
                    }
                }
            )
        }
    }
}


