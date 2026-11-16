package com.senghol.knotx


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status


class DootawasBroadcastReceiver : BroadcastReceiver() {

    private var listener: Listener? = null

    fun initListener(listener: Listener) {
        this.listener = listener
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == SmsRetriever.SMS_RETRIEVED_ACTION) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            if (status == null) {
                Log.e("DootawasReceiver", "❌ No status found in extras.")
                return
            }


            when (status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val sms = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)

                    val otp = parseOtp(sms)

                    if (otp.isNullOrEmpty()) {
                        Log.w("DootawasReceiver", "⚠️ OTP not found in message.")
                    } else {
                    }
                    listener?.onOtpReceived(otp)
                }

                CommonStatusCodes.TIMEOUT -> {
                    Log.w("DootawasReceiver", "⏱️ SMS Retriever timed out (no message in 5 min).")
                }

                else -> {
                    Log.w("DootawasReceiver", "⚠️ Unknown status code: ${status.statusCode}")
                }
            }
        } else {
            Log.w("DootawasReceiver", "🚫 Ignored unrelated broadcast action: ${intent?.action}")
        }
    }

    private fun parseOtp(sms: String?): String? {
        if (sms.isNullOrBlank()) return null

        // Regex to extract 4–8 digit OTP
        val regex = Regex("\\b\\d{4,8}\\b")
        val match = regex.find(sms)
        val otp = match?.value ?: sms.take(6) // fallback to first 6 chars if regex fails

        return otp
    }

    interface Listener {
        fun onOtpReceived(value: String?)
    }
}
