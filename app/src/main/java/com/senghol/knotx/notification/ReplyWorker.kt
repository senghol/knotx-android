package com.senghol.knotx.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senghol.knotx.KnotxApp

class ReplyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val replyTo = inputData.getString("REPLY_TO") ?: return Result.failure()
        val replyText = inputData.getString("REPLY_TEXT") ?: return Result.failure()
        val messageId = inputData.getString("MESSAGE_ID") ?: return Result.failure()

        val app = applicationContext as? KnotxApp
        val dakiya = app?.knotx

        if (dakiya == null) {
            Log.e("ReplyWorker", "Dakiya instance is null!")
            return Result.retry()
        }

        

        // ✅ THIS SENDS + UPDATES NOTIFICATION THREAD
        dakiya.notificationReply(replyTo, replyText)

        return Result.success()
    }
}
