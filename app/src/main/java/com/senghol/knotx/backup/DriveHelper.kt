package com.senghol.knotx.backup

import android.content.Context
import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object DriveHelper {

    private const val TAG = "DriveHelper"
    private const val APP_FOLDER_NAME = "DootawasBackup"
    private const val MIME_FOLDER = "application/vnd.google-apps.folder"
    private const val MIME_FILE = "application/octet-stream"

    // ─── DRIVE SERVICE ────────────────────────────────────────────────────────

    private fun getDriveService(context: Context, accountName: String): Drive {
        val credential = GoogleAuthHelper.getDriveCredential(context, accountName)
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Knotx")
            .build()
    }

    // ─── FOLDER ───────────────────────────────────────────────────────────────

    private suspend fun getOrCreateAppFolder(drive: Drive): String {
        return withContext(Dispatchers.IO) {
            val result = drive.files().list()
                .setQ("name='$APP_FOLDER_NAME' and mimeType='$MIME_FOLDER' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                result.files[0].id
            } else {
                val folder = DriveFile().apply {
                    name = APP_FOLDER_NAME
                    mimeType = MIME_FOLDER
                }
                drive.files().create(folder)
                    .setFields("id")
                    .execute().id
            }
        }
    }

    // ─── UPLOAD ───────────────────────────────────────────────────────────────

    suspend fun uploadBackup(
        context: Context,
        accountName: String,
        file: File,
        fileName: String
    ): String? {
        return withContext(Dispatchers.IO) {
            try {

                val drive = getDriveService(context, accountName)
                val folderId = getOrCreateAppFolder(drive)

                // If file with same name exists — delete it first (e.g. manifest)
                val existing = drive.files().list()
                    .setQ("name='$fileName' and '$folderId' in parents and trashed=false")
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()
                existing.files.forEach {
                    drive.files().delete(it.id).execute()
                }

                val metadata = DriveFile().apply {
                    name = fileName
                    mimeType = MIME_FILE
                    parents = listOf(folderId)
                }

                val mediaContent = FileContent(MIME_FILE, file)
                val uploaded = drive.files().create(metadata, mediaContent)
                    .setFields("id, name, createdTime, size")
                    .execute()

                uploaded.id
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Upload failed: $fileName — ${e.message}", e)
                null
            }
        }
    }

    // ─── LIST ─────────────────────────────────────────────────────────────────

    suspend fun listBackups(
        context: Context,
        accountName: String
    ): List<DriveBackupItem> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(context, accountName)
                val folderId = getOrCreateAppFolder(drive)

                val result = drive.files().list()
                    .setQ("'$folderId' in parents and trashed=false and name contains 'db_'")
                    .setSpaces("drive")
                    .setFields("files(id, name, createdTime, size)")
                    .setOrderBy("createdTime desc")
                    .execute()


                result.files.map {
                    DriveBackupItem(
                        id = it.id,
                        name = it.name,
                        createdTime = it.createdTime?.value ?: 0L,
                        size = it.getSize() ?: 0L
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "List failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    // ─── DOWNLOAD ─────────────────────────────────────────────────────────────

    suspend fun downloadBackup(
        context: Context,
        accountName: String,
        fileId: String,
        destFile: File
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(context, accountName)
                FileOutputStream(destFile).use { output ->
                    drive.files().get(fileId).executeMediaAndDownloadTo(output)
                }
                true
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Download failed: ${e.message}", e)
                false
            }
        }
    }

    // ─── DELETE SINGLE FILE ───────────────────────────────────────────────────

    suspend fun deleteFile(
        context: Context,
        accountName: String,
        fileId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(context, accountName)
                drive.files().delete(fileId).execute()
                true
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Delete failed: $fileId — ${e.message}", e)
                false
            }
        }
    }

    // ─── PRUNE OLD DB BACKUPS ─────────────────────────────────────────────────

    suspend fun pruneOldDbBackups(
        context: Context,
        accountName: String,
        keepCount: Int = 7
    ) {
        withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(context, accountName)
                val folderId = getOrCreateAppFolder(drive)

                // Only target db_*.enc files
                val result = drive.files().list()
                    .setQ("'$folderId' in parents and trashed=false and name contains 'db_'")
                    .setSpaces("drive")
                    .setFields("files(id, name, createdTime)")
                    .setOrderBy("createdTime desc")
                    .execute()


                val toDelete = result.files.drop(keepCount)
                toDelete.forEach {
                    drive.files().delete(it.id).execute()
                }

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Prune DB failed: ${e.message}", e)
            }
        }
    }


    // ─── GET DRIVE STORAGE USED ───────────────────────────────────────────────

    suspend fun getDriveStorageUsed(
        context: Context,
        accountName: String
    ): Long {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(context, accountName)
                val folderId = getOrCreateAppFolder(drive)

                val result = drive.files().list()
                    .setQ("'$folderId' in parents and trashed=false")
                    .setSpaces("drive")
                    .setFields("files(size)")
                    .execute()

                val total = result.files.sumOf { it.getSize() ?: 0L }
                total
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Storage check failed: ${e.message}", e)
                0L
            }
        }
    }
    suspend fun listAllFiles(
        context: Context,
        accountName: String
    ): List<DriveBackupItem> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(context, accountName)
                val folderId = getOrCreateAppFolder(drive)
                val allFiles = mutableListOf<com.google.api.services.drive.model.File>()
                var pageToken: String? = null

                do {
                    val result = drive.files().list()
                        .setQ("'$folderId' in parents and trashed=false")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, createdTime, size)")
                        .setOrderBy("createdTime asc")
                        .setPageSize(1000) // max per page
                        .also { if (pageToken != null) it.pageToken = pageToken }
                        .execute()

                    allFiles.addAll(result.files)
                    pageToken = result.nextPageToken
                } while (pageToken != null)


                allFiles.map {
                    DriveBackupItem(
                        id = it.id,
                        name = it.name,
                        createdTime = it.createdTime?.value ?: 0L,
                        size = it.getSize() ?: 0L
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "listAllFiles failed: ${e.message}", e)
                emptyList()
            }
        }
    }

}

// ─── MODEL ────────────────────────────────────────────────────────────────────

data class DriveBackupItem(
    val id: String,
    val name: String,
    val createdTime: Long,
    val size: Long
)