package com.senghol.knotx.notification


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senghol.knotx.KnotxApp

class GroupReplyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val groupId = inputData.getString("GROUP_ID") ?: return Result.failure()
        val groupName = inputData.getString("GROUP_NAME") ?: return Result.failure()
        val replyText = inputData.getString("REPLY_TEXT") ?: return Result.failure()
        val messageId = inputData.getString("MESSAGE_ID") ?: return Result.failure()

        val app = applicationContext as? KnotxApp
        val dakiya = app?.knotx

        if (dakiya == null) {
            Log.e("GroupReplyWorker", "Knotx instance is null!")
            return Result.retry()
        }

        

        // ✅ THIS MUST:
        // 1. Send message to server
        // 2. Insert into DB
        // 3. Update notification thread (group)
        dakiya.notificationGroupReply(
            groupId = groupId,
            groupName = groupName,
            replyMessage = replyText
        )

        return Result.success()
    }
}
