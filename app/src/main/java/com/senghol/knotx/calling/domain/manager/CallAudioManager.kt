package com.senghol.knotx.calling.domain.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import com.senghol.knotx.calling.domain.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class CallAudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

data class CallAudioDevice(
    val route: CallAudioRoute,
    val name: String
)

/**
 * Owns everything about HOW call audio is routed.
 *
 * Mic mute is NOT handled here any more. The single source of truth for the
 * mic is CallMediaManager / WebRtcManager (track.setEnabled). Keeping a second
 * mic state here (plus audioManager.isMicrophoneMute) made the two disagree.
 */
@Singleton
class CallAudioManager @Inject constructor(
    private val context: Context,
    @Suppress("unused") private val webRtcManager: WebRtcManager
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isSpeakerEnabled = MutableStateFlow(false)
    val isSpeakerEnabled: StateFlow<Boolean> =
        _isSpeakerEnabled.asStateFlow()

    private val _availableAudioDevices =
        MutableStateFlow<List<CallAudioDevice>>(emptyList())
    val availableAudioDevices: StateFlow<List<CallAudioDevice>> =
        _availableAudioDevices.asStateFlow()

    private val _audioRoute = MutableStateFlow(CallAudioRoute.EARPIECE)
    val audioRoute: StateFlow<CallAudioRoute> = _audioRoute.asStateFlow()

    private var isDeviceMonitoring = false

    // Per-call routing state
    private var callType: CallType = CallType.VOICE
    private var userChoice: CallAudioRoute? = null
    private var focusRequest: AudioFocusRequest? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Call once per call, BEFORE WebRTC starts the mic (i.e. before
     * prepareLocalMedia / before sending invite or accept).
     */
    fun initialize(callType: CallType) {
        this.callType = callType
        userChoice = null

        requestAudioFocus()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        startDeviceMonitoring()      // also refreshes the device list
        applyBestRoute(force = true) // voice -> earpiece, video -> speaker, headset wins
    }

    fun reset() {
        stopDeviceMonitoring()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            stopLegacyBluetoothSco()

            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }

        audioManager.mode = AudioManager.MODE_NORMAL
        abandonAudioFocus()

        // NOTE: the old version called webRtcManager.setAudioEnabled(false)
        // here. That ran AFTER WebRtcManager.dispose() had reset the flag to
        // true, so the NEXT call started with the mic disabled.

        userChoice = null
        _isSpeakerEnabled.value = false
        _audioRoute.value = CallAudioRoute.EARPIECE
        _availableAudioDevices.value = emptyList()
    }

    // -------------------------------------------------------------------------
    // Public routing API (user actions)
    // -------------------------------------------------------------------------

    /** User explicitly picked a route. Remembered until a new headset appears. */
    fun setAudioRoute(route: CallAudioRoute): Boolean {
        userChoice = route

        val ok = switchTo(route)
        if (!ok) userChoice = null

        return ok
    }

    fun toggleSpeaker() {
        val target =
            if (_audioRoute.value == CallAudioRoute.SPEAKER) {
                CallAudioRoutePolicy.bestNonSpeaker(availableRoutes())
            } else {
                CallAudioRoute.SPEAKER
            }

        setAudioRoute(target)
    }

    // -------------------------------------------------------------------------
    // Automatic routing (the policy)
    // -------------------------------------------------------------------------

    private fun availableRoutes(): Set<CallAudioRoute> =
        _availableAudioDevices.value.map { it.route }.toSet()

    private fun applyBestRoute(force: Boolean = false) {
        val available = availableRoutes()

        val target = CallAudioRoutePolicy.pick(
            callType = callType,
            available = available,
            userChoice = userChoice
        )

        if (!force && target == _audioRoute.value) return

        if (switchTo(target)) return

        // Target failed (e.g. no earpiece on a tablet). Walk down the list.
        Log.w("CallAudio", "route $target failed, falling back")

        for (fallback in FALLBACK_ORDER) {
            if (fallback == target) continue
            if (fallback !in available) continue
            if (switchTo(fallback)) return
        }
    }

    // -------------------------------------------------------------------------
    // Low-level switching (no policy, no userChoice changes)
    // -------------------------------------------------------------------------

    private fun switchTo(route: CallAudioRoute): Boolean {
        val previous = _audioRoute.value

        // API < 31: leave Bluetooth SCO BEFORE moving to another output.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            previous == CallAudioRoute.BLUETOOTH &&
            route != CallAudioRoute.BLUETOOTH
        ) {
            stopLegacyBluetoothSco()
        }

        val success = when (route) {
            CallAudioRoute.SPEAKER -> setSpeakerRoute()
            CallAudioRoute.EARPIECE -> setEarpieceRoute()
            CallAudioRoute.BLUETOOTH -> setBluetoothRoute()
            CallAudioRoute.WIRED_HEADSET -> setWiredHeadsetRoute()
        }

        if (success) {
            _audioRoute.value = route
            _isSpeakerEnabled.value = route == CallAudioRoute.SPEAKER
        }

        return success
    }

    private fun setSpeakerRoute(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = findCommunicationDevice(
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            ) ?: return false

            return audioManager.setCommunicationDevice(device)
        }

        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = true

        return true
    }

    private fun setEarpieceRoute(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = findCommunicationDevice(
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            ) ?: return false

            return audioManager.setCommunicationDevice(device)
        }

        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false

        return true
    }

    private fun setBluetoothRoute(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.isBluetooth() }
                ?: return false

            return audioManager.setCommunicationDevice(device)
        }

        if (!hasLegacyBluetoothDevice()) return false

        startLegacyBluetoothSco()
        return true
    }

    private fun setWiredHeadsetRoute(): Boolean {
        if (!hasWiredHeadset()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.isWired() }
                ?: return false

            return audioManager.setCommunicationDevice(device)
        }

        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false

        return true
    }

    // -------------------------------------------------------------------------
    // Device discovery
    // -------------------------------------------------------------------------

    private fun AudioDeviceInfo.isBluetooth(): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                type == AudioDeviceInfo.TYPE_BLE_HEADSET

    private fun AudioDeviceInfo.isWired(): Boolean =
        type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET

    fun refreshAvailableRoutes() {
        val devices = mutableListOf<CallAudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices.forEach { device ->
                when {
                    device.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ->
                        devices.add(CallAudioDevice(CallAudioRoute.EARPIECE, "Earpiece"))

                    device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
                        devices.add(CallAudioDevice(CallAudioRoute.SPEAKER, "Speaker"))

                    device.isBluetooth() ->
                        devices.add(
                            CallAudioDevice(
                                CallAudioRoute.BLUETOOTH,
                                device.productName?.toString()?.takeIf { it.isNotBlank() }
                                    ?: "Bluetooth"
                            )
                        )

                    device.isWired() ->
                        devices.add(
                            CallAudioDevice(CallAudioRoute.WIRED_HEADSET, "Wired headset")
                        )
                }
            }
        } else {
            devices.add(CallAudioDevice(CallAudioRoute.EARPIECE, "Earpiece"))
            devices.add(CallAudioDevice(CallAudioRoute.SPEAKER, "Speaker"))

            if (hasLegacyBluetoothDevice()) {
                devices.add(CallAudioDevice(CallAudioRoute.BLUETOOTH, "Bluetooth"))
            }

            if (hasWiredHeadset()) {
                devices.add(CallAudioDevice(CallAudioRoute.WIRED_HEADSET, "Wired headset"))
            }
        }

        _availableAudioDevices.value = devices.distinctBy { it.route }
    }

    private fun findCommunicationDevice(type: Int): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        return audioManager.availableCommunicationDevices
            .firstOrNull { it.type == type }
    }

    private fun hasLegacyBluetoothDevice(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return false

        @Suppress("DEPRECATION")
        return audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
    }

    private fun hasWiredHeadset(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return audioManager.availableCommunicationDevices.any { it.isWired() }
        }

        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn
    }

    private fun startLegacyBluetoothSco() {
        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()

        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = true

        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
    }

    private fun stopLegacyBluetoothSco() {
        @Suppress("DEPRECATION")
        audioManager.stopBluetoothSco()

        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = false
    }

    // -------------------------------------------------------------------------
    // Device monitoring (headset plugged / unplugged mid-call)
    // -------------------------------------------------------------------------

    private val audioDeviceCallback = object : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshAvailableRoutes()

            // A headset connecting is an explicit "use me" signal: it beats an
            // earlier manual speaker/earpiece choice (same as WhatsApp).
            if (addedDevices.any { it.isBluetooth() || it.isWired() }) {
                userChoice = null
            }

            applyBestRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshAvailableRoutes()

            // If the active device vanished the policy falls back to the
            // default (voice -> earpiece, video -> speaker), or to the user's
            // choice if that is still available.
            applyBestRoute()
        }
    }

    fun startDeviceMonitoring() {
        if (isDeviceMonitoring) return

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        isDeviceMonitoring = true
        refreshAvailableRoutes()
    }

    fun stopDeviceMonitoring() {
        if (!isDeviceMonitoring) return

        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)

        isDeviceMonitoring = false
    }

    // -------------------------------------------------------------------------
    // Audio focus
    // -------------------------------------------------------------------------

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()

            audioManager.requestAudioFocus(request)
            focusRequest = request
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private companion object {
        val FALLBACK_ORDER = listOf(
            CallAudioRoute.BLUETOOTH,
            CallAudioRoute.WIRED_HEADSET,
            CallAudioRoute.EARPIECE,
            CallAudioRoute.SPEAKER
        )
    }
}