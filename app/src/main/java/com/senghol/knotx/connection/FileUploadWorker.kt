package com.senghol.knotx.connection
import android.content.Context

import com.google.gson.Gson

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.senghol.knotx.KnotxApp
import android.util.Log
import androidx.work.*
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.ContentValues
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.media.MediaMetadataRetriever
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.senghol.knotx.BuildConfig
import com.senghol.knotx.connection.Knotx.SamagriUpdateLock
import com.senghol.knotx.connection.fileTransfer.ProgressRequestBody
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.GroupDao
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.data.database.UnsentFilesDao
import com.senghol.knotx.data.database.UnsentSandesh
import com.senghol.knotx.data.database.UnsentSandeshDao
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.FileDataType
import com.senghol.knotx.utils.enums.FileDataTypeToken
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.objects.DLog
import com.senghol.knotx.utils.objects.PrefKeys
import com.senghol.knotx.utils.objects.UploadData
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max

object NetworkClient {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_HOST + BuildConfig.API_PREFIX)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
// -----------------------------
// Upload Scheduler
// -----------------------------

object UploadScheduler {

    fun startUpload(context: Context, data: UploadData) {

        val json = Gson().toJson(data)

        val inputData = workDataOf(
            FileUploadWorker.KEY_UPLOAD_DATA to json,
            "message_id_key" to data.messageId
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true) // optional but allowed
            .build()
        val request = OneTimeWorkRequestBuilder<FileUploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)

            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30,
                TimeUnit.SECONDS
            )

            .setExpedited(
                OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "upload_${data.messageId}",
                ExistingWorkPolicy.KEEP,
                request
            )

    }

    fun cancelUpload(context: Context, messageId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("upload_$messageId")
    }
}




// -----------------------------
// Upload Worker
// -----------------------------

class FileUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "dootawas_uploads"
        const val KEY_UPLOAD_DATA = "upload_data"
        const val KEY_PROGRESS = "progress"
        private const val MAX_SIZE_IN_BYTES = 2300  // ✅ No warning
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val messageId = inputData.getString("message_id_key") ?: "0"
        createNotificationChannel()

        return createForegroundInfo(
            messageId,
            0,
            "Preparing upload…"
        )
    }

    fun urisToTempFiles(
        context: Context,
        uris: List<String>,
        messageId: String
    ): List<File> {

        val tempFiles = mutableListOf<File>()

        uris.forEach { uriString ->

            try {

                val inputStream = if (uriString.startsWith("/")) {

                    // ✅ Internal app file
                    File(uriString).inputStream()

                } else {

                    // ✅ Old URI support
                    val uri = uriString.toUri()
                    context.contentResolver.openInputStream(uri)
                } ?: return@forEach

                val tempFile = File.createTempFile(
                    "upload_${messageId}_",
                    ".tmp",
                    context.cacheDir
                )

                inputStream.use { input ->

                    tempFile.outputStream().use { output ->

                        input.copyTo(output)
                    }
                }

                tempFiles.add(tempFile)

            } catch (e: Exception) {

                Log.e(
                    "UploadWorker",
                    "Failed for: $uriString",
                    e
                )
            }
        }

        return tempFiles
    }

    /**
     * Uploads encrypted files directly to Cloudflare R2 using a presigned URL.
     *
     * Flow:
     * 1. Convert all selected URIs into temporary files.
     * 2. Encrypt them into a password-protected AES ZIP archive.
     * 3. Request a presigned upload URL from the server.
     * 4. Upload the encrypted ZIP directly to Cloudflare R2 using HTTP PUT.
     * 5. Track upload progress and update the local database.
     * 6. Automatically retry transient failures.
     * 7. If the upload URL expires (HTTP 403), request a fresh URL and continue.
     *
     * Security:
     * - Only the authenticated uploader can request an upload URL.
     * - The upload URL is short-lived and cryptographically signed.
     * - No auth token or message metadata is sent to Cloudflare R2.
     * - Only the encrypted ZIP bytes are uploaded.
     *
     * @return HTTP status code or custom error code.
     *
     * Return values:
     * -2  -> Local file not found.
     * 200 -> Upload successful.
     * 400 -> Bad request.
     * 403 -> Upload forbidden / URL expired.
     * 404 -> Upload target not found.
     * 413 -> Payload too large.
     * 500 -> Internal server error.
     * 503 -> Network error or temporary server failure.
     */
    suspend fun uploadFileInternal(
        data: UploadData,
        progressCallback: ((Int) -> Unit)? = null,
        groupDao: GroupDao,
        sandeshDao: SandeshDao
    ): Int {

        val tempFiles = urisToTempFiles(
            applicationContext,
            data.filePaths,
            data.messageId
        )

        if (tempFiles.isEmpty()) {
            Log.e("Knotx", "❌ No valid temp files")
            return -2
        }

        val receivers: List<String> = Gson().fromJson(
            data.receiversJson,
            object : TypeToken<List<String>>() {}.type
        )

        var response: Response? = null
        var zipFile: File? = null

        try {

            // -----------------------------------
            // Encrypt files into password protected ZIP
            // -----------------------------------
            zipFile = File(
                applicationContext.cacheDir,
                "encrypted_${UUID.randomUUID()}.zip"
            )

            val zipParameters = ZipParameters().apply {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                compressionLevel = CompressionLevel.FAST
            }

            val zip = ZipFile(zipFile, data.password.toCharArray())

            tempFiles.forEach { file ->
                zip.addFile(file, zipParameters)
            }

            val mediaType = "application/zip".toMediaType()
            val fileRequestBody = zipFile.asRequestBody(mediaType)

            // -----------------------------------
            // Request upload URL
            // -----------------------------------
            val dootawas = (applicationContext as KnotxApp).knotx

            val uploadResponse = if (data.dataType == FileDataType.STORY) {
                dootawas.requestStoryUploadUrl(
                    storyId = data.messageId
                )
            } else {
                dootawas.requestUploadUrl(
                    messageId = data.uuid,
                    receivers = receivers
                )
            }

            if (!uploadResponse.success) {
                return 503
            }

            var uploadUrl = uploadResponse.uploadUrl

            // -----------------------------------
            // Track upload progress
            // -----------------------------------
            val progressRequestBody = ProgressRequestBody(fileRequestBody) { progress ->

                progressCallback?.invoke(progress)

                CoroutineScope(Dispatchers.IO).launch {

                    val mutex = SamagriUpdateLock.getMutexForId(data.messageId)

                    mutex.withLock {

                        val safeProgress = minOf(progress, 99)

                        if (data.isGroup) {

                            val sandesh = groupDao.getGroupSandeshByIdAndGroupId(
                                data.messageId,
                                data.receiver
                            )

                            sandesh?.let {

                                val jsonObject =
                                    JsonParser.parseString(it.samagri).asJsonObject

                                val currentProgress =
                                    jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0

                                if (safeProgress > currentProgress) {

                                    jsonObject.addProperty(
                                        DootawasMessage.DOWNLOAD_PERCENT,
                                        safeProgress
                                    )

                                    groupDao.updateSamagri(
                                        data.messageId,
                                        jsonObject.toString(),
                                        data.receiver
                                    )
                                }
                            }

                        } else {

                            val sandesh = sandeshDao.getSandeshByIdAndUser(
                                data.messageId,
                                data.receiver
                            )

                            sandesh?.let {

                                val jsonObject =
                                    JsonParser.parseString(it.samagri).asJsonObject

                                val currentProgress =
                                    jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0

                                if (safeProgress > currentProgress) {

                                    jsonObject.addProperty(
                                        DootawasMessage.DOWNLOAD_PERCENT,
                                        safeProgress
                                    )

                                    sandeshDao.updateSamagri(
                                        data.messageId,
                                        jsonObject.toString()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val retrofit = NetworkClient.retrofit

            var lastException: Exception? = null

            repeat(3) { attempt ->

                try {

                    val request = Request.Builder()
                        .url(uploadUrl)
                        .put(progressRequestBody)
                        .build()

                    response = retrofit.callFactory().newCall(request).execute()

                    val code = response.code
                    val errorBody = response.body.string()

                    Log.e(
                        "UploadFile",
                        "HTTP $code : $errorBody"
                    )
                    if (code == 403) {
                        val body = response.body.string()

                        Log.e("UploadFile", "403: $body")

                        if (body.contains("RequestExpired") == true) {
                            // request new URL
                        } else {
                            response.close()
                            return 403
                        }
                        response.close()

                        DLog.w("UploadFile : Upload URL expired. Requesting a new URL...")

                        val refreshed = dootawas.requestUploadUrl(
                            messageId = data.uuid,
                            receivers = receivers
                        )

                        if (!refreshed.success) {
                            return 503
                        }

                        uploadUrl = refreshed.uploadUrl

                        return@repeat
                    }

                    response.close()
                    return code

                } catch (e: IOException) {

                    lastException = e

                    Log.e(
                        "UploadFile",
                        "Retry ${attempt + 1} failed: ${e.message}"
                    )

                    delay(2000)
                }
            }


            Log.e("UploadFile", "All retries failed", lastException)
            return 503

        } catch (e: IOException) {

            Log.e("UploadFile", "Network Error: ${e.message}", e)
            return 503

        } finally {

            tempFiles.forEach { it.delete() }
            zipFile?.takeIf { it.exists() }?.delete()
            response?.close()
        }
    }

    suspend fun Context.getFileToken(): String? {
        val prefs = userDataStore.data.first()
        return prefs[PrefKeys.FILE_TOKEN]
    }
    override suspend fun doWork(): Result {

        val json = inputData.getString(KEY_UPLOAD_DATA) ?: return Result.retry()
        val data = Gson().fromJson(json, UploadData::class.java)

        DLog.i("Uploading file")

        createNotificationChannel()

        try {
            setForeground(
                createForegroundInfo(
                    data.messageId,
                    0,
                    "Starting upload..."
                )
            )
        } catch (e: Exception) {
            Log.w("UploadWorker", "Foreground start blocked: ${e.message}")
        }

        val db = DootawasDatabase.getDatabase(applicationContext)

        val unsentDao = db.unsentFilesDao()
        val sandeshDao = db.sandeshDao()
        val unsentSandeshDao = db.unsentSandeshDao()
        val groupDao = db.groupDao()

        return try {

            val code = uploadFileInternal(
                data = data,
                progressCallback = { progress ->

                    setProgressAsync(
                        workDataOf(KEY_PROGRESS to progress)
                    )

                    try {
                        setForegroundAsync(
                            createForegroundInfo(
                                data.messageId,
                                progress,
                                "Uploading $progress%"
                            )
                        )
                    } catch (_: Exception) {
                    }
                },
                groupDao = groupDao,
                sandeshDao = sandeshDao
            )

            when (code) {

                in 200..299 -> {

                    try {
                        setForeground(
                            createForegroundInfo(
                                data.messageId,
                                100,
                                "Upload complete"
                            )
                        )
                    } catch (_: Exception) {
                    }

                    handleUploadSuccess(
                        data,
                        unsentDao,
                        sandeshDao,
                        unsentSandeshDao,
                        groupDao
                    )

                    Result.success()
                }

                // Temporary failures -> WorkManager retries
                in listOf(429, 502, 503, 504) -> {

                    Log.w(
                        "UploadWorker",
                        "Temporary upload failure ($code). Retrying..."
                    )

                    Result.retry()
                }

                // Permanent failure
                else -> {

                    Log.e(
                        "UploadWorker",
                        "Permanent upload failure: $code"
                    )

                    handleUploadFailure(
                        data,
                        code,
                        unsentDao,
                        sandeshDao
                    )

                    Result.failure()
                }
            }

        } catch (e: Exception) {

            Log.e("UploadWorker", "Upload crashed", e)

            Result.retry()
        }
    }

    // -----------------------------
    // SUCCESS HANDLER
    // -----------------------------
    private fun retrieveDocument(filePath: String?): File? {
        if (filePath.isNullOrBlank()) return null

        val file = File(filePath)

        return if (file.exists()) {
            file
        } else {
            Log.e("UploadWorker", "File not found: $filePath")
            null
        }
    }

    fun formatFileSize(sizeInBytes: Long): String {
        val kb = sizeInBytes / 1024
        val mb = kb / 1024
        return when {
            mb > 0 -> "$mb MB"
            kb > 0 -> "$kb KB"
            else -> "$sizeInBytes Bytes"
        }
    }

    suspend fun deleteSendData(unsentFilesDao: UnsentFilesDao,messageId: String, receiver: String) {
        unsentFilesDao.deleteByMessageIdAndSender(messageId, receiver)
    }

    @Throws(IOException::class)
    private fun extractFrameFromVideo(
        file: File
    ): Bitmap? {

        val retriever =
            MediaMetadataRetriever()

        return try {

            retriever.setDataSource(
                file.absolutePath
            )

            val timeUs = 1_000_000L // 1 second

            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

        } catch (e: Exception) {

            Log.e(
                "VideoPreview",
                "Failed to extract frame",
                e
            )

            null

        } finally {

            retriever.release()
        }
    }
    private fun Bitmap.toWebPByteArray(quality: Int = 30): ByteArray {
        ByteArrayOutputStream().use { stream ->
            compress(Bitmap.CompressFormat.WEBP, quality, stream)
            return stream.toByteArray()
        }
    }
    private fun bitmapToWebPBase64(bitmap: Bitmap, quality: Int = 30): String =
        Base64.encodeToString(bitmap.toWebPByteArray(quality), Base64.NO_WRAP)
    private fun processBlurredPreview(
        file: File,
        isVideo: Boolean
    ): String? {

        var originalBitmap: Bitmap? = null
        var downSampledBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        var base64String: String? = null

        try {

            // -----------------------------------
            // 🔥 LOAD BITMAP FROM URI (no File)
            // -----------------------------------
            originalBitmap = if (isVideo) {

                extractFrameFromVideo(file)

            } else {

                file.inputStream().use {

                    BitmapFactory.decodeStream(it)
                }
            }

            if (originalBitmap == null) {
                Log.e("ProcessBlur", "❌ Failed to decode bitmap from URI.")
                return null
            }

            val width = originalBitmap.width
            val height = originalBitmap.height

            // -----------------------------------
            // Downsample
            // -----------------------------------
            downSampledBitmap = if (width > 400 || height > 400) {
                val targetSize = 210
                val downSampleFactor =
                    (max(width.toDouble(), height.toDouble()) / targetSize).toInt()
                downSampleBitmap(originalBitmap, downSampleFactor)
            } else originalBitmap

            val downW = downSampledBitmap.width
            val downH = downSampledBitmap.height

            // -----------------------------------
            // Size debug (unchanged)
            // -----------------------------------
            val webpBytesA = downSampledBitmap.toWebPByteArray(quality = 30)
            val sizeInKBB = webpBytesA.size / 1024.0


            // -----------------------------------
            // Crop (unchanged)
            // -----------------------------------
            croppedBitmap = when {
                downH > downW * 1.33f -> {
                    val desiredHeight = (downW * 1.33f).toInt()
                    val cropY = (downH - desiredHeight) / 2
                    Bitmap.createBitmap(downSampledBitmap, 0, cropY, downW, desiredHeight)
                }
                downH < downW * 1.5f -> {
                    val desiredWidth = (downH / 1.5f).toInt()
                    val cropX = (downW - desiredWidth) / 2
                    Bitmap.createBitmap(downSampledBitmap, cropX, 0, desiredWidth, downH)
                }
                else -> downSampledBitmap
            }

            val croppedW = croppedBitmap.width
            val croppedH = croppedBitmap.height

            val mutableCroppedBitmap =
                croppedBitmap.copy(Bitmap.Config.ARGB_8888, true)

            // -----------------------------------
            // Blur (unchanged)
            // -----------------------------------
            val blurredBitmap = blurBitmapA(mutableCroppedBitmap, 10f)

            val base64WithBlur = blurredBitmap?.let { compressBitmapToBase64(it) }

            mutableCroppedBitmap.recycle()

            if (blurredBitmap !== mutableCroppedBitmap) {
                blurredBitmap?.recycle()
            }
            val withBlurSize = base64WithBlur?.toByteArray(Charsets.UTF_8)?.size ?: 0

            base64String = base64WithBlur

        } catch (e: OutOfMemoryError) {
            Log.e("ProcessBlur", "OutOfMemoryError: ${e.message}")
        } catch (e: Exception) {
            Log.e("ProcessBlur", "Error: ${e.message}")
        } finally {
            originalBitmap?.recycle()
            if (downSampledBitmap !== originalBitmap) downSampledBitmap?.recycle()
            if (croppedBitmap !== downSampledBitmap && croppedBitmap !== originalBitmap) {
                croppedBitmap?.recycle()
            }
        }

        return base64String
    }




    private fun blurBitmapA(inputBitmap: Bitmap, radius: Float): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurBitmapWithHardwareRenderer(inputBitmap, radius)
        } else {
            blurBitmap(inputBitmap) // Your previous Stack Blur method
        }
    }
    private fun blurBitmap(bitmap: Bitmap): Bitmap {
        val outputBitmap: Bitmap = Bitmap.createBitmap(bitmap)

        // Create a RenderScript object
        val renderScript: RenderScript = RenderScript.create(applicationContext)

        // Allocate memory for Renderscript to work with
        val input: Allocation = Allocation.createFromBitmap(renderScript, bitmap)
        val output: Allocation = Allocation.createFromBitmap(renderScript, outputBitmap)

        // Load the script to blur the image
        val blur: ScriptIntrinsicBlur =
            ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        blur.setInput(input)

        // Set the blur radius (0 < radius <= 25)
        blur.setRadius(17f)

        // Perform the Renderscript blur
        blur.forEach(output)

        // Copy the output bitmap back into the original bitmap
        output.copyTo(outputBitmap)

        // Destroy the Renderscript instance to free up resources
        renderScript.destroy()

        return outputBitmap
    }
    @RequiresApi(Build.VERSION_CODES.S) // API 31 (Android 12)
    fun blurBitmapWithHardwareRenderer(inputBitmap: Bitmap, radius: Float): Bitmap? {
        // Ensure we're on Android 12+; otherwise, return null or fallback (caller should check)

        // Step 1: Set up ImageReader for output
        val imageReader = ImageReader.newInstance(
            inputBitmap.width,
            inputBitmap.height,
            PixelFormat.RGBA_8888,
            1, // Single output image
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )

        // Step 2: Configure RenderNode and HardwareRenderer
        val renderNode = RenderNode("BlurEffect")
        val hardwareRenderer = HardwareRenderer().apply {
            setSurface(imageReader.surface)
            setContentRoot(renderNode)
        }
        renderNode.setPosition(0, 0, imageReader.width, imageReader.height)

        // Step 3: Apply blur effect
        val blurRenderEffect = RenderEffect.createBlurEffect(
            radius.coerceIn(0f, 25f), // Cap radius at 25 for consistency with older APIs
            radius.coerceIn(0f, 25f),
            Shader.TileMode.MIRROR // Mirror edges for smooth boundaries
        )
        renderNode.setRenderEffect(blurRenderEffect)

        // Step 4: Draw the bitmap with blur
        val renderCanvas = renderNode.beginRecording()
        renderCanvas.drawBitmap(inputBitmap, 0f, 0f, null)
        renderNode.endRecording()

        // Step 5: Render and sync
        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        // Step 6: Extract the blurred Bitmap from HardwareBuffer
        return try {
            val image = imageReader.acquireNextImage()
                ?: throw RuntimeException("Failed to acquire image from ImageReader")
            val hardwareBuffer = image.hardwareBuffer
                ?: throw RuntimeException("No HardwareBuffer available")
            val blurredBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                ?: throw RuntimeException("Failed to create Bitmap from HardwareBuffer")

            // Step 7: Clean up resources
            hardwareBuffer.close()
            image.close()
            imageReader.close()
            renderNode.discardDisplayList()
            hardwareRenderer.destroy()

            blurredBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Method to calculate the size of a bitmap in bytes
    private fun getBitmapSize(bitmap: Bitmap): Int {
        val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray().size // Return size in bytes
    }

    // DownSample the Bitmap
    private fun downSampleBitmap(bitmap: Bitmap, sampleSize: Int): Bitmap {
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        // Calculate the new dimensions
        val newWidth: Int = width / sampleSize
        val newHeight: Int = height / sampleSize

        // Use createScaledBitmap for faster resizing
        return bitmap.scale(newWidth, newHeight) // 'true' for filtering for better quality
    }

    private fun compressBitmapToBase64(bitmap: Bitmap): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            
            compressBitmapToBase64API30(bitmap)
        } else {
            
            compressBitmapToBase64Legacy(bitmap)
        }
    }

    // ✅ Modern Compression (API 30+)
    @RequiresApi(Build.VERSION_CODES.R)
    fun compressBitmapToBase64API30(bitmap: Bitmap): String? {
        return compressBitmapToBase64Original(bitmap, Bitmap.CompressFormat.WEBP_LOSSY)
    }

    // ✅ Legacy Compression (API <30)
    private fun compressBitmapToBase64Legacy(bitmap: Bitmap): String? {
        return compressBitmapToBase64Original(bitmap, Bitmap.CompressFormat.JPEG) // 🔥 Use JPEG instead of WEBP
    }

    private fun compressBitmapToBase64Original(bitmap: Bitmap, format: Bitmap.CompressFormat): String? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        var quality = 100  // Start at max quality

        // Binary search bounds
        var maxQuality = 100

        var bestByteArray: ByteArray? = null
        var lastDecrease = 0

        while (0 <= maxQuality) {
            byteArrayOutputStream.reset()
            // ✅ Ensure quality is always within 0-100 range
            quality = quality.coerceIn(0, 100)
            bitmap.compress(format, quality, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            if (byteArray.size <= MAX_SIZE_IN_BYTES) {
                // ✅ Found a valid compressed image, store it as best candidate
                bestByteArray = byteArray
                // ✅ Try increasing quality to get closer to MAX_SIZE_IN_BYTES
                val adaptiveIncrease = max((lastDecrease / 2).toDouble(), 1.0).toInt()
                val higherQuality = (quality + adaptiveIncrease).coerceIn(0, 100)
                byteArrayOutputStream.reset()
                bitmap.compress(format, higherQuality, byteArrayOutputStream)
                val higherQualityByteArray = byteArrayOutputStream.toByteArray()
                if (higherQualityByteArray.size <= MAX_SIZE_IN_BYTES) {
                    // ✅ If higher quality still fits, use it
                    bestByteArray = higherQualityByteArray
                } else {
                    // ✅ If higher quality is too large, keep previous best
                }
                break
            } else {
                // ❌ Too large, reduce quality using binary search
                maxQuality = quality - 1
                val newQuality = (0 + maxQuality) / 2
                // Calculate last decrease step
                lastDecrease = quality - newQuality
                quality = newQuality
            }
        }

        if (bestByteArray != null) {
            val base64Str = Base64.encodeToString(bestByteArray, Base64.DEFAULT)
            
            
            return base64Str
        }

        
        return null
    }

    private fun saveImagePreview(base64Image: String): String {
        val previewDir = File(applicationContext.filesDir, "preview")
        if (!previewDir.exists() && !previewDir.mkdirs()) {
            
            return ""
        }

        val fileName = "preview_${System.currentTimeMillis()}.webp"
        val imageFile = File(previewDir, fileName)

        return try {
            // Decode Base64 to Bitmap
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val originalBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            // Always use WebP format (lossless for API 30+)
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP
            }
            // Save the image
            FileOutputStream(imageFile).use { outputStream ->
                val success = originalBitmap.compress(format, 100, outputStream)
                if (!success) {
                    
                    return ""
                }
            }
            // ✅ Return file path if saved successfully
            
            imageFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    private suspend fun insertUnsentData(unsentSandeshDao: UnsentSandeshDao,unsentSandesh: UnsentSandesh, checkAlready: Boolean = true) {
        try {
            if (checkAlready){

                val existing = unsentSandeshDao.findSimilarUnsentSandesh(
                    receiver = unsentSandesh.receiver,
                    message = unsentSandesh.message ?: "",
                    dataType = unsentSandesh.dataType
                )

                if (existing != null) {
                    
                    return
                }
            }
            
            unsentSandeshDao.insert(unsentSandesh)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(ContentValues.TAG, "❌ Error inserting unsentSandesh: ${e.message}")
        }
    }

    private suspend fun handleUploadSuccess(
        data: UploadData,
        unsentFileDao: UnsentFilesDao,
        sandeshDao: SandeshDao,
        unsentSandeshDao: UnsentSandeshDao,
        groupDao: GroupDao
    ) {
        val mainJsonObject =
            JsonParser.parseString(data.mainJson).asJsonObject

        when (data.dataType) {

            FileDataType.IMAGE,
            FileDataType.REPLY_IMAGE -> {

                val dataArray = mainJsonObject.getAsJsonArray(DootawasMessage.ALL_DATA)

                val firstUriString = dataArray
                    ?.firstOrNull()
                    ?.asJsonObject
                    ?.get(DootawasMessage.ORIGINAL_URI)
                    ?.asString

                if (firstUriString.isNullOrBlank()) {
                    Log.e("Upload", "No valid image URI found")
                    return
                }



                val file = File(firstUriString)

                val base64Image =
                    processBlurredPreview(
                        file,
                        false
                    )

                mainJsonObject.addProperty(
                            DootawasMessage.IMAGE_PREVIEW,
                            base64Image ?: ""
                        )

                    var savedUri: String? = null
                    // Save image preview to personal storage and get URI
                    base64Image?.let {
                        savedUri =
                            saveImagePreview(it)
                    }



                    mainJsonObject.addProperty(
                        DootawasMessage.FILE_PASS,
                        data.password
                    )
                    mainJsonObject.addProperty(
                        DootawasMessage.FILE_ID,
                        data.uuid
                    )

                    insertUnsentData(unsentSandeshDao,
                        UnsentSandesh(
                            messId = data.messageId,
                            receiver = data.receiver,
                            message = mainJsonObject.toString(),
                            dataType = DataType.MESSAGE,
                            isGroup = data.isGroup
                        )
                    )
                    if (data.isGroup) {
                        // Get sandesh from DB
                        val sandeshToUpdate =
                            groupDao.getGroupSandeshByIdAndGroupId(
                                data.messageId,
                                data.receiver
                            )
                        if (sandeshToUpdate != null) {
                            // Parse samagri as JsonObject safely
                            val samagriJson: JsonObject? =
                                runCatching {
                                    JsonParser.parseString(
                                        sandeshToUpdate.samagri
                                    ).asJsonObject
                                }.getOrNull()

                            if (samagriJson != null) {
                                // ✅ You can now update fields inside samagriJson
                                

                                if (!savedUri.isNullOrBlank()) {
                                    samagriJson.addProperty(
                                        DootawasMessage.BLUR_URI,
                                        savedUri
                                    )
                                }
                                samagriJson.addProperty(
                                    DootawasMessage.DOWNLOAD_PERCENT,
                                    100
                                )
                                samagriJson.addProperty(DootawasMessage.THUMBNAIL_URI, firstUriString)
                                // If you want to save back, convert to string
                                val updatedSamagri =
                                    samagriJson.toString()

                                // You can now call your update DAO method or save back
                                
                                groupDao.updateSamagri(
                                    data.messageId,
                                    updatedSamagri,
                                    data.receiver
                                )
                            }
                        }
                    } else {

                        // Get sandesh from DB
                        val sandeshToUpdate =
                            sandeshDao.getSandeshByIdAndUser(
                                data.messageId,
                                data.receiver
                            )
                        if (sandeshToUpdate != null) {
                            // Parse samagri as JsonObject safely
                            val samagriJson: JsonObject? =
                                runCatching {
                                    JsonParser.parseString(
                                        sandeshToUpdate.samagri
                                    ).asJsonObject
                                }.getOrNull()

                            if (samagriJson != null) {
                                // ✅ You can now update fields inside samagriJson
                                

                                if (!savedUri.isNullOrBlank()) {
                                    samagriJson.addProperty(
                                        DootawasMessage.BLUR_URI,
                                        savedUri
                                    )


                                }
                                samagriJson.addProperty(
                                    DootawasMessage.DOWNLOAD_PERCENT,
                                    100
                                )
                                samagriJson.addProperty(DootawasMessage.THUMBNAIL_URI, firstUriString)
                                // If you want to save back, convert to string
                                val updatedSamagri = samagriJson.toString()

                                // You can now call your update DAO method or save back
                                
                                sandeshDao.updateSamagri(data.messageId, updatedSamagri)
                            }
                        }
                    }
            }

            FileDataType.VIDEO,
            FileDataType.REPLY_VIDEO -> {

                // ✅ Check if multiple videos (ALL_DATA array exists)
                val dataArray = mainJsonObject.getAsJsonArray(DootawasMessage.ALL_DATA)

                val firstUriString = if (dataArray != null) {
                    // ✅ MULTIPLE VIDEOS — get first for preview
                    dataArray.firstOrNull()
                        ?.asJsonObject
                        ?.get(DootawasMessage.ORIGINAL_URI)
                        ?.asString
                } else {
                    // ✅ SINGLE VIDEO — original logic
                    mainJsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
                }

                if (firstUriString.isNullOrBlank()) {
                    Log.e("Upload", "No valid video URI found")
                    return
                }

                val file = File(firstUriString)

                val preview = processBlurredPreview(file, true)

                var savedUri: String? = null
                preview?.let {
                    savedUri = saveImagePreview(it)
                }

                mainJsonObject.addProperty(
                    DootawasMessage.IMAGE_PREVIEW,
                    preview ?: ""
                )

                // ✅ Only remove ORIGINAL_URI for single video (it uses DUPLICATE_URI)
                if (dataArray == null) {
                    mainJsonObject.remove(DootawasMessage.ORIGINAL_URI)
                }

                mainJsonObject.addProperty(DootawasMessage.FILE_PASS, data.password)
                mainJsonObject.addProperty(DootawasMessage.FILE_ID, data.uuid)


                insertUnsentData(
                    unsentSandeshDao,
                    UnsentSandesh(
                        messId = data.messageId,
                        receiver = data.receiver,
                        message = mainJsonObject.toString(),
                        dataType = DataType.MESSAGE,
                        isGroup = data.isGroup
                    )
                )

                if (data.isGroup) {
                    val sandeshToUpdate = groupDao.getGroupSandeshByIdAndGroupId(
                        data.messageId,
                        data.receiver
                    )
                    if (sandeshToUpdate != null) {
                        val samagriJson: JsonObject? = runCatching {
                            JsonParser.parseString(sandeshToUpdate.samagri).asJsonObject
                        }.getOrNull()

                        if (samagriJson != null) {
                            if (!savedUri.isNullOrBlank()) {
                                samagriJson.addProperty(DootawasMessage.BLUR_URI, savedUri)
                            }
                            samagriJson.addProperty(DootawasMessage.DOWNLOAD_PERCENT, 100)
                            // ✅ Add thumbnail like image does
                            if (!firstUriString.isNullOrBlank()) {
                                samagriJson.addProperty(DootawasMessage.THUMBNAIL_URI, firstUriString)
                            }
                            val updatedSamagri = samagriJson.toString()
                            groupDao.updateSamagri(data.messageId, updatedSamagri, data.receiver)
                        }
                    }
                } else {
                    val sandeshToUpdate = sandeshDao.getSandeshByIdAndUser(
                        data.messageId,
                        data.receiver
                    )
                    if (sandeshToUpdate != null) {
                        val samagriJson: JsonObject? = runCatching {
                            JsonParser.parseString(sandeshToUpdate.samagri).asJsonObject
                        }.getOrNull()

                        if (samagriJson != null) {
                            if (!savedUri.isNullOrBlank()) {
                                samagriJson.addProperty(DootawasMessage.BLUR_URI, savedUri)
                            }
                            samagriJson.addProperty(DootawasMessage.DOWNLOAD_PERCENT, 100)
                            // ✅ Add thumbnail like image does
                            if (!firstUriString.isNullOrBlank()) {
                                samagriJson.addProperty(DootawasMessage.THUMBNAIL_URI, firstUriString)
                            }
                            val updatedSamagri = samagriJson.toString()
                            sandeshDao.updateSamagri(data.messageId, updatedSamagri)
                        }
                    }
                }
            }

            FileDataType.DOCUMENT,
                FileDataType.REPLY_DOCUMENT -> {
                    val uriString = mainJsonObject
                        .get(DootawasMessage.ORIGINAL_URI)
                        ?.asString


                    if (!uriString.isNullOrBlank()) {


                        mainJsonObject.addProperty(
                            DootawasMessage.FILE_PASS,
                            data.password
                        )
                        mainJsonObject.addProperty(
                            DootawasMessage.FILE_ID,
                            data.uuid
                        )
                        mainJsonObject.addProperty(
                            DootawasMessage.DUPLICATE_URI,
                            ""
                        )

                        insertUnsentData(
                            unsentSandeshDao,
                            UnsentSandesh(
                                messId = data.messageId,
                                receiver = data.receiver,
                                message = mainJsonObject.toString(), // this is
                                dataType = DataType.MESSAGE,
                                isGroup = data.isGroup
                            )
                        )
                    }

                    if (data.isGroup) {
                        val sandeshToUpdate =
                            groupDao.getGroupSandeshByIdAndGroupId(
                                data.messageId,
                                data.receiver
                            )
                        if (sandeshToUpdate != null) {
                            // Parse samagri as JsonObject safely
                            val samagriJson: JsonObject? =
                                runCatching {
                                    JsonParser.parseString(
                                        sandeshToUpdate.samagri
                                    ).asJsonObject
                                }.getOrNull()
                            if (samagriJson != null) {

                                samagriJson.addProperty(
                                    DootawasMessage.DOWNLOAD_PERCENT,
                                    100
                                )
                                // If you want to save back, convert to string
                                val updatedSamagri =
                                    samagriJson.toString()
                                // You can now call your update DAO method or save back
                                groupDao.updateSamagri(
                                    data.messageId,
                                    updatedSamagri,
                                    data.receiver
                                )
                            }
                        }
                    } else {
                        // Get sandesh from DB
                        val sandeshToUpdate =
                            sandeshDao.getSandeshByIdAndUser(
                                data.messageId,
                                data.receiver
                            )
                        if (sandeshToUpdate != null) {
                            // Parse samagri as JsonObject safely
                            val samagriJson: JsonObject? =
                                runCatching {
                                    JsonParser.parseString(
                                        sandeshToUpdate.samagri
                                    ).asJsonObject
                                }.getOrNull()
                            if (samagriJson != null) {
                                samagriJson.addProperty(
                                    DootawasMessage.DOWNLOAD_PERCENT,
                                    100
                                )
                                // If you want to save back, convert to string
                                val updatedSamagri =
                                    samagriJson.toString()

                                // You can now call your update DAO method or save back

                                sandeshDao.updateSamagri(
                                    data.messageId,
                                    updatedSamagri
                                )
                            }
                        }
                    }
                }

                FileDataType.AUDIO,
                FileDataType.REPLY_AUDIO -> {

                    val uriString = mainJsonObject
                        .get(DootawasMessage.ORIGINAL_URI)
                        ?.asString

                    if (!uriString.isNullOrBlank()) {
                        val uri = uriString.toUri()
                        // -----------------------------------


                        // -----------------------------------
                        // 🔥 File size
                        // -----------------------------------

                    mainJsonObject.apply {

                        mainJsonObject.addProperty(
                            DootawasMessage.FILE_PASS,
                            data.password
                        )
                        mainJsonObject.addProperty(
                            DootawasMessage.FILE_ID,
                            data.uuid
                        )
                        //addProperty(DootawasMessage.IMAGE_PREVIEW, base64Image ?: "")
                    }

                    mainJsonObject.remove(DootawasMessage.DOWNLOAD_PERCENT)
                    insertUnsentData(
                        unsentSandeshDao,
                        UnsentSandesh(
                            messId = data.messageId,
                            receiver = data.receiver,
                            message = mainJsonObject.toString(), // this is your
                            dataType = DataType.MESSAGE,
                            isGroup = data.isGroup
                        )
                    )
                }
                }

                FileDataType.CONTACT,
                FileDataType.REPLY_CONTACT -> {
                    // TODO handle contact success (normal + reply)
                }

                FileDataType.LOCATION,
                FileDataType.REPLY_LOCATION -> {

                    val uriString = mainJsonObject
                        .get(DootawasMessage.ORIGINAL_URI)
                        ?.asString

                    if (!uriString.isNullOrBlank()) {

                            // -----------------------------------

                        val file = File(uriString)

                        val preview =
                            processBlurredPreview(
                                file,
                                false
                            )

                        // -----------------------------------
                        // 🔥 File size
                        // -----------------------------------

                        // Remove the saved URI before processing or logging
                        mainJsonObject.remove(
                            DootawasMessage.ORIGINAL_URI
                        )
                        mainJsonObject.apply {

                            addProperty(
                                DootawasMessage.IMAGE_PREVIEW,
                                preview ?: ""
                            )
                            addProperty(
                                DootawasMessage.FILE_PASS,
                                data.password
                            )
                            addProperty(
                                DootawasMessage.FILE_ID,
                                data.uuid
                            )
                        }

                        insertUnsentData(
                            unsentSandeshDao,
                            UnsentSandesh(
                                messId = data.messageId,
                                receiver = data.receiver,
                                message = mainJsonObject.toString(),
                                dataType = DataType.MESSAGE,
                                isGroup = data.isGroup
                            )
                        )
                    }
                }
                FileDataType.REPLY_MESSAGE -> {
                    // TODO handle reply message
                }

                FileDataType.GET_PROFILE_PIC -> {
                    // TODO handle profile pic upload success
                }

                FileDataType.GROUP_ACTION -> {
                    // TODO handle group action success
                }

                FileDataType.PROFILE_UPDATE -> {
                    // TODO handle profile update success
                }

                FileDataType.DELETE_PROFILE -> {
                    // TODO handle profile delete success
                }

            FileDataType.STORY -> {

                val storyData = JsonParser
                    .parseString(data.mainJson)
                    .asJsonObject

                val storyId = storyData.get("storyId")?.asString

                if (storyId.isNullOrBlank()) {
                    DLog.e("STORY: Missing storyId")
                    return
                }

                val durationMs = storyData.get("durationMs")?.asLong

                if (durationMs == null) {
                    DLog.e("STORY: Missing durationMs")
                    return
                }

                val visibility = storyData.get("visibility")?.asInt

                if (visibility == null) {
                    DLog.e("STORY: Missing visibility")
                    return
                }

                val storyUpdateRequest = JsonObject().apply {
                    addProperty(
                        JsonKeys.FILE_TOKEN_TYPE,
                        FileDataTypeToken.STORY_UPLOAD_COMPLETE.value
                    )

                    addProperty(
                        JsonKeys.MESSAGE_ID,
                        UUID.randomUUID().toString()
                    )

                    addProperty(
                        DootawasMessage.STORY_ID,
                        storyId
                    )

                    addProperty(
                        DootawasMessage.STORY_PASS,
                        data.password
                    )

                    addProperty(
                        DootawasMessage.MEDIA_TYPE,
                        storyData.get(DootawasMessage.MEDIA_TYPE)?.asString
                    )

                    addProperty(
                        DootawasMessage.DURATION,
                        durationMs
                    )

                    addProperty(
                        DootawasMessage.VISIBILITY,
                        visibility
                    )
                }

                DLog.i(
                    "STORY: Upload successful, sending DB update: $storyUpdateRequest"
                )

                insertUnsentData(
                    unsentSandeshDao,
                    UnsentSandesh(
                        messId = UUID.randomUUID().toString(),
                        receiver = "server",
                        message = storyUpdateRequest.toString(),
                        dataType = DataType.FILE_PROCESS,
                        isGroup = false
                    )
                )
            }                else -> {
                    // fallback
                }

        }
        deleteSendData(unsentFileDao,
            data.messageId,
            data.receiver
        )

    }



    // -----------------------------
    // FAILURE HANDLER
    // -----------------------------

    private suspend fun handleUploadFailure(
        data: UploadData,
        code: Int,
        unsentFileDao: UnsentFilesDao,
        sandeshDao: SandeshDao
    ) {

        deleteSendData(unsentFileDao,
            data.messageId,
            data.receiver
        )
//        when (data.dataType) {
//
//            FileDataType.IMAGE,
//            FileDataType.REPLY_IMAGE -> {
//                // TODO handle failure
//            }
//
//            FileDataType.VIDEO,
//            FileDataType.REPLY_VIDEO -> {
//                // TODO handle failure
//            }
//
//            else -> {
//                // fallback
//            }
//        }
    }



    // -----------------------------
    // Foreground Notification
    // -----------------------------

    private fun createForegroundInfo(
        messageId: String,
        progress: Int,
        text: String
    ): ForegroundInfo {

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("KnotX Upload") //todo changeToLan
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress.coerceAtMost(100), false)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .build()

        val notifId = messageId.hashCode().coerceAtLeast(1)

        return ForegroundInfo(
            notifId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }



    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val manager = applicationContext
                .getSystemService(NotificationManager::class.java)

            manager?.createNotificationChannel(channel)
        }
    }
}