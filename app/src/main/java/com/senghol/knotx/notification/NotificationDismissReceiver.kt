package com.senghol.knotx.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.senghol.knotx.notification.NotificationStore.messagingStyleMap

class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val senderId = intent.getStringExtra("SENDER_ID") ?: return

        // Forget the old messages so the next notification starts fresh
        messagingStyleMap.remove(senderId)
    }
}