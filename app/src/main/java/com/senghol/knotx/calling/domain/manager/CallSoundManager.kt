package com.senghol.knotx.calling.domain.manager

import android.app.NotificationManager
import android.content.Context
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RawRes
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.model.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * All call sounds in one place.
 *
 * OUTGOING
 *  - other phone ringing  -> ringback loop
 *  - busy / declined      -> busy tone
 *  - failed               -> error tone
 *  - answered call ended  -> short end beep
 *
 * INCOMING
 *  - your custom ringtone (res/raw) in a loop + vibration, until the call is
 *    answered / declined / cancelled / missed
 *  - respects the phone's ringer mode (normal = sound + vibrate, vibrate =
 *    vibrate only, silent = nothing) and Do Not Disturb (total silence / alarms only)
 *  - pressing a volume button silences the ring (see silenceRinging())
 *
 * The incoming-call NOTIFICATION must be silent (no channel sound/vibration),
 * otherwise the user hears two rings.
 *
 * Tones use STREAM_VOICE_CALL, so they follow the call audio route chosen by
 * CallAudioManager.
 */
class CallSoundManager(
    private val context: Context,
    private val callManager: CallManager,
    private val playIncomingRingtone: Boolean = true,
    @RawRes private val incomingRingtoneRes: Int? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val ringAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // call tones
    private var tone: ToneGenerator? = null
    private var loopingToneType = NO_TONE

    @Volatile
    private var timedToneJob: Job? = null

    // incoming ring
    private var isRinging = false
    private var ringPlayer: MediaPlayer? = null
    private var systemRingtone: Ringtone? = null
    private var ringFocus: AudioFocusRequest? = null
    private var volumeObserver: ContentObserver? = null
    private var ringVolumeBaseline = 0
    private var ringerModeBaseline = AudioManager.RINGER_MODE_NORMAL
    private var silencedCallId: String? = null

    private var lastCallId: String? = null
    private var connectedBeepCallId: String? = null

    /** Call once, next to wherever you call callManager.start(). */
    fun start() {
        callManager.onCallEnded = { call -> onCallEnded(call) }

        scope.launch {
            callManager.activeCall.collect { onActiveCallChanged(it) }
        }
    }

    /**
     * Lets an end tone (busy / failed / hang-up beep) finish before the audio
     * route is torn down. Returns immediately if nothing is playing.
     */
    suspend fun awaitEndTone() {
        timedToneJob?.join()
    }

    /**
     * Stops the ring + vibration of the CURRENT incoming call, but not the call
     * itself (the call screen stays up). Called when the user presses a volume
     * button, like on a normal phone call.
     */
    fun silenceRinging() {
        if (!isRinging) return

        silencedCallId = lastCallId
        stopRinging()
    }

    // -------------------------------------------------------------------------
    // Reacting to call changes
    // -------------------------------------------------------------------------

    private fun onActiveCallChanged(call: CallSession?) {
        val live = call?.takeIf { it.state !in TERMINAL_STATES }

        if (live == null) {
            // Don't touch a running end tone; it was started by onCallEnded().
            stopLooping()
            stopRinging()
            return
        }

        val id = live.callId.value

        if (id != lastCallId) {
            // A new call supersedes any end tone still playing.
            stopTimed()
            lastCallId = id
        }

        when {
            live.direction == CallDirection.OUTGOING &&
                    live.state == CallState.RINGING -> {
                stopRinging()
                playLooping(ToneGenerator.TONE_SUP_RINGTONE)
            }

            live.direction == CallDirection.INCOMING &&
                    live.state == CallState.RINGING &&
                    playIncomingRingtone -> {
                stopLooping()

                if (silencedCallId != id) startRinging()
            }

            else -> {
                stopLooping()
                stopRinging()
            }
        }

        if (CONNECTED_BEEP &&
            live.state == CallState.CONNECTED &&
            connectedBeepCallId != id
        ) {
            connectedBeepCallId = id
            playTimed(ToneGenerator.TONE_PROP_ACK, 250)
        }
    }

    /**
     * Invoked synchronously by CallManager.setState() just BEFORE activeCall
     * becomes null. That ordering matters: the tone job exists by the time the
     * teardown code calls awaitEndTone().
     */
    private fun onCallEnded(call: CallSession) {
        stopLooping()
        stopRinging()
        lastCallId = null

        when (call.state) {
            CallState.BUSY ->
                playTimed(ToneGenerator.TONE_SUP_BUSY, 2_500)

            CallState.REJECTED ->
                if (call.direction == CallDirection.OUTGOING) {
                    playTimed(ToneGenerator.TONE_SUP_BUSY, 2_000)
                }

            CallState.FAILED ->
                playTimed(ToneGenerator.TONE_SUP_CONGESTION, 2_000)

            CallState.ENDED ->
                if (call.answeredAt != null) {
                    playTimed(ToneGenerator.TONE_PROP_PROMPT, 400)
                }

            else -> Unit // cancelled / missed: silence is correct
        }
    }

    // -------------------------------------------------------------------------
    // Call tones (ringback / busy / failed / end)
    // -------------------------------------------------------------------------

    private fun createTone(): ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_VOLUME)
    } catch (e: RuntimeException) {
        Log.e("CallSound", "ToneGenerator unavailable", e)
        null
    }

    @Synchronized
    private fun playLooping(toneType: Int) {
        if (loopingToneType == toneType && tone != null) return


        stopToneLocked()

        val generator = createTone() ?: return
        generator.startTone(toneType)

        tone = generator
        loopingToneType = toneType
    }

    @Synchronized
    private fun playTimed(toneType: Int, durationMs: Long) {

        stopToneLocked()

        val generator = createTone() ?: return
        generator.startTone(toneType, durationMs.toInt())

        tone = generator

        timedToneJob = scope.launch {
            delay(durationMs + 150)

            synchronized(this@CallSoundManager) {
                if (tone === generator) stopToneLocked()
            }
        }
    }

    @Synchronized
    private fun stopLooping() {
        if (loopingToneType != NO_TONE) stopToneLocked()
    }

    @Synchronized
    private fun stopTimed() {
        if (timedToneJob != null) stopToneLocked()
    }

    private fun stopToneLocked() {
        timedToneJob?.cancel()
        timedToneJob = null
        loopingToneType = NO_TONE

        tone?.let {
            runCatching { it.stopTone() }
            it.release()
        }
        tone = null
    }

    // -------------------------------------------------------------------------
    // Incoming ring: ringtone + vibration
    // -------------------------------------------------------------------------

    @Synchronized
    private fun startRinging() {
        if (isRinging) return

        if (isDoNotDisturbBlocking()) {
            return
        }

        val mode = audioManager.ringerMode

        if (mode == AudioManager.RINGER_MODE_SILENT) {
            return
        }


        isRinging = true

        requestRingFocus()

        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            startRingtonePlayback()
        }

        // normal AND vibrate mode vibrate
        startVibration()

        watchForVolumeButton()
    }

    @Synchronized
    private fun stopRinging() {
        if (!isRinging &&
            ringPlayer == null &&
            systemRingtone == null &&
            volumeObserver == null
        ) {
            return
        }


        isRinging = false

        ringPlayer?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        ringPlayer = null

        systemRingtone?.let { runCatching { it.stop() } }
        systemRingtone = null

        runCatching { vibrator?.cancel() }

        volumeObserver?.let {
            runCatching { context.contentResolver.unregisterContentObserver(it) }
        }
        volumeObserver = null

        abandonRingFocus()
    }

    private fun isDoNotDisturbBlocking(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return false

        return when (manager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> true

            else -> false
        }
    }

    private fun startRingtonePlayback() {
        val resId = incomingRingtoneRes

        if (resId != null) {
            val player = MediaPlayer()

            try {
                player.setAudioAttributes(ringAttributes)

                context.resources.openRawResourceFd(resId).use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }

                player.isLooping = true
                player.setOnPreparedListener { it.start() }
                player.setOnErrorListener { _, what, extra ->
                    Log.e("CallSound", "ringtone player error what=$what extra=$extra")
                    false
                }
                player.prepareAsync()

                ringPlayer = player
                return
            } catch (e: Exception) {
                Log.e("CallSound", "custom ringtone failed, using system ringtone", e)
                runCatching { player.release() }
            }
        }

        startSystemRingtone()
    }

    private fun startSystemRingtone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE
        ) ?: Settings.System.DEFAULT_RINGTONE_URI

        systemRingtone = RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = ringAttributes

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }

            play()
        }
    }

    private fun startVibration() {
        val v = vibrator ?: return

        if (!v.hasVibrator()) return

        val pattern = longArrayOf(0, 900, 900)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // The AudioAttributes (ringtone usage) overload is what lets a
                // call vibrate while the app is in the background.
                @Suppress("DEPRECATION")
                v.vibrate(VibrationEffect.createWaveform(pattern, 0), ringAttributes)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("CallSound", "vibration failed", e)
        }
    }

    /**
     * A volume press while the app is in the background (heads-up notification,
     * not the full call screen) is handled by the system, so we watch for the
     * ring volume / ringer mode changing. On the full call screen CallActivity
     * also forwards the key press to silenceRinging().
     */
    private fun watchForVolumeButton() {
        ringVolumeBaseline = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        ringerModeBaseline = audioManager.ringerMode

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val volumeChanged =
                    audioManager.getStreamVolume(AudioManager.STREAM_RING) != ringVolumeBaseline
                val modeChanged = audioManager.ringerMode != ringerModeBaseline

                if (volumeChanged || modeChanged) silenceRinging()
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            observer
        )

        volumeObserver = observer
    }

    // Pause other audio (music etc.) while the phone rings.
    private fun requestRingFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(ringAttributes)
                .setOnAudioFocusChangeListener { }
                .build()

            audioManager.requestAudioFocus(request)
            ringFocus = request
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonRingFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ringFocus?.let { audioManager.abandonAudioFocusRequest(it) }
            ringFocus = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private companion object {
        const val NO_TONE = -1
        const val TONE_VOLUME = 80
        const val CONNECTED_BEEP = false // flip to true for a beep when the call connects

        val TERMINAL_STATES = setOf(
            CallState.ENDED,
            CallState.REJECTED,
            CallState.CANCELLED,
            CallState.BUSY,
            CallState.MISSED,
            CallState.FAILED
        )
    }
}