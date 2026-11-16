package com.senghol.knotx.ui.calling
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.Connection
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.senghol.knotx.R
import com.senghol.knotx.broadcast.CallBroadcastReceiver
import com.senghol.knotx.utils.MiuiPermissionHelper

class IncomingCallForegroundService : Service() {

    companion object {

        fun cancelCallNotification(context: Context, roomId: String) {
            val id = roomId.hashCode()
            val nm = NotificationManagerCompat.from(context)
            nm.cancel(id)
        }

        fun start(
            context: Context,
            callerName: String,
            isVideoCall: Boolean,
            userId: String,
            roomId: String,
            callerAvatarUri: Uri? = null
        ) {
            val intent = Intent(context, IncomingCallForegroundService::class.java).apply {
                putExtra("callerName", callerName)
                putExtra("isVideo", isVideoCall)
                putExtra("userId", userId)
                putExtra("roomId", roomId)
                callerAvatarUri?.let { putExtra("avatarUri", it) }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, IncomingCallForegroundService::class.java)
            context.stopService(intent)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val callerName = intent.getStringExtra("callerName") ?: "Unknown"
        val isVideoCall = intent.getBooleanExtra("isVideo", false)
        val userId = intent.getStringExtra("userId") ?: ""
        val roomId = intent.getStringExtra("roomId") ?: ""
        val avatarUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("avatarUri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("avatarUri")
        }

        val notification = buildCallNotification(callerName, isVideoCall, userId, roomId,avatarUri)

        val notificationId = roomId.hashCode()
        startForeground(notificationId, notification)

        // Add this at the end of onStartCommand, after startForeground()
        if (MiuiPermissionHelper.isMiui()) {
            try {
                val callIntent = Intent(this, CallActivity2::class.java).apply {
                    putExtra("userNumber", userId)
                    putExtra("video", isVideoCall)
                    putExtra("outgoing", false)
                    putExtra("callerName", callerName)
                    putExtra("roomId", roomId)
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or  // ✅ this.flags
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(callIntent)
            } catch (e: Exception) {
                Log.e("VOIP", "MIUI direct activity launch failed", e)
            }
        }//        triggerRingingConnectionFromNotification(userId, isVideoCall, callerName)

        // At the end of onStartCommand, after startForeground()
        Handler(Looper.getMainLooper()).postDelayed({
            if (ConnectionDootaws.currentConnection?.state == Connection.STATE_RINGING) {
                ConnectionDootaws.currentConnection?.onReject()
                cancelCallNotification(this, roomId)   // 🔥 ADD THIS
                stopSelf()
            }
        }, 40_000)
        return START_NOT_STICKY
    }

    private fun buildCallNotification(
        callerName: String,
        isVideoCall: Boolean,
        userId: String,
        roomId: String,
        callerAvatarUri: Uri?
    ): Notification {

        // Smart channel selection
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val channelId = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "incoming_call_sound_channel"
            AudioManager.RINGER_MODE_VIBRATE -> "incoming_call_vibrate_channel"
            else -> "incoming_call_silent_channel"
        }

        // === ACCEPT: Direct to CallActivity2 + auto-answer ===
        val acceptIntent = Intent(this, CallActivity2::class.java).apply {
            putExtra("userNumber", userId)
            putExtra("video", isVideoCall)
            putExtra("outgoing", false)
            putExtra("callerName", callerName)
            putExtra("roomId", roomId)
            putExtra("autoAnswer", true)      // Will answer automatically
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val acceptRequestCode = (roomId + "_accept").hashCode()

        val acceptPending = PendingIntent.getActivity(
            this,
            acceptRequestCode,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, CallBroadcastReceiver::class.java).apply {
            action = CallBroadcastReceiver.ACTION_REJECT_CALL
            putExtra("userNumber", userId)
            putExtra("roomId", roomId)   // 🔥 ADD THIS
        }
        val requestCode = roomId.hashCode()
        val rejectPending = PendingIntent.getBroadcast(
            this,
            requestCode,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // === CONTENT INTENT: Tap on notification body → open ringing UI (manual accept/reject) ===
// AFTER
        val contentIntent = Intent(this, CallActivity2::class.java).apply {
            putExtra("userNumber", userId)
            putExtra("video", isVideoCall)
            putExtra("outgoing", false)
            putExtra("callerName", callerName)
            putExtra("roomId", roomId)          // ✅ ADD THIS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }


        val contentRequestCode = (roomId + "_content").hashCode()

        val contentPending = PendingIntent.getActivity(
            this,
            contentRequestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Person
        val personBuilder = Person.Builder()
            .setName(callerName)
            .setImportant(true)
        callerAvatarUri?.let {
            personBuilder.setIcon(IconCompat.createWithContentUri(it))
        }
        val callerPerson = personBuilder.build()

        // Notification Builder
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.icon_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ✅ ADD THIS
            .setContentIntent(contentPending)
            .setFullScreenIntent(contentPending, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(45_000)
            .addPerson(callerPerson)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callStyle = NotificationCompat.CallStyle
                .forIncomingCall(callerPerson, rejectPending, acceptPending)  // reject first, accept second

            notificationBuilder
                .setStyle(callStyle)
                .setContentTitle("Incoming ${if (isVideoCall) "Video" else "Voice"} Call")
        } else {
            // Fallback for older Android
            notificationBuilder
                .setContentTitle("Incoming ${if (isVideoCall) "Video" else "Voice"} Call")
                .setContentText(callerName)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Incoming call from $callerName"))
                .addAction(R.drawable.call_24px, "Accept", acceptPending)
//                .addAction(R.drawable.call_end_24px, "Decline", rejectPending)
        }

        return notificationBuilder.build()
    }    override fun onBind(intent: Intent?): IBinder? = null

}