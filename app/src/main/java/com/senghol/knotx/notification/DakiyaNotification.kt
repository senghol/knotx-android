package com.senghol.knotx.notification

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.calling.data.signaling.CallData
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.connection.MessageSyncWorker
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.objects.ChunkManager
import com.senghol.knotx.utils.objects.DLog
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class DakiyaNotification : FirebaseMessagingService() {

    private val knotx: Knotx by lazy { (applicationContext as KnotxApp).knotx }

    companion object {
        private const val TAG = "DakiyaFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        knotx.sendToken(token)            // your existing retry logic
    }

    override fun onDeletedMessages() {



        MessageSyncWorker.enqueueOnce(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            when {
                data.containsKey("chunk") -> {
                    val chunkJson = data["chunk"] ?: return
                    val mid = JSONObject(chunkJson).getString("mid")
                    if (!ChunkManager.isDone(applicationContext, mid)) {
                        showPlaceholderForChunk(chunkJson)          // visible result right away
                    }
                    ChunkManager.handleChunk(applicationContext, chunkJson) { fullCipher, json ->
                        rebuildAndProcess(json, fullCipher, json.getString("mid"))
                    }
                }
                data.containsKey("data") -> data["data"]?.let { handleFullMessage(it) }
                else -> Unit
            }
        } catch (e: Exception) {
            Log.e(TAG, "onMessageReceived failed", e)
        }
    }

    private fun showPlaceholderForChunk(chunkJson: String) {
        try {
            val j = JSONObject(chunkJson)
            val target = if (j.optBoolean("isGroup", false)) j.optString("groupId")
            else j.optString("sender")
            if (target.isNotBlank()) knotx.showPlaceholderNotification(target)
        } catch (e: Exception) {
            Log.w(TAG, "placeholder failed", e)
        }
    }

    private fun rebuildAndProcess(json: JSONObject, fullCipher: String, mid: String) {
        try {
            val isGroup = json.getBoolean("isGroup")

            val sandesh = JSONObject().apply {
                put("receiver", json.getString("receiver"))
                put("cipherMessageType", json.optString("cipherMessageType", ""))
                put("cipherMessage", fullCipher)
                put("isGroup", isGroup)
                if (isGroup && json.has("groupId")) put("groupId", json.getString("groupId"))
                if (json.has("rRN")) put("rRN", json.getBoolean("rRN"))
                if (json.has("userNameId")) put("userNameId", json.getString("userNameId"))
            }

            val finalJson = JSONObject().apply {
                put("sender", json.getString("sender"))
                put("receiver", json.getString("receiver"))
                put("timeStamp", json.getString("timeStamp"))
                put("sandesh", sandesh.toString())          // kept as String, your pipeline expects it
                put("messageId", mid)
                put("dataType", json.getInt("dataType"))
                put("storageType", json.getInt("storageType"))
            }

            handleFullMessage(finalJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "rebuildAndProcess failed", e)
            // Tell the user something arrived; opening the app runs your direct sync
            val target = if (json.optBoolean("isGroup", false)) json.optString("groupId")
            else json.optString("sender")
            if (target.isNotBlank()) knotx.showFallbackNotification(target)
        }
    }

    private fun enqueueMessage(mid: String, json: String) {
        // Save to disk: Work Data is limited to ~10KB and this survives process death.
        val safeMid = ChunkManager.safe(mid)
        val dir = File(applicationContext.filesDir, "pending_fcm").apply { mkdirs() }
        File(dir, "$safeMid.json").writeText(json)

        val request = OneTimeWorkRequestBuilder<FcmWorker>()
            .setInputData(workDataOf("mid" to safeMid))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork("msg_$safeMid", ExistingWorkPolicy.KEEP, request)   // dedupes
    }

    private fun handleFullMessage(messageObjectJson: String) {
        val jsonMessage = JsonParser.parseString(messageObjectJson).asJsonObject
        val retrievedValue = jsonMessage[JsonKeys.DATA_TYPE]?.asInt ?: return

        when (DataType.fromInt(retrievedValue)) {
            DataType.MESSAGE -> {
                val sender = jsonMessage[JsonKeys.SENDER]?.asString
                val encryptedSandesh = jsonMessage[JsonKeys.SANDESH]?.asString
                var notificationTarget = sender
                if (!encryptedSandesh.isNullOrBlank()) {
                    try {
                        val sandeshJson = JsonParser.parseString(encryptedSandesh).asJsonObject
                        if (sandeshJson["isGroup"]?.asBoolean == true) {
                            notificationTarget = sandeshJson["groupId"]?.asString
                        }
                    } catch (_: Exception) {
                    }
                }
                if (!notificationTarget.isNullOrBlank()) {
                    knotx.showPlaceholderNotification(notificationTarget)
                }
                val mid = jsonMessage["messageId"]?.asString ?: UUID.randomUUID().toString()
                enqueueMessage(mid, messageObjectJson)
            }

            DataType.CALL_DATA -> {
                DLog.i("CALL DATA gson $jsonMessage")
                try {
                    val callData = Gson().fromJson(jsonMessage, CallData::class.java)
                    knotx.dispatchCallData(callData)
                } catch (e: Exception) {
                    DLog.e("Failed to parse CALL_DATA: ${e.message}", e)
                }
            }

            else -> Unit
        }
    }
}