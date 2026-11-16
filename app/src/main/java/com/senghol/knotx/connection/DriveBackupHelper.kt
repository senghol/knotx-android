package com.senghol.knotx.connection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

import com.senghol.knotx.utils.enums.SettingPrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


import androidx.work.ForegroundInfo
import com.senghol.knotx.drive.GoogleDriveManager

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.FileOutputStream

class DriveBackupHelper(
    private val context: Context,
    private val accountName: String,
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "backup_channel"
        const val NOTIFICATION_ID = 1002
    }

    /** ----------------------------------------------------------
     *  NOTIFICATIONS
     *  ---------------------------------------------------------- */

    fun showNotification(title: String, message: String) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Backup & Restore Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    fun buildForegroundInfo(content: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Backup/Restore")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    /** ----------------------------------------------------------
     *  DRIVE REST OPERATIONS
     *  ---------------------------------------------------------- */

    private val api get() = GoogleDriveManager.driveApi
        ?: throw IllegalStateException("Drive not connected")

    /**
     * Finds folder or creates it
     */
    suspend fun getOrCreateBackupFolder(): String = withContext(Dispatchers.IO) {
        val folderName = "DakiyaBackups"

        // Check if exists
        val q = "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val response = api.listFiles(q)
        val existing = response.files.firstOrNull()

        if (existing != null) {
            return@withContext existing.id
        }

        // Create folder (metadata only)
        val metadataJson = """
            {
              "name": "$folderName",
              "mimeType": "application/vnd.google-apps.folder"
            }
        """.trimIndent()

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), metadataJson)
        val created = api.createMetadataOnly(body)

        created.id
    }

    /**
     * Upload or update file
     */
    suspend fun uploadFile(localFile: java.io.File, folderId: String): String =
        withContext(Dispatchers.IO) {

            val fileName = localFile.name

            // Check if exists
            val searchQ =
                "name='$fileName' and '$folderId' in parents and trashed=false"
            val list = api.listFiles(searchQ).files

            val mime = when (localFile.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "mp4" -> "video/mp4"
                "db" -> "application/x-sqlite3"
                else -> "application/octet-stream"
            }

            val metadataJson = """
                {
                  "name": "$fileName",
                  "parents": ["$folderId"]
                }
            """.trimIndent()

            val metadataBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                metadataJson
            )

            val fileBody = localFile.asRequestBody(mime.toMediaTypeOrNull())
            val multipart = MultipartBody.Part.createFormData("file", fileName, fileBody)

            if (list.isNotEmpty()) {
                val fileId = list[0].id
                val updated = api.updateFile(fileId, metadataBody, multipart)
                updated.id
            } else {
                val created = api.uploadFile(metadataBody, multipart)
                created.id
            }
        }

    /**
     * Download file
     */
    suspend fun downloadFile(fileId: String, target: java.io.File) =
        withContext(Dispatchers.IO) {
            target.parentFile?.mkdirs()

            val body = api.downloadFile(fileId)

            FileOutputStream(target).use { out ->
                out.write(body.bytes())
            }

        }

    /**
     * Save last backup timestamp
     */
    suspend fun updateLastBackupTime() {
        dataStore.edit { prefs ->
            prefs[SettingPrefKeys.LAST_BACKUP_TIME] = System.currentTimeMillis()
        }
    }
}