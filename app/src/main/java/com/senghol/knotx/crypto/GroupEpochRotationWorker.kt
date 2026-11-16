package com.senghol.knotx.crypto

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.utils.objects.DLog

class GroupEpochRotationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_GROUP_ID = "group_id"

        fun uniqueWorkName(groupId: String): String {
            return "group_epoch_rotation_$groupId"
        }
    }

    override suspend fun doWork(): Result {
        val groupId = inputData.getString(KEY_GROUP_ID)
            ?: return Result.failure()

        return try {
            val app = applicationContext as KnotxApp

            app.knotx.checkAndRotateGroupEpoch(
                groupId = groupId,
                forceAfterTimeout = true
            )

            DLog.i(
                "Group epoch timeout worker completed: group=$groupId"
            )

            Result.success()
        } catch (e: Exception) {
            DLog.e(
                "Group epoch timeout worker failed: group=$groupId",
                e
            )

            Result.retry()
        }
    }
}