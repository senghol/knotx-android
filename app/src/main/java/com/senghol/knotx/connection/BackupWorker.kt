package com.senghol.knotx.connection

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.senghol.knotx.drive.GoogleDriveManager
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.settingStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.io.File


class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // Load settings
            val backupAccount = applicationContext.settingStore.data.first()[SettingPrefKeys.BACKUP_ACCOUNT]
                ?: return@coroutineScope Result.failure()

            val includeMedia = inputData.getBoolean("include_media", false)

            val helper = DriveBackupHelper(
                applicationContext,
                backupAccount,
                applicationContext.settingStore
            )

            // Foreground notification
            setForeground(helper.buildForegroundInfo("Backup in progress..."))

            // Ensure Drive is connected
            val driveApi = GoogleDriveManager.driveApi
                ?: return@coroutineScope Result.failure()

            // Locate or create backup folder
            val folderId = helper.getOrCreateBackupFolder()

            // ----------------------------------------------------
            // 1) BACKUP DATABASE FILES
            // ----------------------------------------------------
            val dbBaseName = "Knotx"

            val dbMain = applicationContext.getDatabasePath(dbBaseName)
            val dbShm = File(dbMain.parentFile, "$dbBaseName-shm")
            val dbWal = File(dbMain.parentFile, "$dbBaseName-wal")

            val dbFiles = listOf(dbMain, dbShm, dbWal)
                .filter { it.exists() }

            for (file in dbFiles) {
                helper.uploadFile(file, folderId)
            }

            // ----------------------------------------------------
            // 2) BACKUP MEDIA FILES
            // ----------------------------------------------------
            if (includeMedia) {
                val mediaDir = File(applicationContext.getExternalFilesDir(null), "media")

                mediaDir.listFiles()?.forEach { file ->
                    helper.uploadFile(file, folderId)
                }
            }

            // ----------------------------------------------------
            // 3) FINISH
            // ----------------------------------------------------
            helper.updateLastBackupTime()
            helper.showNotification("Backup Complete", "Database and media backed up successfully.")

            Result.success()

        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed", e)
            Result.retry()
        }
    }
}
