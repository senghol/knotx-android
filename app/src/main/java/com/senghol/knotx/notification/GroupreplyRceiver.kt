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
class GroupReplyReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_TEXT_REPLY_REC = "reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        

        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = results.getCharSequence(KEY_TEXT_REPLY_REC)?.toString() ?: return

        val groupId = intent.getStringExtra("groupId") ?: return
        val groupName = intent.getStringExtra("groupName") ?: return
        val messageId = intent.getStringExtra("conversationId") ?: return

        

        val inputData = workDataOf(
            "GROUP_ID" to groupId,
            "GROUP_NAME" to groupName,
            "REPLY_TEXT" to replyText,
            "MESSAGE_ID" to messageId
        )

        val workRequest = OneTimeWorkRequestBuilder<GroupReplyWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
