package com.senghol.knotx.calling.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.senghol.knotx.R
import com.senghol.knotx.calling.domain.repository.CallParticipantRepository
import com.senghol.knotx.calling.domain.model.CallParticipantUi

import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.service.CallActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallNotificationManager(
    private val context: Context,
    private val callParticipantRepository: CallParticipantRepository
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private val lock = Any()
    private val pendingCalls = HashSet<String>()
    fun showIncomingCall(call: CallSession) {
        val callId = call.callId.value
        synchronized(lock) { pendingCalls.add(callId) }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val participant = call.participants.firstOrNull()
                val resolvedParticipant = participant?.let {
                    callParticipantRepository.resolveParticipant(it)
                }

                // can take a while (avatar download)
                val notification = buildIncomingCallNotification(call, resolvedParticipant)

                // The call may have been cancelled while we were building this.
                synchronized(lock) {
                    if (callId in pendingCalls) {
                        notificationManager.notify(
                            incomingCallNotificationId(callId),
                            notification
                        )
                    } else {
                    }
                }
            } catch (e: Exception) {
                Log.e("CallNotification", "failed to show incoming call", e)
            }
        }
    }

    fun cancelIncomingCall(callId: String) {
        synchronized(lock) {
            pendingCalls.remove(callId)
            notificationManager.cancel(incomingCallNotificationId(callId))
        }
    }

    private suspend fun buildIncomingCallNotification(
        call: CallSession,
        participant: CallParticipantUi?
    ): Notification {
        val callId = call.callId.value
        // Person.setName() rejects empty strings, and displayName can be ""
        // (not null), so skip blank values.
        val callerName = participant?.displayName?.takeIf { it.isNotBlank() }
            ?: participant?.participant?.userNameId?.takeIf { it.isNotBlank() }
            ?: participant?.participant?.userNumber?.takeIf { it.isNotBlank() }
            ?: "Incoming call"

        val callText = when (call.callType) {
            CallType.VOICE -> "Incoming voice call"
            CallType.VIDEO -> "Incoming video call"
        }

        // 1. Asynchronously fetch high-res contact avatar using Coil for the Notification Large Icon
        val avatarBitmap = participant?.profileUrl?.let { url ->
            fetchAvatarBitmap(context, url)
        }

        val callActivityIntent = Intent(
            context,
            CallActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            callActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 10_000,
            callActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept opens the call screen directly with auto-accept. Going through a
        // BroadcastReceiver that then starts the activity is blocked on Android 12+.
        val acceptActivityIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallActivity.EXTRA_AUTO_ACCEPT, true)
        }

        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 1,
            acceptActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            callId.hashCode() + 2,
            CallActionReceiver.reject(context, callId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Build Person for CallStyle context
        val callerPersonBuilder = Person.Builder()
            .setName(callerName)
            .setImportant(true)

        if (avatarBitmap != null) {
            callerPersonBuilder.setIcon(IconCompat.createWithBitmap(avatarBitmap))
        }

        val callerPerson = callerPersonBuilder.build()

        // 3. Native Incoming Call Style with CallStyle API
        val callStyle = NotificationCompat.CallStyle.forIncomingCall(
            callerPerson,
            rejectPendingIntent,
            acceptPendingIntent
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.call_24px)
            .setContentTitle(callerName)
            .setContentText(callText)
            .setSubText("Knotx")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setStyle(callStyle)
            // Color accent for system UI elements (Accept button / badge highlights)
            .setColor(0xFF4CAF50.toInt())
            .setColorized(true)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        return builder.build()
    }

    private suspend fun fetchAvatarBitmap(context: Context, url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // Required for notification bitmaps
                    .transformations(CircleCropTransformation())
                    .build()
                val result = ImageLoader(context).execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap
            }.getOrNull()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming Knotx calls"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
            setSound(null, null)       // CallSoundManager rings
            enableVibration(false)      // CallSoundManager vibrates
            enableLights(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun incomingCallNotificationId(callId: String): Int {
        return callId.hashCode()
    }

    companion object {
        private const val CHANNEL_ID = "dootawas_incoming_calls_silent"
    }
}