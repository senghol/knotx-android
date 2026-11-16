package com.senghol.knotx.backup

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senghol.knotx.R
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.settingStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val accountName = inputData.getString("accountName") ?: return Result.failure()
        val includeMedia = inputData.getBoolean("includeMedia", false)
        val retentionCount = inputData.getInt("retentionCount", 7)
        val mediaRetentionMonths = inputData.getInt("mediaRetentionMonths", -1)

        // Read wifiOnly from DataStore at runtime
        val wifiOnly = applicationContext.settingStore.data
            .map { it[SettingPrefKeys.BACKUP_WIFI_ONLY] ?: true }
            .first()


        if (wifiOnly && !isOnWifi()) {
            // Show notification so user knows
            showWifiNotification()
            return Result.retry()
        }

        val success = BackupManager.startBackup(
            applicationContext, accountName, includeMedia, retentionCount, mediaRetentionMonths
        )
        return if (success) Result.success() else Result.retry()
    }

    private fun isOnWifi(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun showWifiNotification() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        val channelId = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "message_sound_channel"
            AudioManager.RINGER_MODE_VIBRATE -> "message_vibrate_channel"
            else -> "message_silent_channel"
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Backup Waiting")
            .setContentText("Connect to Wi-Fi to backup your data")
            .setAutoCancel(true)
            .build()
        nm.notify(7003, notification)
    }
}