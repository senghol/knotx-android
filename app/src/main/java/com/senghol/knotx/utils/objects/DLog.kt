package com.senghol.knotx.utils.objects

import android.util.Log
import com.senghol.knotx.BuildConfig
import org.json.JSONObject


object DLog {

    private const val TAG = "Knotx"
    private val enabled = BuildConfig.DEBUG

    // --- Debug-only: silent in release ---

    fun d(msg: String) { if (enabled) Unit }
    fun i(msg: String) { if (enabled) Unit }
    fun w(msg: String) { if (enabled) Log.w(TAG, msg) }

    // Lazy versions: the string is only built when logging is on
    inline fun d(msg: () -> String) { if (BuildConfig.DEBUG) Unit }
    inline fun i(msg: () -> String) { if (BuildConfig.DEBUG) Unit }

    // --- Errors: allowed in release, but sanitized ---

    fun e(msg: String, tr: Throwable? = null) {
        if (enabled) {
            Log.e(TAG, msg, tr)
        } else {
            // Release: no custom message, no stack trace, only the exception type
            Log.e(TAG, "error: ${tr?.javaClass?.simpleName ?: "unknown"}")
        }
    }

    fun json(json: String) {
        if (!enabled) return
        try {
        } catch (e: Exception) {
        }
    }

    fun network(direction: String, msg: String) {
        if (enabled) Unit
    }

    const val TAG_PUBLIC = TAG
}