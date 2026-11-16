package com.senghol.knotx.utils

import android.os.Handler
import android.os.Looper
import com.senghol.knotx.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
object NetworkClient {

    val okHttpClient: OkHttpClient by lazy {

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }
}
object ReceiptSender {

    private const val TAG = "ReceiptSender"
    private val client = NetworkClient.okHttpClient

    fun send(
        jsonBody: String,
        onResult: (Boolean, String) -> Unit
    ): Call {


        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BuildConfig.SPRING_URL)
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        call.enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                onResult(false, e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()


                    if (!it.isSuccessful) {
                        onResult(false, "HTTP ${it.code}: $body")
                        return
                    }

                    onResult(true, body)
                }
            }
        })

        return call
    }
}
object ReceiptQueue {

    private const val TAG = "ReceiptQueue"

    private val queue = ArrayDeque<String>()
    private var isProcessing = false

    fun enqueue(json: String) {
        queue.addLast(json)


        processNext()
    }

    private fun processNext() {
        if (isProcessing) {
            return
        }

        val item = queue.removeFirstOrNull()

        if (item == null) {
            return
        }

        isProcessing = true


        ReceiptSender.send(item) { success, message ->


            if (!success) {
                queue.addFirst(item)
            }

            isProcessing = false

            processNext()
        }
    }
}