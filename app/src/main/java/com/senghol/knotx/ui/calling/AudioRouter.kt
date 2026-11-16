package com.senghol.knotx.ui.calling


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.annotation.RequiresApi
import com.senghol.knotx.utils.data.AudioDeviceDakiya
import com.senghol.knotx.utils.enums.AudioOutputDevice

class AudioRouter(private val context: Context) {

    companion object {
        private const val TAG = "AudioRouter"
    }

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Callback so ViewModel can refresh UI when devices change
    var onDeviceListChanged: (() -> Unit)? = null

    /* ────────────────────────────────────────────────
     * INIT
     * ──────────────────────────────────────────────── */

    fun init(isVideoCall: Boolean) {

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false

        if (isVideoCall) routeToSpeaker() else routeToEarpiece()

        registerDeviceCallbacks()
    }

    /* ────────────────────────────────────────────────
     * UNIFIED SETTER — called by ViewModel
     * ──────────────────────────────────────────────── */

    fun setOutputDevice(type: AudioOutputDevice): Boolean {
        return when (type) {
            AudioOutputDevice.SPEAKER      -> routeToSpeaker()
            AudioOutputDevice.EARPIECE     -> routeToEarpiece()
            AudioOutputDevice.BLUETOOTH    -> routeToBluetooth()
            AudioOutputDevice.WIRED_HEADSET -> routeToWiredHeadset()
            else -> false
        }
    }

    /* ────────────────────────────────────────────────
     * DEVICE LIST (for UI)
     * ──────────────────────────────────────────────── */

