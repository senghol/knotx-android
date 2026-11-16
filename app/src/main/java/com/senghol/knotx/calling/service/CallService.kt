package com.senghol.knotx.calling.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.R
import com.senghol.knotx.calling.data.signaling.CallSignalingManager
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import com.senghol.knotx.calling.domain.manager.CallManager
import com.senghol.knotx.calling.domain.manager.CallMediaManager
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.repository.CallParticipantRepository
import com.senghol.knotx.calling.ui.CallActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// COPY-PASTE REPLACEMENT: keep your `package` line and imports, and ADD:
//
//   import android.Manifest
//   import android.content.pm.PackageManager
//   import android.content.pm.ServiceInfo
//   import androidx.core.content.ContextCompat
//   import kotlinx.coroutines.flow.distinctUntilChanged
//   import kotlinx.coroutines.flow.map
//
// MANIFEST (required, see the notes in the chat):
//
//   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
//   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
//   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
//
//   <service
//       android:name=".calling.service.CallService"   <- your real name
//       android:exported="false"
//       android:foregroundServiceType="microphone|camera" />
//
// What changed:
//  1. startForeground() is ALWAYS called after startForegroundService(), even
//     when there is no call (then it stops right away). Before, an early
//     `return` crashed the app with "did not call startForeground()".
//  2. the call observer no longer stops the service before it ever became a
//     foreground service (its first value is often null)
//  3. foreground service TYPES (microphone, + camera for video) so Android 14+
//     keeps the mic/camera alive in the background
//  4. new SILENT, low-importance channel for the ongoing-call notification, so
//     it no longer dings on every state change (the old channel had the default
//     sound and is deleted)
//  5. the notification only updates when call TYPE or STATE changes (not for
//     mic / camera / participant changes), and not for terminal states (no flicker, and no zombie
//     notification after the service stopped)

// COPY-PASTE REPLACEMENT: keep your `package` line and imports, and ADD:
//
//   import android.Manifest
//   import android.content.pm.PackageManager
//   import android.content.pm.ServiceInfo
//   import android.util.Log
//   import androidx.annotation.RequiresApi
//   import com.senghol.knotx.calling.data.webrtc.WebRtcManager
//   import com.senghol.knotx.calling.domain.manager.CallMediaManager
//   import androidx.core.content.ContextCompat
//   import kotlinx.coroutines.flow.distinctUntilChanged
//   import kotlinx.coroutines.flow.map
//
// MANIFEST (required, see the notes in the chat):
//
//   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
//   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
//   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
//
//   <service
//       android:name=".calling.service.CallService"   <- your real name
//       android:exported="false"
//       android:foregroundServiceType="microphone|camera" />
//
// SCREEN SHARING additions:
//  - manifest: android:foregroundServiceType="microphone|camera|mediaProjection"
//    and <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
//  - ACTION_START_SCREEN_SHARE: upgrades this service to type mediaProjection
//    FIRST (Android 10+ requires it, Android 14 enforces it), THEN starts the
//    capture. ACTION_STOP_SCREEN_SHARE stops it. When sharing ends the service
//    drops back to microphone/camera.
//  - extra imports: kotlinx.coroutines.flow.combine
//
// What changed:
//  1. startForeground() is ALWAYS called after startForegroundService(), even
//     when there is no call (then it stops right away). Before, an early
//     `return` crashed the app with "did not call startForeground()".
//  2. the call observer no longer stops the service before it ever became a
//     foreground service (its first value is often null)
//  3. foreground service TYPES (microphone, + camera for video) so Android 14+
//     keeps the mic/camera alive in the background
//  4. new SILENT, low-importance channel for the ongoing-call notification, so
//     it no longer dings on every state change (the old channel had the default
//     sound and is deleted)
//  5. the notification only updates when call TYPE or STATE changes (not for
//     mic / camera / participant changes), and not for terminal states (no flicker, and no zombie
//     notification after the service stopped)

class CallService : LifecycleService() {

    private lateinit var callManager: CallManager
    private lateinit var callMediaManager: CallMediaManager
    private lateinit var webRtcManager: WebRtcManager
    private lateinit var participantRepository: CallParticipantRepository
    private lateinit var signalingManager: CallSignalingManager

    // Shown in the ongoing-call notification (name + running timer)
    private var callerName: String = "Call"
    private var answeredAt: Long? = null
    private var resolvedCallId: String? = null

    private data class CallInfo(
        val callType: CallType,
        val state: CallState,
        val answeredAt: Long?
    )

    private var callStateJob: Job? = null

    // true only between startForeground() and stopCallRuntime()
    private var isForeground = false

    // true while the foreground service type includes mediaProjection
    private var projectionActive = false

