package com.senghol.knotx


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.datastore.DootawasStore

import com.senghol.knotx.ui.dialogs.AppDialogs.PaymentSuccessDialog
import com.senghol.knotx.ui.navigation.AppNavGraph
import com.senghol.knotx.ui.screens.PasscodeScreen
import com.senghol.knotx.ui.theme.DootawasTheme
import com.senghol.knotx.utils.LocaleHelper
import com.senghol.knotx.utils.MiuiPermissionHelper

import com.senghol.knotx.utils.ScreenReceiver

import com.senghol.knotx.utils.objects.PrefsHelper
import kotlinx.coroutines.Job


//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private lateinit var knotx: Knotx
    private lateinit var dootawasStore: DootawasStore
    private lateinit var screenReceiver: ScreenReceiver
    private var disconnectJob: Job? = null
    private val isRegisteredState = mutableStateOf(false)
    private val paymentSuccessState = mutableStateOf(false)
    override fun onPaymentSuccess(
        razorpayPaymentId: String?,
        paymentData: PaymentData?
    ) {
        try {
            val orderId = paymentData?.orderId
            val paymentId = paymentData?.paymentId ?: razorpayPaymentId
            val signature = paymentData?.signature


            // 🔥 Trigger UI state
            paymentSuccessState.value = true

            // 🔐 TODO: Send to backend for verification
            // sendPaymentToServer(orderId, paymentId, signature)

        } catch (e: Exception) {
            Log.e("PAYMENT", "Exception in success callback", e)
        }
    }

    override fun onPaymentError(
        code: Int,
        response: String?,
        paymentData: PaymentData?
    ) {
        try {
            val orderId = paymentData?.orderId
            val paymentId = paymentData?.paymentId

            Log.e("PAYMENT", "❌ FAILED")
            Log.e("PAYMENT", "code: $code")
            Log.e("PAYMENT", "reason: $response")
            Log.e("PAYMENT", "orderId: $orderId")
            Log.e("PAYMENT", "paymentId: $paymentId")

            // 🔥 Optional: show error UI / toast
            // paymentErrorState.value = errorReason

        } catch (e: Exception) {
            Log.e("PAYMENT", "Exception in error callback", e)
        }
    }
    private val sharedTextState = mutableStateOf<String?>(null)
    private val sharedUrisState = mutableStateOf<List<Uri>?>(null)
    private val chatSenderState = mutableStateOf<String?>(null)
    private fun handleAppIntent(intent: Intent?) {


        intent ?: return

        // ✅ Prevent duplicate handling
        if (intent.getBooleanExtra("intent_handled", false)) return
        intent.putExtra("intent_handled", true)

        intent.getStringExtra("open_chat_user")?.let { sender ->

            chatSenderState.value = sender
            PrefsHelper.setDirectId(this, sender)
        }
        // 🔥 2️⃣ Share Intent
        when (intent.action) {

            Intent.ACTION_SEND -> {
                when {
                    intent.type == "text/plain" -> {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (!text.isNullOrEmpty()) {
                            sharedTextState.value = text
                        }
                    }

                    intent.type?.startsWith("image/") == true ||
                            intent.type?.startsWith("video/") == true ||
                            intent.type?.startsWith("application/") == true -> {

                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        }

                        if (uri != null) {
                            sharedUrisState.value = listOf(uri)
                        }
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }

                if (!uris.isNullOrEmpty()) {
                    sharedUrisState.value = uris
                }
            }
        }
    }

    fun changeAppIcon(context: Context, useAlternate: Boolean) {
        val pm = context.packageManager
        val defaultIcon = ComponentName(context, "${context.packageName}.DefaultIcon")
        val alternateIcon = ComponentName(context, "${context.packageName}.AlternateIcon")

        pm.setComponentEnabledSetting(
            if (useAlternate) alternateIcon else defaultIcon,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        pm.setComponentEnabledSetting(
            if (useAlternate) defaultIcon else alternateIcon,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
    private var isThemeReady = false
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleAppIntent(intent)


        // Room DAOs
        val database = DootawasDatabase.getDatabase(this)
        val dootawasDB = (applicationContext as KnotxApp).dootawasDB
        val upbhogtaDao = database.upbhogtaDao()
        val contactDao = database.contactDao()
        val callsDao = database.callSessionDao()
        val sandeshDao = database.sandeshDao()
        val groupDao = database.groupDao()
        knotx = (applicationContext as KnotxApp).knotx
        dootawasStore = (applicationContext as KnotxApp).dootawasStore
        knotx.isAppRunning = true
        enableEdgeToEdge()


        PrefsHelper.init(this)
        val isRegistered = PrefsHelper.getIsRegistered(this)

        isRegisteredState.value = isRegistered
        (applicationContext as KnotxApp).tracker.untrigger()



        setContent {
            val navController = rememberNavController()
            val passcode by knotx.getPasscode().collectAsState(initial = null)
            var isPasscodeVerified by remember { mutableStateOf(false) }

            DootawasTheme(viewModel = knotx) {
                Surface {
                    if (passcode != null && !isPasscodeVerified) {
                        // Show Passcode Screen
                        PasscodeScreen(
                            onPasscodeEntered = { enteredPasscode ->
                                if (enteredPasscode == passcode) {
                                    isPasscodeVerified = true
                                } else {
                                    // Show error or shake effect

                                }
                            }
                        )
                    } else {

                        AppNavGraph(
                            navController = navController,
                            isRegistered = isRegistered,
                            upbhogtaDao = upbhogtaDao,
                            contactDao = contactDao,
                            sandeshDao = sandeshDao,
                            knotx = knotx,
                            dootawasStore = dootawasStore,
                            groupDao = groupDao,

                            // ✅ FINAL FIXED VALUES
                            sharedText = sharedTextState.value,
                            sharedUris = sharedUrisState.value,
                            chatSender = chatSenderState.value,

                            // ✅ RESET PROPERLY
                            onShareHandled = {
                                sharedTextState.value = null
                                sharedUrisState.value = null
                                chatSenderState.value = null
                            },
                            onChatHandled = { chatSenderState.value = null },
                            dootawasDatabase = database,
                            dootawasDB = dootawasDB
                        )
                    }
                }
            }
            val paymentSuccess by paymentSuccessState

            if (paymentSuccess) {
                PaymentSuccessDialog(
                    onDismiss = {
                        paymentSuccessState.value = false
                    }
                )
                knotx.getContributionsFromServer()
            }
        }


        knotx.resetNotificationCount()
        knotx.clearFallbackNotifications()
        knotx.clearAllMissedCallNotifications()
//        changeAppIcon(this, true)


    }



    private fun applyLocale(context: Context): Context {

        return if (PrefsHelper.getUseSystemLanguage(context)) {

            LocaleHelper.useSystemLocale(context)

        } else {

            val language = PrefsHelper.getAppLanguage(context)

            if (language != null) {
                LocaleHelper.setLocale(context, language)
            } else {
                LocaleHelper.useSystemLocale(context)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    fun checkMiuiPermissions(activity: Activity) {
        if (!MiuiPermissionHelper.isMiui()) return

        AlertDialog.Builder(activity)
            .setTitle("Enable Call Screen")
            .setMessage(
                "Knotx needs two permissions to show incoming calls on your lock screen:\n\n" +
                        "1. Display pop-up windows while running in background\n" +
                        "2. Show on Lock screen\n\n" +
                        "Please enable both in the next screen."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                MiuiPermissionHelper.openMiuiPermissionSettings(activity)
            }
            .setNegativeButton("Later", null)
            .show()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.let {
            setIntent(it)
            handleAppIntent(it)
        }
    }
    override fun onDestroy() {
        super.onDestroy()

    }
}