    fun getAvailableDevices(): List<AudioDeviceDakiya> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                audioManager.availableCommunicationDevices.map {
                    AudioDeviceDakiya(
                        id = it.id,
                        type = mapType(it.type),
                        name = it.productName?.toString() ?: "Unknown"
                    )
                }
            }
            else -> buildLegacyDeviceList()
        }
    }

    fun getCurrentDevice(): AudioOutputDevice {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                mapType(audioManager.communicationDevice?.type)
            }
            else -> {
                @Suppress("DEPRECATION")
                when {
                    audioManager.isBluetoothScoOn -> AudioOutputDevice.BLUETOOTH
                    isWiredHeadsetConnectedLegacy() -> AudioOutputDevice.WIRED_HEADSET
                    audioManager.isSpeakerphoneOn -> AudioOutputDevice.SPEAKER
                    else -> AudioOutputDevice.EARPIECE
                }
            }
        }
    }

    /* ────────────────────────────────────────────────
     * ROUTING
     * ──────────────────────────────────────────────── */

    fun routeToSpeaker(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val device = audioManager.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                device?.let { audioManager.setCommunicationDevice(it) } ?: false
            }
            else -> {
                stopBluetoothScoIfActive()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
                true
            }
        }
    }

    fun routeToEarpiece(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val device = audioManager.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                device?.let { audioManager.setCommunicationDevice(it) } ?: false
            }
            else -> {
                stopBluetoothScoIfActive()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                true
            }
        }
    }

    fun routeToBluetooth(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val device = audioManager.availableCommunicationDevices
                    .firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    }
                device?.let { audioManager.setCommunicationDevice(it) } ?: false
            }
            else -> {
                if (!audioManager.isBluetoothScoAvailableOffCall) {
                    Log.w(TAG, "Bluetooth SCO not available")
                    return false
                }
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                // Register receiver to confirm SCO connected before returning success
                registerScoReceiver()
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = true
                true
            }
        }
    }

    private fun routeToWiredHeadset(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val device = audioManager.availableCommunicationDevices
                    .firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    }
                device?.let { audioManager.setCommunicationDevice(it) } ?: false
            }
            else -> {
                // Wired headset is auto-routed by Android on legacy — just turn off speaker
                stopBluetoothScoIfActive()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                true
            }
        }
    }

    fun routeToDevice(deviceId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // On legacy, map by id from buildLegacyDeviceList and call the right route fn
            return when (deviceId) {
                1 -> routeToEarpiece()
                2 -> routeToSpeaker()
                3 -> routeToBluetooth()
                4 -> routeToWiredHeadset()
                else -> false
            }
        }
        val device = audioManager.availableCommunicationDevices
            .firstOrNull { it.id == deviceId }
        return device?.let { audioManager.setCommunicationDevice(it) } ?: false
    }

    /* ────────────────────────────────────────────────
     * MICROPHONE
     * ──────────────────────────────────────────────── */

    fun muteMic(mute: Boolean) {
        audioManager.isMicrophoneMute = mute
    }

    /* ────────────────────────────────────────────────
     * CLEANUP
     * ──────────────────────────────────────────────── */

    fun release() {

        unregisterScoReceiver()
        unregisterDeviceCallbacks()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                audioManager.clearCommunicationDevice()
            }
            else -> {
                stopBluetoothScoIfActive()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
            }
        }

        audioManager.isMicrophoneMute = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    /* ────────────────────────────────────────────────
     * DEVICE CHANGE CALLBACKS
     * ──────────────────────────────────────────────── */

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
            onDeviceListChanged?.invoke()
        }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
            onDeviceListChanged?.invoke()
        }
    }

    private fun registerDeviceCallbacks() {
        audioManager.registerAudioDeviceCallback(
            deviceCallback,
            Handler(Looper.getMainLooper())
        )
    }

    private fun unregisterDeviceCallbacks() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    /* ────────────────────────────────────────────────
     * BLUETOOTH SCO RECEIVER (legacy only)
     * ──────────────────────────────────────────────── */

    private var scoReceiver: BroadcastReceiver? = null

    private fun registerScoReceiver() {
        if (scoReceiver != null) return
        scoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(
                    AudioManager.EXTRA_SCO_AUDIO_STATE, -1
                )
                when (state) {
                    AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                        onDeviceListChanged?.invoke()
                    }
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                        onDeviceListChanged?.invoke()
                    }
                }
            }
        }
        @Suppress("DEPRECATION")
        context.registerReceiver(
            scoReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )
    }

    private fun unregisterScoReceiver() {
        scoReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        scoReceiver = null
    }

    /* ────────────────────────────────────────────────
     * HELPERS
     * ──────────────────────────────────────────────── */

    private fun stopBluetoothScoIfActive() {
        @Suppress("DEPRECATION")
        if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }
    }

    @Suppress("DEPRECATION")
    private fun isWiredHeadsetConnectedLegacy(): Boolean {
        return audioManager.isWiredHeadsetOn
    }

    private fun mapType(type: Int?): AudioOutputDevice {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER   -> AudioOutputDevice.SPEAKER
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE  -> AudioOutputDevice.EARPIECE
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP    -> AudioOutputDevice.BLUETOOTH
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES  -> AudioOutputDevice.WIRED_HEADSET
            else                                   -> AudioOutputDevice.UNKNOWN
        }
    }

    private fun buildLegacyDeviceList(): List<AudioDeviceDakiya> {
        val list = mutableListOf<AudioDeviceDakiya>()

        // Always present
        list.add(AudioDeviceDakiya(id = 1, type = AudioOutputDevice.EARPIECE, name = "Earpiece"))
        list.add(AudioDeviceDakiya(id = 2, type = AudioOutputDevice.SPEAKER,  name = "Speaker"))

        // Only add bluetooth if actually connected
        @Suppress("DEPRECATION")
        if (audioManager.isBluetoothScoAvailableOffCall || audioManager.isBluetoothA2dpOn) {
            list.add(AudioDeviceDakiya(id = 3, type = AudioOutputDevice.BLUETOOTH, name = "Bluetooth"))
        }

        // Only add wired headset if actually plugged in
        @Suppress("DEPRECATION")
        if (audioManager.isWiredHeadsetOn) {
            list.add(AudioDeviceDakiya(id = 4, type = AudioOutputDevice.WIRED_HEADSET, name = "Wired Headset"))
        }

        return list
    }
}