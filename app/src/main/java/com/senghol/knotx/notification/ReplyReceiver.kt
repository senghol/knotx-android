package com.senghol.knotx.notification


import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.senghol.knotx.notification.ReplyReceiver.AndroidEntryPoint


@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {
    annotation class AndroidEntryPoint

    companion object {
        const val KEY_TEXT_REPLY_REC = "reply"
    }
    override fun onReceive(context: Context, intent: Intent) {
        

        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = results.getCharSequence(KEY_TEXT_REPLY_REC)?.toString() ?: return

        val replyTo = intent.getStringExtra("sender") ?: return
        val messageId = intent.getStringExtra("conversationId") ?: return

        

        val inputData = workDataOf(
            "REPLY_TO" to replyTo,
            "REPLY_TEXT" to replyText,
            "MESSAGE_ID" to messageId
        )

        val workRequest = OneTimeWorkRequestBuilder<ReplyWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // ❌ DO NOT CANCEL THE NOTIFICATION
        // notificationManager.cancel(messageId.hashCode())  ❌ DELETE THIS
    }
}