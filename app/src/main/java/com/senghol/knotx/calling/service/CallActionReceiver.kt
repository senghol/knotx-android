package com.senghol.knotx.calling.service


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.senghol.knotx.KnotxApp

import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.ui.CallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return
        val pendingResult = goAsync()

        val application = context.applicationContext as KnotxApp
        val container = application.callContainer

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val call = container.callManager.activeCall.value

                // No such call any more (it ended / was cancelled while this
                // notification was still on screen): just remove the stale
                // notification instead of leaving dead buttons.
                if (call == null || call.callId.value != callId) {
                    Log.w(
                        "CallNotification",
                        "${intent.action} ignored: callId=$callId, active=${call?.callId?.value}"
                    )
                    container.callNotificationManager.cancelIncomingCall(callId)
                    return@launch
                }

                when (intent.action) {
                    ACTION_ACCEPT -> {

                        if (call.state != CallState.RINGING) {
                            return@launch
                        }


                        val callActivityIntent = Intent(
                            context,
                            CallActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(CallActivity.EXTRA_AUTO_ACCEPT, true)
                        }

                        context.startActivity(callActivityIntent)

                    }

                    ACTION_REJECT -> {

                        if (!container.callManager.rejectCall()) {
                            Log.w("CallNotification", "REJECT ignored: state=${call.state}")
                            container.callNotificationManager.cancelIncomingCall(callId)
                            return@launch
                        }

                        container.callNotificationManager.cancelIncomingCall(callId)

                        call.participants.forEach { participant ->
                            container.callSignalingManager.sendCallReject(
                                callId = call.callId.value,
                                target = participant.userNumber
                            )
                        }

                        container.callAudioManager.reset()
                        container.callCameraManager.reset()
                    }

                    ACTION_CANCEL -> {
                        if (!container.callManager.cancelCall()) return@launch

                        call.participants.forEach { participant ->
                            container.callSignalingManager.sendCallCancel(
                                callId = call.callId.value,
                                target = participant.userNumber
                            )
                        }

                        container.callAudioManager.reset()
                        container.callCameraManager.reset()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT =
            "com.senghol.knotx.calling.ACCEPT"

        const val ACTION_REJECT =
            "com.senghol.knotx.calling.REJECT"

        const val ACTION_CANCEL =
            "com.senghol.knotx.calling.CANCEL"

        const val EXTRA_CALL_ID =
            "call_id"

        fun accept(context: Context, callId: String) =
            Intent(context, CallActionReceiver::class.java).apply {
                action = ACTION_ACCEPT
                putExtra(EXTRA_CALL_ID, callId)
            }

        fun reject(context: Context, callId: String) =
            Intent(context, CallActionReceiver::class.java).apply {
                action = ACTION_REJECT
                putExtra(EXTRA_CALL_ID, callId)
            }

        fun cancel(context: Context, callId: String) =
            Intent(context, CallActionReceiver::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_CALL_ID, callId)
            }
    }
}