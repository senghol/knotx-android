package com.senghol.knotx.backup

import android.content.Context
import java.io.File

import android.app.NotificationManager
import android.content.ContentValues

import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.media.AudioManager
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.senghol.knotx.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


object RestoreManager {

    private const val TAG = "RestoreManager"
    private const val NOTIF_ID = 7002
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

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int = 0,
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
        if (!done) builder.setProgress(100, progress, indeterminate)
        else builder.setProgress(0, 0, false)
        nm.notify(NOTIF_ID, builder.build())
    }

    // ─── MAIN RESTORE ─────────────────────────────────────────────────────────

    // Helper to update both notification and UI

    suspend fun startRestore(
        context: Context,
        accountName: String,
        driveFileId: String,
        restoreMedia: Boolean = false,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                fun updateProgress(progress: Int, message: String) {
                    showNotification(context, "Knotx Restore", message, progress = progress)
                    onProgress(progress, message)
                }


                showNotification(context, "Knotx Restore", "Preparing restore...", indeterminate = true)

                val tempDir = File(context.cacheDir, "restore_temp").also {
                    it.deleteRecursively()
                    it.mkdirs()
                }






                // ── STEP 1: DOWNLOAD MANIFEST FIRST ───────────────────────────────
                updateProgress(5, "Loading backup info...")
                val manifest = restoreManifest(context, accountName, tempDir)

                // ── STEP 2: DOWNLOAD + DECRYPT DB BACKUP ──────────────────────────

                updateProgress(15, "Downloading backup...")
                val encFile = File(tempDir, "restore.enc")
                val zipFile = File(tempDir, "restore.zip")
                val unzipDir = File(tempDir, "unzipped").also { it.mkdirs() }

                val downloaded = DriveHelper.downloadBackup(context, accountName, driveFileId, encFile)
                if (!downloaded) {
                    Log.e(TAG, "Download failed")
                    showNotification(context, "Restore Failed ❌", "Could not download backup", done = true)
                    tempDir.deleteRecursively()
                    return@withContext false
                }

                updateProgress(30, "Decrypting...")
                EncryptionHelper.decrypt(encFile, zipFile, accountName)

                ZipHelper.unzip(zipFile, unzipDir)



                // ── STEP 3: CLOSE ROOM DB ─────────────────────────────────────────

                // Force WAL checkpoint and close
                try {
                    val dbFile = context.getDatabasePath("knotx.db")
                    // WAL files will be overwritten — that's fine
                } catch (e: Exception) {
                    Log.w(TAG, "DB close warning: ${e.message}")
                }

// ── STEP 4: REPLACE DB FILES ──────────────────────────────────────
                updateProgress(40, "Preparing database...")

// ─────────────────────────────────────────────────────────────
// TABLES TO PRESERVE FROM CURRENT DEVICE
// ─────────────────────────────────────────────────────────────

                val preservedTables = setOf(

                    // Signal crypto/device state
                    "identity_keys",
                    "prekeys",
                    "signed_prekeys",
                    "sessions",
                    "message_session_map",
                    "session_trigger_status",
                    "trusted_identities",
                    "kyber_prekeys",
                    "key_state",

                    // Device/runtime specific
                    "DakiyaSetting",
                    "debug_logs",

                    // Pending/transient
                    "unsentFiles",
                    "unsentSandesh",

                    // Local-only data
                    "transactions_entity",
                    "contact"
                )

// ─────────────────────────────────────────────────────────────
// STEP 4.0 — BACKUP CURRENT DEVICE TABLES
// ─────────────────────────────────────────────────────────────

                updateProgress(42, "Saving device state...")

                val preservedData = mutableMapOf<String, MutableList<ContentValues>>()

                val currentDbPath = context.getDatabasePath("knotx.db").absolutePath

                val currentDb = SQLiteDatabase.openDatabase(
                    currentDbPath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )

                try {

                    preservedTables.forEach { table ->

                        try {

                            val rows = mutableListOf<ContentValues>()

                            val cursor = currentDb.rawQuery(
                                "SELECT * FROM $table",
                                null
                            )

                            val columns = cursor.columnNames

                            while (cursor.moveToNext()) {

                                val values = ContentValues()

                                columns.forEachIndexed { index, column ->

                                    when (cursor.getType(index)) {

                                        Cursor.FIELD_TYPE_INTEGER ->
                                            values.put(column, cursor.getLong(index))

                                        Cursor.FIELD_TYPE_FLOAT ->
                                            values.put(column, cursor.getDouble(index))

                                        Cursor.FIELD_TYPE_STRING ->
                                            values.put(column, cursor.getString(index))

                                        Cursor.FIELD_TYPE_BLOB ->
                                            values.put(column, cursor.getBlob(index))

                                        Cursor.FIELD_TYPE_NULL ->
                                            values.putNull(column)
                                    }
                                }

                                rows.add(values)
                            }

                            cursor.close()

                            preservedData[table] = rows


                        } catch (e: Exception) {
                            Log.e(TAG, "Failed preserving $table: ${e.message}")
                        }
                    }

                } finally {
                    currentDb.close()
                }

// ─────────────────────────────────────────────────────────────
// STEP 4.1 — DELETE OLD DATABASE FILES
// ─────────────────────────────────────────────────────────────

                val dbDestDir = context.getDatabasePath("knotx.db").parentFile!!

                dbDestDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach {

                        it.delete()
                    }

                updateProgress(50, "Restoring messages...")

// ─────────────────────────────────────────────────────────────
// STEP 4.2 — RESTORE BACKUP DATABASE
// ─────────────────────────────────────────────────────────────

                val dbSource = File(unzipDir, "databases")

                if (dbSource.exists()) {

                    dbSource.copyRecursively(
                        dbDestDir,
                        overwrite = true
                    )


                } else {

                    Log.e(TAG, "databases/ not found in backup")

                    showNotification(
                        context,
                        "Restore Failed ❌",
                        "Backup missing database files",
                        done = true
                    )

                    tempDir.deleteRecursively()

                    return@withContext false
                }

// ─────────────────────────────────────────────────────────────
// STEP 4.3 — RESTORE PRESERVED TABLES
// ─────────────────────────────────────────────────────────────

                updateProgress(55, "Restoring device state...")

                val restoredDb = SQLiteDatabase.openDatabase(
                    currentDbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )

                try {

                    restoredDb.beginTransaction()

                    try {

                        preservedData.forEach { (table, rows) ->

                            try {

                                restoredDb.execSQL("DELETE FROM $table")

                                rows.forEach { values ->

                                    restoredDb.insertWithOnConflict(
                                        table,
                                        null,
                                        values,
                                        SQLiteDatabase.CONFLICT_REPLACE
                                    )
                                }


                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Failed restoring preserved table $table: ${e.message}"
                                )
                            }
                        }

                        restoredDb.setTransactionSuccessful()

                    } finally {
                        restoredDb.endTransaction()
                    }

                } finally {
                    restoredDb.close()
                }



                // ── STEP 6: RESTORE MEDIA (optional) ─────────────────────────────
                if (restoreMedia) {
                    restoreMediaDeltas(context, accountName, tempDir, manifest) { progress, message ->
                        updateProgress(progress, message)
                    }
                } else {
                }

                // ── CLEANUP ───────────────────────────────────────────────────────
                tempDir.deleteRecursively()


                showNotification(
                    context, "Restore Complete ✅",
                    "Restart app to see your data",
                    progress = 100, done = true
                )

                true
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed: ${e.message}", e)
                showNotification(context, "Restore Failed ❌", e.message ?: "Unknown error", done = true)
                false
            }
        }
    }

    // ─── RESTORE MEDIA DELTAS ─────────────────────────────────────────────────

    private suspend fun restoreMediaDeltas(
        context: Context,
        accountName: String,
        tempDir: File,
        manifest: JSONObject,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ) {
        try {
            val allBackups = DriveHelper.listAllFiles(context, accountName)
            val mediaDeltas = allBackups.filter { it.name.startsWith("media_delta_") }
                .sortedBy { it.createdTime }


            mediaDeltas.forEachIndexed { index, delta ->
                // Calculate progress 70-90% spread across all deltas
                val deltaProgress = if (mediaDeltas.size > 0) {
                    70 + ((index.toFloat() / mediaDeltas.size) * 20).toInt()
                } else 70

                onProgress(deltaProgress, "Restoring media ${index + 1}/${mediaDeltas.size}")



                val deltaEnc = File(tempDir, "delta_${index}.enc")
                val deltaZip = File(tempDir, "delta_${index}.zip")
                val deltaUnzip = File(tempDir, "delta_${index}").also { it.mkdirs() }


                onProgress(deltaProgress, "Downloading media ${index + 1}/${mediaDeltas.size}")
                val downloaded = DriveHelper.downloadBackup(context, accountName, delta.id, deltaEnc)
                if (!downloaded) {
                    Log.e(TAG, "Failed to download delta: ${delta.name}")
                    return@forEachIndexed
                }

                // Decrypt
                onProgress(deltaProgress + 1, "Decrypting media ${index + 1}/${mediaDeltas.size}")
                EncryptionHelper.decrypt(deltaEnc, deltaZip, accountName)
                ZipHelper.unzip(deltaZip, deltaUnzip)

                // Copy files
                val allFiles = deltaUnzip.walkTopDown().filter { it.isFile }.toList()
                allFiles.forEachIndexed { fileIndex, file ->
                    // Per-file progress within this delta
                    val fileProgress = deltaProgress + 1 +
                            ((fileIndex.toFloat() / allFiles.size) * 1).toInt()

                    if (fileIndex % 10 == 0) { // update every 10 files
                        onProgress(fileProgress.coerceAtMost(89), "Restoring files ${fileIndex + 1}/${allFiles.size}")
                    }
                    val relPath =
                        deltaUnzip.toURI()
                            .relativize(file.toURI())
                            .path

                    var destFile: File? = null

                    val filesObj =
                        manifest.optJSONObject("files")

                    val entry =
                        filesObj?.optJSONObject(relPath)

                    if (entry != null) {

                        val backupRoot =
                            entry.optString("backupRoot")

                        val relativePath =
                            entry.optString("relativePath")

                        destFile = when (backupRoot) {

                            "internal_userdata" -> {

                                File(
                                    context.filesDir,
                                    "userdata/$relativePath"
                                )
                            }

                            "private_media" -> {

                                File(
                                    Environment.getExternalStorageDirectory(),
                                    "Android/media/com.senghol.knotx/Knotx/Media/$relativePath"
                                )
                            }

                            else -> null
                        }
                    }

                    if (destFile == null) {

                        Log.w(
                            TAG,
                            "Could not resolve restore path for: $relPath"
                        )

                        return@forEachIndexed
                    }
                    destFile.let {

                        it.parentFile?.mkdirs()

                        file.copyTo(
                            it,
                            overwrite = true
                        )

                    }
                }

                deltaEnc.delete()
                deltaZip.delete()
                deltaUnzip.deleteRecursively()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Media restore failed: ${e.message}", e)
        }
    }    // ─── RESTORE MANIFEST ─────────────────────────────────────────────────────

    private suspend fun restoreManifest(
        context: Context,
        accountName: String,
        tempDir: File
    ): JSONObject {
        return try {
            val manifestItems = DriveHelper.listAllFiles(context, accountName)
                .filter { it.name == "backup_manifest.json" }
            if (manifestItems.isEmpty()) return JSONObject()

            val manifestFile = File(tempDir, "manifest_download.json")
            val downloaded = DriveHelper.downloadBackup(
                context, accountName, manifestItems[0].id, manifestFile
            )
            if (downloaded) {
                try { JSONObject(manifestFile.readText()) } catch (e: Exception) { JSONObject() }
            } else JSONObject()
        } catch (e: Exception) {
            Log.e(TAG, "Manifest restore failed: ${e.message}")
            JSONObject()
        }
    }
    // ─── RESTART APP ──────────────────────────────────────────────────────────

    fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        if (intent != null) {
            context.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}