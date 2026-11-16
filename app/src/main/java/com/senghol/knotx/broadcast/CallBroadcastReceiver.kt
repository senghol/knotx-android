package com.senghol.knotx.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Connection
import android.util.Log
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.ui.calling.ConnectionDootaws
import com.senghol.knotx.ui.calling.IncomingCallForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class CallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REJECT_CALL) {

            val user = intent.getStringExtra(EXTRA_REMOTE_USER)
            val roomId = intent.getStringExtra("roomId")

            if (user.isNullOrEmpty()) {
                Log.e("VOIP", "Reject failed: user missing")
                return
            }

            val appContext = context.applicationContext

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {

                val dootawas = (appContext as KnotxApp).knotx

                // 🔥 cancel UI FIRST
                withContext(Dispatchers.Main) {
                    if (!roomId.isNullOrEmpty()) {
                        IncomingCallForegroundService.cancelCallNotification(appContext, roomId)
                    }

                    IncomingCallForegroundService.stop(appContext)

                    ConnectionDootaws.currentConnection?.let { conn ->

                        if (conn.state == Connection.STATE_RINGING) {
                            conn.onReject()
                        } else {
                            conn.onDisconnect()
                        }
                    }

                    ConnectionDootaws.clearAll()
                }

                // 🔥 then network
//                knotx.connection(ConnectReason.Login)
                delay(1000)
                dootawas.rejectCall(user)
            }
        }
    }

    companion object {
        const val ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
        const val EXTRA_REMOTE_USER = "userNumber"
    }
}
