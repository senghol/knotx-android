package com.senghol.knotx.connection

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senghol.knotx.KnotxApp


import androidx.work.*

import java.util.concurrent.TimeUnit

class MessageSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dootawas = (applicationContext as KnotxApp).knotx
        return when (dootawas.isMessagePending()) {
            null -> {
                if (runAttemptCount < 5) Result.retry() else Result.failure()
            }
            true -> {
                SocketManager.setAppActive(true)
                Result.success()
            }
            false -> Result.success()
        }
    }

    companion object {
        private val networkOnly = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Run one check as soon as there is network. Safe to call many times. */
        fun enqueueOnce(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<MessageSyncWorker>()
                .setConstraints(networkOnly)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork("pending_check_once", ExistingWorkPolicy.KEEP, req)
        }

        /** Safety net: check every ~15 min (Android's minimum). Call once at app start. */
        fun schedulePeriodic(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<MessageSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkOnly)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork("message_sync_worker", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}