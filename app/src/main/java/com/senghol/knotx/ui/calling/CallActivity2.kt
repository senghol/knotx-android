package com.senghol.knotx.ui.calling

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.telecom.Connection
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi

import androidx.compose.runtime.*
import androidx.core.view.WindowCompat

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.connection.SocketManager
import com.senghol.knotx.ui.theme.DootawasTheme
import com.senghol.knotx.ui.viewmodel.CallViewModel
import com.senghol.knotx.utils.data.CallEvent
import com.senghol.knotx.utils.data.CallUpbhogta
import com.senghol.knotx.utils.enums.CallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.apply
import kotlin.jvm.java
import kotlin.let
import kotlin.text.isNullOrBlank
import kotlin.text.trimIndent

class CallActivity2 : ComponentActivity() {
//    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var knotx: Knotx

    private lateinit var viewModel: CallViewModel
    private var isInPipMode by mutableStateOf(false)

    private val pool = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun generateRoomId(length: Int = 6): String {
        return (1..length).map { pool.random() }.joinToString("")
    }

    @SuppressLint("SourceLockedOrientationActivity")
//    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        // AFTER

        super.onCreate(savedInstanceState)
        val isTabletOrDesktop =
            resources.configuration.smallestScreenWidthDp >= 600

        requestedOrientation = if (!isTabletOrDesktop) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        setupProximitySensor()
        // 🛡️ Request to dismiss keyguard (unlock screen if possible)





        // ✅ Keep screen ON during call (replaces wake lock!)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Extract intent data
        val userNumber = intent.getStringExtra("userNumber")
        val isVideo = intent.getBooleanExtra("video", false)
        val isOutgoing = intent.getBooleanExtra("outgoing", false)
        val roomId = intent.getStringExtra("roomId")
            ?.takeIf { it.isNotBlank() }
            ?: generateRoomId()
        // 🖨️ Pretty print values

        // Early return if missing data
        if (userNumber.isNullOrBlank()) {
            finish()
            return
        }

        // Init dependencies
        knotx = (applicationContext as KnotxApp).knotx

        // When call starts
        SocketManager.isCallRunning = true
        if (knotx.isCallAlreadyEnded(roomId)) {
            finish()
            return
        }

        val factory = CallViewModelFactory(application, knotx,roomId)
        viewModel = ViewModelProvider(this, factory)[CallViewModel::class.java]


        knotx.createNotificationChannels()
        // 🔑 ONE entry point
        viewModel.initCallUiState(
            remoteNumber = userNumber,
            isVideo = isVideo,
            isOutgoing = isOutgoing
        )
        
