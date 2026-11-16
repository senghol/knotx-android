package com.senghol.knotx.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.senghol.knotx.connection.Knotx
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.razorpay.Checkout
import com.senghol.knotx.connection.AppSignatureHelper.TAG
import org.json.JSONObject
import java.io.ByteArrayOutputStream

interface PaymentListener {
    fun onOrderCreated(orderId: String,key: String,number: String,amount: Int)
    fun onOrderFailed(message: String)
}
// In a separate helper file
private fun getBase64FromResource(activity: Activity, resourceId: Int): String {
    // We use the activity parameter to get the resources
    val bitmap = BitmapFactory.decodeResource(activity.resources, resourceId) ?: return ""

    // Safety check: if the image is an XML/Adaptive icon, bitmap will be null

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()

    return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

// Then call it in your Activity like this:


fun startPaymentRazor(activity: Activity, orderId: String,key: String,number: String,amount: Int) {

    val checkout = Checkout()
    checkout.setKeyID(key) // 🔥 your LIVE key

//    checkout.setKeyID("rzp_test_STnuVMhKayONqL")
    /**
     * Set your logo here
     */
//    checkout.setImage(R.mipmap.ic_launcher_round)


        /**
         * Pass your payment options to the Razorpay Checkout as a JSONObject
         */
        try {
            val options = JSONObject()

            options.put("name", "Knotx")
            options.put("description", "Reference No. #123456")
//            options.put("image", "")
            // Convert your local drawable/mipmap to Base64
//            val localLogoBase64 = getBase64FromResource(activity,R.mipmap.ic_launcher_round)
//            options.put("image", localLogoBase64) //todo can set logo there but required server return logo
            options.put("order_id", orderId)
        options.put("theme.color", "#075942")
        options.put("currency", "INR")
        options.put("amount", amount)
        options.put("prefill.email", "")
        options.put("prefill.contact", number)
        val retryObj = JSONObject()
        retryObj.put("enabled", true)
        retryObj.put("max_count", 4)
        options.put("retry", retryObj)

        checkout.open(activity, options)
    } catch (e: Exception) {
        Log.e(TAG, "Error in starting Razorpay Checkout", e)
    }
}


@Composable
fun PaymentScreen(
    navController: NavController,
    knotx: Knotx
) {
    val activity = LocalActivity.current ?: return

    var isLoading by remember { mutableStateOf(false) }

    // 🔥 Register listener
    LaunchedEffect(Unit) {
        knotx.paymentListener = object : PaymentListener {

            override fun onOrderCreated(orderId: String,key: String,number: String,amount: Int) {
                isLoading = false

                startPaymentRazor(activity, orderId,key,number,amount)
            }

            override fun onOrderFailed(message: String) {
                isLoading = false
                Log.e("PAYMENT", message)
            }
        }
    }

    // 🔥 Cleanup (VERY IMPORTANT)
    DisposableEffect(Unit) {
        onDispose {
            knotx.paymentListener = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Button(
            onClick = {
                if (!isLoading) {
                    isLoading = true
                    knotx.createOrderID(20)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Please wait..." else "Pay ₹1")
        }

        TextButton(
            onClick = {
                navController.popBackStack()
            }
        ) {
            Text("Skip")
        }
    }
}