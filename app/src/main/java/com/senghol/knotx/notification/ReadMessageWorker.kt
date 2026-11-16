package com.senghol.knotx.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.senghol.knotx.KnotxApp
class ReadMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val messageId = inputData.getString("MESSAGE_ID") ?: return Result.failure()
        val targetId = inputData.getString("TARGET_ID") ?: return Result.failure()
        val isGroup = inputData.getBoolean("IS_GROUP", false)

        val app = applicationContext as? KnotxApp
        val dakiya = app?.knotx ?: return Result.retry()

        

        // ✅ SINGLE ENTRY POINT
        if (isGroup) {
            dakiya.sendGroupDhrist( targetId,true)
        } else {
            dakiya.sendDhrist( targetId,true)
        }

        return Result.success()
    }
}