        // UI
        setContent {
            DootawasTheme(viewModel = knotx) {
                val statusCall by viewModel.callStatus.collectAsState()
                val uiState by viewModel.uiState.collectAsState()
                var upbhogta by remember { mutableStateOf<CallUpbhogta?>(null) }

                // Fetch user from DB
                LaunchedEffect(Unit) {
                    upbhogta = knotx.createCallUpbhogta(userNumber)
                }

                if (upbhogta != null) {
                    // Keep screen on
                    DisposableEffect(Unit) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onDispose {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    // Cleanup
                    DisposableEffect(Unit) {
                        onDispose { viewModel.endCall() }
                    }

                    // Back press
                    BackHandler(enabled = uiState.callState == CallState.InCall || uiState.callState == CallState.Ringing) {
                        if (uiState.callState == CallState.InCall) {
                            tryEnterPipMode()
                        } else {
                            tryEnterPipMode()
//                            viewModel.endCall()
                        }
                    }

                    val permissions = remember {
                        mutableListOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CALL_PHONE // 🔥 REQUIRED
                        ).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                add(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                add(Manifest.permission.BLUETOOTH)
                                add(Manifest.permission.BLUETOOTH_ADMIN)
                            }
                        }
                    }
//
//                    val callPermissions = rememberMultiplePermissionsState(permissions)
//
//                    LaunchedEffect(Unit) {
//                            callPermissions.launchMultiplePermissionRequest()
//                    }
//
//                    LaunchedEffect(callPermissions.permissions) {
//                        callPermissions.permissions.forEach { permissionState ->
//
//                        }
//                    }


//                    LaunchedEffect(callPermissions.allPermissionsGranted, isOutgoing) {
//                        if (isOutgoing && callPermissions.allPermissionsGranted) {
//
//
//                            val callInitiate = JsonObject().apply {
//                                addProperty(JsonKeys.RECEIVER, upbhogta!!.userNumber)
//                                addProperty(JsonKeys.DATA_TYPE, DataType.CALL_DATA.value)
//                                addProperty(JsonKeys.MESSAGE_ID, UUID.randomUUID().toString())
//                                addProperty(
//                                    JsonKeys.CALL_DATA_TYPE,
//                                    if (isVideo) CallDataType.VIDEO_RECEIVED.value
//                                    else CallDataType.AUDIO_RECEIVED.value
//                                )
//                                addProperty(JsonKeys.SANDESH, "New Call")
//                                addProperty(JsonKeys.PROFILE_ID,roomId)
//                            }
//
////                            knotx.writeJsonToSessionCall(callInitiate)
//
//                            upbhogta?.userName?.let {
//                                viewModel.startOutgoingCall(isVideo, it, upbhogta!!.userNumber)
//                            }
//                        } else if (!isOutgoing) {
//                            upbhogta?.userName?.let {
//
//                                viewModel.receiveIncomingCall(it, isVideo, upbhogta!!.userNumber)
//                            }
//                        }
//                    }


                    // BEAUTIFUL BLURRED BACKGROUND - APPLIED ONCE
                    SamsungCallScreenBackground {

                    // UI Switch
                    when (uiState.callState) {
                        CallState.Ringing -> {
                            val autoAnswer = intent.getBooleanExtra("autoAnswer", false)
                            if (autoAnswer) {
                                LaunchedEffect(true) {
                                    delay(1000L) // ⏱️ auto-answer delay
                                    viewModel.onEvent(CallEvent.AcceptCall)
                                }
                                ConnectingView(isVideoCall = uiState.isVideoCall)
                            }else{
                                IncomingCallView(uiState, uiState.isVideoCall, viewModel::onEvent )
                            }
                            IncomingCallForegroundService.cancelCallNotification(this, roomId)
                            IncomingCallForegroundService.stop(this)
                        }
                        CallState.Calling -> OutgoingCallView(uiState,statusCall, uiState.isVideoCall, viewModel::onEvent)
                        CallState.Connecting -> {
                            LaunchedEffect(Unit) {
                                delay(6_000)
                                if (viewModel.uiState.value.callState == CallState.Connecting) {
                                    Toast.makeText(this@CallActivity2, "Failed. Please try again.", Toast.LENGTH_SHORT).show()
                                    viewModel.endCall()
                                }
                            }
                            ConnectingView(isVideoCall = uiState.isVideoCall)
                        }

                        CallState.InCall -> InCallView(
                            uiState = uiState,
                            isVideoCall = uiState.isVideoCall,
                            isInPipMode = isInPipMode,
                            onEvent = viewModel::onEvent,
                            onEnterPip = { tryEnterPipMode() }
                        )


                        CallState.Ended -> {
                                startActivity(
                                    Intent(this, CallEndedActivity::class.java).apply {
                                        putExtra("video", uiState.isVideoCall)
                                        putExtra("duration", uiState.callDuration)
                                        putExtra("name", upbhogta?.userName)
                                        putExtra("number", upbhogta?.userNumber)
                                        putExtra("image", upbhogta?.profileImageUrl)
                                    }
                                )
                                finish()
//
//                            // ⏳ Finish activity after 3 seconds
//                            LaunchedEffect(Unit) {
//                                delay(3_000)
//                                finish()
//                            }
//                            CallEndedView(
//                                isVideoCall = uiState.isVideoCall,
//                                callDuration = uiState.callDuration, // Provide this in state
//                                userName = upbhogta?.userName,
//                                userNumber = upbhogta!!.userNumber,
//                                userImageUrl = upbhogta?.profileImageUrl, // Optional
//                                onRecall = {
//                                        initiateCall(upbhogta!!, isOutgoing, uiState.isVideoCall)
//                                },
//                                onBack = { finish() }
//                            )
                        }
                        CallState.BusyOnAnotherCall -> {
                            Toast.makeText(this, "User is busy on another call", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        else -> {}
                    }

                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.callState == CallState.InCall && !state.isVideoCall) {
                    registerProximitySensor()
                } else {
                    unregisterProximitySensor()
                }
            }
        }
    }


    private val pipParamsBuilder = PictureInPictureParams.Builder()

    private fun tryEnterPipMode() {
        if (viewModel.uiState.value.callState == CallState.InCall
        ) {
            enterPictureInPictureMode(
                pipParamsBuilder.setAspectRatio(Rational(9, 16)).build()
            )
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        // 1. Turn off screen-on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)



        unregisterProximitySensor()
// When call ends (in all exit paths!)
        SocketManager.isCallRunning = false
        // 3. End call in ViewModel (safe even if not initialized)
        if (this::viewModel.isInitialized) {
            viewModel.endCall()

        }

        // 4. Clean up Telecom / manual connection — THE CORRECT WAY
        ConnectionDootaws.currentConnection?.let { conn ->

            if (conn.state == Connection.STATE_RINGING) {
                conn.onReject()
            } else {
                conn.onDisconnect()
            }
        }

        // 5. Reset companion object state (critical to avoid leaks & ghost calls)
        ConnectionDootaws.clearAll()  // This clears both connection and listener
    }




    @RequiresApi(Build.VERSION_CODES.TIRAMISU) // API 33
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }


    @Deprecated("Deprecated in android.app.Activity")
    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipMode = isInPictureInPictureMode
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        tryEnterPipMode()
    }


    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!viewModel.uiState.value.isVideoCall &&
                viewModel.uiState.value.callState == CallState.InCall) {
                val isNear = event.values[0] < (proximitySensor?.maximumRange ?: 5f)
                if (isNear) {
                    wakeLock.acquire(10 * 60 * 1000L) // screen off
                } else {
                    if (wakeLock.isHeld) wakeLock.release() // screen on
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun setupProximitySensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "knotx:proximity"
        )
    }

    private fun registerProximitySensor() {
        proximitySensor?.let {
            sensorManager.registerListener(proximityListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }
    private fun unregisterProximitySensor() {
        sensorManager.unregisterListener(proximityListener)
        if (::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
    }

}