    override fun onCreate() {
        super.onCreate()

        val container = (application as KnotxApp).callContainer

        callManager = container.callManager
        callMediaManager = container.callMediaManager
        webRtcManager = container.webRtcManager
        participantRepository = container.callParticipantRepository
        signalingManager = container.callSignalingManager

        createNotificationChannel()
        observeCallState()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_CALL -> startCallRuntime()
            ACTION_STOP_CALL -> stopCallRuntime()
            ACTION_HANGUP -> hangUpFromNotification()
            ACTION_START_SCREEN_SHARE -> startScreenShare(readProjectionData(intent))
            ACTION_STOP_SCREEN_SHARE -> callMediaManager.stopScreenShare()
        }

        return START_NOT_STICKY
    }

    private fun startCallRuntime() {
        val call = callManager.activeCall.value

        if (call == null) {
            // startForegroundService() PROMISES startForeground(). Keep the
            // promise, then stop: the call is already gone.
            enterForeground(
                buildNotification(CallType.VOICE, CallState.CONNECTING),
                CallType.VOICE
            )
            stopCallRuntime()
            return
        }

        enterForeground(
            buildNotification(call.callType, call.state),
            call.callType
        )
    }

    private fun enterForeground(
        notification: Notification,
        callType: CallType,
        screenSharing: Boolean = false
    ) {
        // FOREGROUND_SERVICE_TYPE_MICROPHONE / CAMERA only exist from API 30.
        // API 29 can still declare mediaProjection (needed for screen sharing).
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    foregroundServiceTypes(callType, screenSharing)
                )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && screenSharing ->
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )

            else -> startForeground(NOTIFICATION_ID, notification)
        }

        isForeground = true
        projectionActive = screenSharing
    }

    private fun startScreenShare(permissionData: Intent?) {
        val call = callManager.activeCall.value

        if (permissionData == null || call == null || !isForeground) {
            Log.w("CallService", "screen share ignored: data=$permissionData call=$call fg=$isForeground")
            return
        }

        // 1) become a mediaProjection foreground service BEFORE capturing
        enterForeground(
            buildNotification(call.callType, call.state, sharing = true),
            call.callType,
            screenSharing = true
        )

        // 2) start the capture
        if (!callMediaManager.startScreenShare(permissionData)) {
            Log.e("CallService", "screen capture failed to start")

            enterForeground(
                buildNotification(call.callType, call.state, sharing = false),
                call.callType,
                screenSharing = false
            )
        }
    }

    private fun readProjectionData(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }

    /**
     * microphone always, camera too for video calls. Only types whose runtime
     * permission is granted may be declared (otherwise Android 14 throws).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun foregroundServiceTypes(
        callType: CallType,
        screenSharing: Boolean
    ): Int {
        var types = 0

        if (screenSharing) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        }

        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        if (callType == CallType.VIDEO && hasPermission(Manifest.permission.CAMERA)) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        }

        // nothing granted: fall back to whatever the manifest declares
        return if (types != 0) types else ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED

    private fun observeCallState() {
        callStateJob?.cancel()

        callStateJob = lifecycleScope.launch {
            combine(
                // activeCall also emits for mic / camera / participant changes.
                // The notification only shows type + state (+ sharing), so react
                // to those only.
                callManager.activeCall.map { call ->
                    call?.let { CallInfo(it.callType, it.state, it.answeredAt) }
                },
                webRtcManager.screenSharing
            ) { info, sharing -> info to sharing }
                .distinctUntilChanged()
                .collect { (info, sharing) ->

                    if (info == null) {
                        // The first value is often null (service created before
                        // the call exists). Only stop if we were actually running.
                        if (isForeground) stopCallRuntime()
                        return@collect
                    }

                    val callType = info.callType
                    val state = info.state
                    answeredAt = info.answeredAt
                    refreshCallerName()

                    when (state) {
                        CallState.ENDED,
                        CallState.REJECTED,
                        CallState.CANCELLED,
                        CallState.BUSY,
                        CallState.MISSED,
                        CallState.FAILED -> stopCallRuntime()

                        else -> if (isForeground) {
                            if (!sharing && projectionActive) {
                                // sharing ended: drop the mediaProjection type
                                enterForeground(
                                    buildNotification(callType, state, sharing = false),
                                    callType,
                                    screenSharing = false
                                )
                            } else {
                                updateNotification(callType, state, sharing)
                            }
                        }
                    }
                }
        }
    }

    // Contact name for the notification. First the raw participant, then the
    // resolved contact name (re-posts the notification when it arrives).
    private fun refreshCallerName() {
        val call = callManager.activeCall.value ?: return

        if (resolvedCallId == call.callId.value) return
        resolvedCallId = call.callId.value

        val participant = call.participants.firstOrNull() ?: return

        callerName = participant.displayName?.takeIf { it.isNotBlank() }
            ?: participant.userNameId?.takeIf { it.isNotBlank() }
                    ?: participant.userNumber
                    ?: "Call"

        lifecycleScope.launch {
            val resolved = runCatching {
                participantRepository.resolveParticipant(participant)
            }.getOrNull()

            val name = resolved?.displayName?.takeIf { it.isNotBlank() }
                ?: return@launch

            callerName = name

            val current = callManager.activeCall.value ?: return@launch

            if (isForeground) {
                updateNotification(
                    current.callType,
                    current.state,
                    webRtcManager.screenSharing.value
                )
            }
        }
    }

    // "Hang up" button on the notification / status-bar call chip.
    private fun hangUpFromNotification() {
        lifecycleScope.launch {
            val call = callManager.activeCall.value ?: return@launch

            if (!callManager.endCall()) return@launch

            call.participants.forEach { participant ->
                signalingManager.sendCallEnd(
                    callId = call.callId.value,
                    target = participant.userNumber
                )
            }
            // mic / camera / audio teardown: CallSignalingManager.observeCallLifecycle()
        }
    }

    private fun stopCallRuntime() {
        isForeground = false
        projectionActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(
        callType: CallType,
        state: CallState,
        sharing: Boolean
    ) {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            buildNotification(
                callType,
                state,
                sharing
            )
        )
    }

    private fun buildNotification(
        callType: CallType,
        state: CallState,
        sharing: Boolean = false
    ): Notification {
        val title = when (callType) {
            CallType.VOICE -> "Voice call"
            CallType.VIDEO -> "Video call"
        }

        val text = when (state) {
            CallState.RINGING -> "Incoming call"
            CallState.INITIATING -> "Calling"
            CallState.ACCEPTING,
            CallState.CONNECTING -> "Connecting"
            CallState.CONNECTED ->
                if (sharing) "Sharing your screen" else "Call in progress"
            CallState.RECONNECTING -> "Reconnecting"
            else -> "Call"
        }

        val callActivityIntent = Intent(
            this,
            CallActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            callActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val hangUpPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            Intent(this, CallService::class.java).apply {
                action = ACTION_HANGUP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // Person.setName() rejects empty strings
        val caller = Person.Builder()
            .setName(callerName.ifBlank { "Call" })
            .setImportant(true)
            .build()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.call_24px)
            .setContentTitle(callerName)
            .setContentText(text)
            .setSubText(title)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Ongoing-call style: lets the system show the status-bar call chip.
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    caller,
                    hangUpPendingIntent
                )
            )

        // The system keeps the timer ticking by itself.
        val connectedAt = answeredAt

        if (connectedAt != null &&
            (state == CallState.CONNECTED || state == CallState.RECONNECTING)
        ) {
            builder
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(connectedAt)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // The old channel was IMPORTANCE_HIGH with the default sound, and a
        // channel's sound can't be changed after creation.
        manager.deleteNotificationChannel("dootawas_calls")

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ongoing calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing Knotx calls"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        callStateJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "dootawas_calls_silent"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_CALL =
            "com.senghol.knotx.calling.START_CALL"

        const val ACTION_STOP_CALL =
            "com.senghol.knotx.calling.STOP_CALL"

        const val ACTION_HANGUP =
            "com.senghol.knotx.calling.HANGUP"

        const val ACTION_START_SCREEN_SHARE =
            "com.senghol.knotx.calling.START_SCREEN_SHARE"

        const val ACTION_STOP_SCREEN_SHARE =
            "com.senghol.knotx.calling.STOP_SCREEN_SHARE"

        private const val EXTRA_PROJECTION_DATA = "projection_data"

        /** [permissionData] = Intent from the system screen-capture dialog. */
        fun startScreenShare(context: Context, permissionData: Intent) {
            val intent = Intent(
                context,
                CallService::class.java
            ).apply {
                action = ACTION_START_SCREEN_SHARE
                putExtra(EXTRA_PROJECTION_DATA, permissionData)
            }

            context.startService(intent)
        }

        fun stopScreenShare(context: Context) {
            val intent = Intent(
                context,
                CallService::class.java
            ).apply {
                action = ACTION_STOP_SCREEN_SHARE
            }

            context.startService(intent)
        }

        fun start(context: Context) {
            val intent = Intent(
                context,
                CallService::class.java
            ).apply {
                action = ACTION_START_CALL
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(
                context,
                CallService::class.java
            ).apply {
                action = ACTION_STOP_CALL
            }

            context.startService(intent)
        }
    }
}