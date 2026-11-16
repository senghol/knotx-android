package com.senghol.knotx.backup



import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Environment
import androidx.core.app.NotificationCompat
import com.senghol.knotx.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val TAG = "BackupManager"
    private const val NOTIF_ID = 7001
    private const val MANIFEST_FILE = "backup_manifest.json"

    // ─── NOTIFICATION ─────────────────────────────────────────────────────────

    private fun getChannelId(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "message_sound_channel"
            AudioManager.RINGER_MODE_VIBRATE -> "message_vibrate_channel"
            else -> "message_silent_channel"
        }
    }

    private fun showProgressNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int = 0,
        max: Int = 100,
        indeterminate: Boolean = false,
        done: Boolean = false
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, getChannelId(context))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(!done)
            .setOnlyAlertOnce(true)
            .setAutoCancel(done)
        if (!done) builder.setProgress(max, progress, indeterminate)
        else builder.setProgress(0, 0, false)
        nm.notify(NOTIF_ID, builder.build())
    }

    // ─── FILE HASH ────────────────────────────────────────────────────────────

    private fun md5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // ─── MANIFEST ─────────────────────────────────────────────────────────────

    // ─── MANIFEST — DRIVE ONLY ────────────────────────────────────────────────

    private suspend fun loadManifestFromDrive(
        context: Context,
        accountName: String,
        tempDir: File
    ): JSONObject {
        return try {
            val allFiles = DriveHelper.listAllFiles(context, accountName)
            val manifestOnDrive = allFiles.firstOrNull { it.name == MANIFEST_FILE }
            if (manifestOnDrive != null) {
                val manifestFile = File(tempDir, "manifest_temp.json")
                val downloaded = DriveHelper.downloadBackup(
                    context, accountName, manifestOnDrive.id, manifestFile
                )
                if (downloaded) {
                    try { JSONObject(manifestFile.readText()) } catch (e: Exception) { buildEmptyManifest() }
                } else {
                    buildEmptyManifest()
                }
            } else {
                buildEmptyManifest()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "loadManifestFromDrive failed: ${e.message}")
            buildEmptyManifest()
        }
    }

    private suspend fun uploadManifestToDrive(
        context: Context,
        accountName: String,
        manifest: JSONObject,
        tempDir: File
    ) {
        val manifestFile = File(tempDir, MANIFEST_FILE)
        manifestFile.writeText(manifest.toString())
        DriveHelper.uploadBackup(context, accountName, manifestFile, MANIFEST_FILE)
    }


    private fun buildEmptyManifest(): JSONObject {
        return JSONObject().apply {
            put("version", 2)
            put("lastBackup", "")
            put("files", JSONObject())
        }
    }


    private fun getFilesObject(manifest: JSONObject): JSONObject {
        return if (manifest.has("files")) manifest.getJSONObject("files") else JSONObject()
    }

    // ─── RELATIVE PATH ────────────────────────────────────────────────────────

    private fun relativePath(file: File, baseDir: File): String {
        return baseDir.toURI().relativize(file.toURI()).path
    }

    // ─── MAIN BACKUP ──────────────────────────────────────────────────────────

    suspend fun startBackup(
        context: Context,
        accountName: String,
        includeMedia: Boolean,
        retentionCount: Int = 7,         // keep last N db backups
        mediaRetentionMonths: Int = -1   // -1 = forever
    ): Boolean {
        return withContext(Dispatchers.IO) {


            try {

                showProgressNotification(
                    context, "KnotX Backup", "Preparing backup...", indeterminate = true
                )

                val tempDir = File(context.cacheDir, "backup_temp").also {
                    it.deleteRecursively()
                    it.mkdirs()
                }

                val dateStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())

                // ── STEP 1: DB BACKUP (always full) ───────────────────────────
                showProgressNotification(context, "KnotX Backup", "Backing up messages...", progress = 10)

                val dbParent = context.getDatabasePath("knotx.db").parentFile!!
                val datastore = File(context.filesDir, "datastore")
                val profiles = File(context.filesDir, "profiles")

                val dbMergeDir = File(tempDir, "db_merge").also { it.mkdirs() }
                dbParent.copyRecursively(File(dbMergeDir, "databases"), overwrite = true)

                if (datastore.exists()) {
                    datastore.copyRecursively(File(dbMergeDir, "datastore"), overwrite = true)
                }
                if (profiles.exists()) {
                    profiles.copyRecursively(File(dbMergeDir, "profiles"), overwrite = true)
                }

                val dbZip = File(tempDir, "db.zip")
                val dbEnc = File(tempDir, "db_$dateStamp.enc")
                ZipHelper.zip(dbMergeDir, dbZip)
                EncryptionHelper.encrypt(dbZip, dbEnc, accountName)

                showProgressNotification(context, "KnotX Backup", "Uploading messages...", progress = 30)

                val dbFileId = DriveHelper.uploadBackup(context, accountName, dbEnc, "db_$dateStamp.enc")

                if (dbFileId == null) {
                    android.util.Log.e(TAG, "DB upload failed")
                    showProgressNotification(context, "Backup Failed ❌", "Could not upload messages", done = true)
                    tempDir.deleteRecursively()
                    return@withContext false
                }

// ── STEP 2: MEDIA DELTA SYNC ──────────────────────────────────
// Load manifest always — regardless of includeMedia
                val manifest = loadManifestFromDrive(context, accountName, tempDir)
                val filesObj = getFilesObject(manifest)
                val updatedFilesObj = JSONObject(filesObj.toString())

                if (includeMedia) {
                    showProgressNotification(context, "KnotX Backup", "Checking media for changes...", progress = 50)

// Collect media dirs
                    val backupRoots = mutableListOf<File>()

// Private app media dir
                    val privateMediaDir = File(
                        Environment.getExternalStorageDirectory(),
                        "Android/media/com.senghol.knotx/Knotx/Media"
                    )
// Internal userdata root
                    val internalUserdataDir = File(
                        context.filesDir,
                        "userdata"
                    )


                    listOf(
                        privateMediaDir,
                        internalUserdataDir
                    ).forEach { dir ->

                        if (dir.exists()) {

                            backupRoots.add(dir)

                        }
                    }


                    // Find new/changed files
                    val deltaDir = File(tempDir, "delta").also { it.mkdirs() }
                    var newFiles = 0
                    var skippedFiles = 0

                    backupRoots.forEach { baseDir ->
                        baseDir.walkTopDown().filter {
                            it.isFile &&
                                    it.name != ".nomedia"
                        }.forEach { file ->
                            val rootName = when (baseDir) {

                                internalUserdataDir ->
                                    "internal_userdata"

                                privateMediaDir ->
                                    "private_media"

                                else ->
                                    "unknown"
                            }

                            val relativeForManifest =
                                "$rootName/${relativePath(file, baseDir)}"
                            val currentHash = md5(file)
                            val currentSize = file.length()

                            val existing = if (filesObj.has(relativeForManifest))
                                filesObj.getJSONObject(relativeForManifest) else null

                            val savedHash = existing?.optString("hash", null)
                            val savedSize = existing?.optLong("size", -1L) ?: -1L

                            if (savedHash == currentHash && savedSize == currentSize) {
                                skippedFiles++
                            } else {
                                newFiles++
// TO — preserve structure using relative path for zip
                                val relativeForZip =
                                    "$rootName/${relativePath(file, baseDir)}"
                                val destFile = File(deltaDir, relativeForZip)
                                destFile.parentFile?.mkdirs()
                                file.copyTo(destFile, overwrite = true)
                            }
                        }
                    }


                    if (newFiles > 0) {
                        showProgressNotification(context, "KnotX Backup", "Uploading $newFiles new/changed files...", progress = 65)

                        val deltaZip = File(tempDir, "media_delta_$dateStamp.zip")
                        val deltaEnc = File(tempDir, "media_delta_$dateStamp.enc")
                        ZipHelper.zip(deltaDir, deltaZip)
                        EncryptionHelper.encrypt(deltaZip, deltaEnc, accountName)


                        val deltaFileId = DriveHelper.uploadBackup(
                            context, accountName, deltaEnc, "media_delta_$dateStamp.enc"
                        )

                        if (deltaFileId != null) {
                            val now = System.currentTimeMillis()
                            // Walk original media dirs to get correct absolute paths
                            backupRoots.forEach { baseDir ->
                                baseDir.walkTopDown().filter {
                                    it.isFile &&
                                            it.name != ".nomedia"
                                }.forEach { originalFile ->
                                    val rootName = when (baseDir) {

                                        internalUserdataDir ->
                                            "internal_userdata"

                                        privateMediaDir ->
                                            "private_media"

                                        else ->
                                            "unknown"
                                    }
                                    val relativeForZip =
                                        "$rootName/${relativePath(originalFile, baseDir)}"
                                    val deltaFile = File(deltaDir, relativeForZip)
                                    if (deltaFile.exists()) {

                                        val manifestKey =
                                            "$rootName/${relativePath(originalFile, baseDir)}"

                                        val entry = JSONObject().apply {

                                            put("hash", md5(originalFile))

                                            put("size", originalFile.length())

                                            put("backedUpAt", now)

                                            put("driveFileId", deltaFileId)

                                            put("deltaFile", "media_delta_$dateStamp.enc")

                                            put("backupRoot", rootName)

                                            put(
                                                "relativePath",
                                                relativePath(originalFile, baseDir)
                                            )

                                            put("relPath", relativeForZip)
                                        }

                                        updatedFilesObj.put(
                                            manifestKey,
                                            entry
                                        )

                                    }                                }
                            }
                        }                    } else {
                    }
                }

// ── ALWAYS UPLOAD MANIFEST ────────────────────────────────────
// Runs whether includeMedia is true or false
// ── ALWAYS UPLOAD MANIFEST TO DRIVE ──────────────────────────
                manifest.put("files", updatedFilesObj)
                manifest.put("lastBackup", dateStamp)
                manifest.put("version", 2)
                uploadManifestToDrive(context, accountName, manifest, tempDir)

                // ── STEP 3: RETENTION CLEANUP ─────────────────────────────────
                showProgressNotification(context, "KnotX Backup", "Cleaning up old backups...", progress = 80)

                // Prune old DB backups
                DriveHelper.pruneOldDbBackups(context, accountName, retentionCount)

                // Prune old media deltas by age
                if (mediaRetentionMonths > 0) {
                    // TO
                    pruneOldMediaDeltas(context, accountName, mediaRetentionMonths, tempDir)
                }

                // ── DONE ──────────────────────────────────────────────────────
                tempDir.deleteRecursively()


                showProgressNotification(
                    context, "Backup Complete ✅",
                    "Messages and media backed up",
                    progress = 100, done = true
                )

                true
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Backup failed: ${e.message}", e)
                showProgressNotification(context, "Backup Failed ❌", e.message ?: "Unknown error", done = true)
                false
            }
        }
    }

    // ─── PRUNE OLD MEDIA DELTAS ───────────────────────────────────────────────

    private suspend fun pruneOldMediaDeltas(
        context: Context,
        accountName: String,
        retentionMonths: Int,
        tempDir: File  // ← add this param
    ) {
        // Load from Drive
        val manifest = loadManifestFromDrive(context, accountName, tempDir)
        val filesObj = getFilesObject(manifest)
        val cutoffTime = System.currentTimeMillis() - (retentionMonths * 30L * 24 * 60 * 60 * 1000)

        val keysToRemove = mutableListOf<String>()
        val deltaFilesToDelete = mutableSetOf<String>()

        filesObj.keys().forEach { key ->
            val entry = filesObj.getJSONObject(key)
            val backedUpAt = entry.optLong("backedUpAt", Long.MAX_VALUE)
            if (backedUpAt < cutoffTime) {
                keysToRemove.add(key)
                entry.optString("driveFileId", null)?.let { deltaFilesToDelete.add(it) }
            }
        }

        keysToRemove.forEach { filesObj.remove(it) }
        manifest.put("files", filesObj)

        // Upload updated manifest back to Drive
        uploadManifestToDrive(context, accountName, manifest, tempDir)

        deltaFilesToDelete.forEach { fileId ->
            DriveHelper.deleteFile(context, accountName, fileId)
        }

    }

}