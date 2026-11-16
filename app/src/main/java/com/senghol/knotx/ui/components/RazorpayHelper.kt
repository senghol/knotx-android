package com.senghol.knotx.ui.components

import android.app.Activity
import android.util.Log
import com.razorpay.Checkout
import org.json.JSONObject

object RazorpayHelper {

    fun startPayment(activity: Activity, orderId: String) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_live_xxxxx") // 🔥 your LIVE key

        try {
            val options = JSONObject()

            options.put("name", "Your App Name")
            options.put("description", "Support fee to cover SMS cost")
            options.put("currency", "INR")
            options.put("amount", 100)
            options.put("order_id", orderId)

            val prefill = JSONObject()
            prefill.put("email", "test@example.com")
            prefill.put("contact", "9999999999")

            options.put("prefill", prefill)

            checkout.open(activity, options)

        } catch (e: Exception) {
            Log.e("RAZORPAY", "Error in payment", e)
        }
    }
}