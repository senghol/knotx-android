package com.senghol.knotx.ui.calling

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

import androidx.annotation.RequiresApi
class AudioRouteManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Call this on call start
    fun initAudio(isVideoCall: Boolean = true) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false
        updateRoutingForCall(isVideoCall)
    }

    // ✅ Safe wrapper: Fetch available communication devices (only on API 31+)
    fun getAvailableDevices(): List<AudioDeviceInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices.toList()
        } else {
            emptyList()
        }
    }

    // ✅ Safe wrapper: Get current communication device (only on API 31+)
    fun getCurrentDevice(): AudioDeviceInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice
        } else null
    }

    // ✅ Unified request: Accepts both API 31+ device type and fallback
    fun requestDevice(deviceType: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.type == deviceType }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
            } else false
        } else {
            when (deviceType) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                    true
                }
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    true
                }
                else -> false // No support for Bluetooth/Wired headset on pre-API 31
            }
        }
    }

    // ✅ Better version to request by full device (API 31+)
    fun requestDevice(device: AudioDeviceInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.setCommunicationDevice(device)
        } else false
    }

    fun muteMicrophone(mute: Boolean) {
        audioManager.isMicrophoneMute = mute
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isMicrophoneMute = false
    }

    // 📲 Automatically routes based on type of call
    private fun updateRoutingForCall(isVideoCall: Boolean) {
        if (isVideoCall) {
            requestDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        } else {
            requestDevice(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        }
    }
}
