package com.senghol.knotx.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.JsonParser
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.ui.calling.IncomingCallForegroundService
import com.senghol.knotx.utils.enums.CallDataType
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.JsonKeys
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.senghol.knotx.R
import kotlinx.coroutines.CancellationException
import java.io.File

class FcmWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val mid = inputData.getString("mid") ?: return Result.failure()
        val file = File(File(applicationContext.filesDir, "pending_fcm"), "$mid.json")
        if (!file.exists()) return Result.success()          // already processed

        return try {
            val jsonString = file.readText()
            val jsonMessage = JsonParser.parseString(jsonString).asJsonObject
            val retrievedValue = jsonMessage[JsonKeys.DATA_TYPE]?.asInt
            if (retrievedValue == null) {
                file.delete()
                return Result.failure()
            }
            val dataType = DataType.fromInt(retrievedValue)
            val dootawas = (applicationContext as KnotxApp).knotx

            when (dataType) {
                DataType.MESSAGE -> dootawas.processFirebaseMessage(jsonMessage)

                DataType.CALL_DATA -> {
                    val caller = jsonMessage[JsonKeys.SENDER]?.asString
                    val callType = jsonMessage[JsonKeys.CALL_DATA_TYPE]?.asInt

                    if (caller != null && callType != null) {
                        when (callType) {
                            CallDataType.VIDEO_RECEIVED.value,
                            CallDataType.AUDIO_RECEIVED.value -> {
                                val isVideoCall = callType == CallDataType.VIDEO_RECEIVED.value
                                val roomId = jsonMessage[JsonKeys.PROFILE_ID]?.asString ?: "unknown"
                                val callerName = dootawas.getUserNameFromAll(caller)
                                dootawas.showIncomingCallA(callerName, isVideoCall, caller, roomId)
                            }
                            CallDataType.END_CALL.value -> {
                                val roomId = jsonMessage[JsonKeys.PROFILE_ID]?.asString ?: "unknown"
                                dootawas.setCallAlreadyEnded(roomId)
                                dootawas.sendToSignal(jsonString)
                                IncomingCallForegroundService.cancelCallNotification(applicationContext, roomId)
                                IncomingCallForegroundService.stop(applicationContext)
                            }
                            else -> dootawas.sendToSignal(jsonString)
                        }
                    }
                }
                else -> {}
            }

            file.delete()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FcmWorker", "process failed (attempt $runAttemptCount)", e)
            if (runAttemptCount < 5) Result.retry() else {
                val target = runCatching { fallbackTarget(file.readText()) }.getOrNull()
                file.delete()
                target?.let { (applicationContext as KnotxApp).knotx.showFallbackNotification(it) }
                Result.failure()
            }
        }
    }

    private fun fallbackTarget(json: String): String? {
        val o = JsonParser.parseString(json).asJsonObject
        val sandesh = o[JsonKeys.SANDESH]?.asString
            ?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
        return if (sandesh?.get("isGroup")?.asBoolean == true) sandesh["groupId"]?.asString
        else o[JsonKeys.SENDER]?.asString
    }

    // Required for expedited work on Android 11 and below
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "fcm_sync"
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Message sync", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Syncing messages")
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29)
            ForegroundInfo(1001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(1001, n)
    }
}