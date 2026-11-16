package com.senghol.knotx.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.senghol.knotx.notification.ReadMessageWorker
class ReadMessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val messageId = intent.getStringExtra("MESSAGE_ID") ?: return
        val senderOrGroupId = intent.getStringExtra("SENDER_ID") ?: return
        val isGroup = intent.getBooleanExtra("IS_GROUP", false)

        

        // ✅ 1. CLOSE NOTIFICATION IMMEDIATELY
        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.cancel(senderOrGroupId.hashCode())
        }

        // ✅ 2. PASS TO WORKER
        val inputData = workDataOf(
            "MESSAGE_ID" to messageId,
            "TARGET_ID" to senderOrGroupId,
            "IS_GROUP" to isGroup
        )

        val workRequest = OneTimeWorkRequestBuilder<ReadMessageWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
