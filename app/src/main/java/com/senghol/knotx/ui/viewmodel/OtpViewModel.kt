package com.senghol.knotx.ui.viewmodel

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.gson.JsonObject
import com.senghol.knotx.DootawasBroadcastReceiver
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.utils.AESKotlinUtil.encryptForSMS
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.objects.OtpResult
import com.senghol.knotx.utils.objects.PrefKeys
import com.senghol.knotx.utils.objects.SessionKeys
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpViewModel : ViewModel() {

    // -------------------------
    // UI STATE
    // -------------------------
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // -------------------------
    // TIMER
    // -------------------------
    var otpEndTime by mutableLongStateOf(0L)
        private set

    fun startTimerIfNeeded() {
        if (otpEndTime == 0L) {
            otpEndTime = System.currentTimeMillis() + 59_000
        }
    }

    fun getRemainingSeconds(): Int {
        val remaining = (otpEndTime - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0).toInt()
    }

    // -------------------------
    // OTP SEND CONTROL
    // -------------------------
    private var otpSent = false

    fun askNewOTP(knotx: Knotx, phoneNumber: String) {
        if (otpSent) return
        otpSent = true

        viewModelScope.launch {
            try {


                val sessionKey = SessionKeys.serverSessionId

                val otpJson = JsonObject().apply {
                    addProperty(JsonKeys.USER_NUMBER, phoneNumber)
                    addProperty(JsonKeys.TOKEN, sessionKey)
                    addProperty(JsonKeys.DATA_TYPE, DataType.SEND_OTP.value)
                }

                knotx.sendRegisterMessage(otpJson)
                knotx.generateRSAKeyPairIfNotExists("senghol_user_key")

            } catch (e: Exception) {
                otpSent = false
                _uiMessage.value = "Failed to send OTP"
            }
        }
    }

    // -------------------------
    // SUBMIT OTP
    // -------------------------
    fun submitOtp(
        knotx: Knotx,
        phoneNumber: String,
        otp: String,
        context: Context
    ) {
        viewModelScope.launch {

            val sessionKey = SessionKeys.serverSessionId
            val aesKey = SessionKeys.serverAESKey
            val password = SessionKeys.password

            if (sessionKey == null || aesKey == null || password == null) {
                _uiMessage.value = "Session expired"
                return@launch
            }

            _isLoading.value = true
            // 🚀 👉 PASTE YOUR BYPASS RIGHT HERE
            val isReviewUser = phoneNumber == "+918512015149"
            if (isReviewUser) {
                context.userDataStore.edit { preferences ->
                    preferences[PrefKeys.REGION] = "IN"
                    preferences[PrefKeys.USER_NUMBER] =
                        knotx.normalizePhoneNumber(phoneNumber, "IN")!!
                }
                SessionKeys.serverSessionId = null
                SessionKeys.serverAESKey = null
                SessionKeys.password = null

                _isSuccess.value = true

                knotx.addDummyContact()
                return@launch
            }
            try {
                val encrypted = withContext(Dispatchers.Default) {
                    encryptForSMS(password, sessionKey, aesKey)
                }

                val json = JsonObject().apply {
                    addProperty(JsonKeys.USER_NUMBER, phoneNumber)
                    addProperty(JsonKeys.SANDESH, encrypted)
                    addProperty(JsonKeys.OTP_SUBMIT, otp)
                    addProperty(JsonKeys.TOKEN,knotx.requestIntegrityToken())
                }

                knotx.submitOtp(json)

            } catch (e: Exception) {
                _isLoading.value = false
                _uiMessage.value = "Something went wrong"
            }
        }
    }

    // -------------------------
    // LISTENER (ONLY ONCE)
    // -------------------------
    fun startObserving(
        knotx: Knotx,
        context: Context,
        phoneNumber: String,
        countryShortName: String
    ) {
        viewModelScope.launch {

            knotx.otpResult.collect { result ->

                _isLoading.value = false

                when (result) {

                    OtpResult.SUCCESS -> {



                        SessionKeys.serverSessionId = null
                        SessionKeys.serverAESKey = null
                        SessionKeys.password = null

                        _isSuccess.value = true
                    }

                    else -> {
                        val msg = when (result) {
                            OtpResult.INVALID_OTP -> "Invalid OTP"
                            OtpResult.LIMIT_REACHED -> "Too many attempts"
                            OtpResult.INVALID_SESSION -> "Session expired"
                            OtpResult.OTP_EXPIRED -> "OTP expired"
                            OtpResult.INVALID_FORMAT -> "Invalid format"
                            else -> "Something went wrong"
                        }

                        viewModelScope.launch {
                            _uiMessage.value = msg
                            delay(1000) // ⏳ show for 1 sec
                            _uiMessage.value = null
                        }
                    }
                }
            }
        }
    }
    private val _autoOtp = MutableStateFlow<String?>(null)
    val autoOtp = _autoOtp.asStateFlow()
    private var smsReceiver: DootawasBroadcastReceiver? = null
    // Outside the function

    fun startSmsObserver(context: Context) {
        val client = SmsRetriever.getClient(context)
        client.startSmsRetriever()

        // 1. Initialize the property, not a local 'val'
        smsReceiver = DootawasBroadcastReceiver()

        smsReceiver?.initListener(object : DootawasBroadcastReceiver.Listener {
            override fun onOtpReceived(value: String?) {
                _autoOtp.value = value

                // 2. Unregister once you have what you need!
                stopSmsObserver(context)
            }
        })

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)

// Use the explicit Exported flag for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                smsReceiver,
                filter,
                SmsRetriever.SEND_PERMISSION, // This adds security so only Google can send it
                null,
                Context.RECEIVER_EXPORTED
            )
        } else {
            // For older versions, the SEND_PERMISSION is enough
            ContextCompat.registerReceiver(
                context,
                smsReceiver,
                filter,
                SmsRetriever.SEND_PERMISSION,
                null,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    fun stopSmsObserver(context: Context) {
        val receiver = smsReceiver ?: return
        smsReceiver = null

        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
        }
    }

}