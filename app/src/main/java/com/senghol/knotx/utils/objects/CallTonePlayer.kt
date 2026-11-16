package com.senghol.knotx.utils.objects

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.senghol.knotx.R

object OutgoingTone {

    private enum class State {
        IDLE,
        CONNECTING,
        RINGING
    }

    private var state = State.IDLE

    private var sp: SoundPool? = null
    private var streamId: Int = 0

    private var connectingId: Int = 0
    private var ringingId: Int = 0

//    fun startConnecting(context: Context) {
//        if (state != State.IDLE) return // 🔥 only first time
//
//        ensurePool()
//
//        state = State.CONNECTING
//
//        connectingId = sp!!.load(context, R.raw.call_connecting, 1)
//
//        sp!!.setOnLoadCompleteListener { _, id, status ->
//            if (status == 0 && id == connectingId && state == State.CONNECTING) {
//                streamId = sp!!.play(id, 0.3f, 0.3f, 0, -1, 1f)
//            }
//        }
//    }

    fun switchToRinging(context: Context) {
        if (state == State.RINGING) return

        ensurePool()

        // 🔥 stop connecting tone
        sp?.stop(streamId)

        state = State.RINGING

//        ringingId = sp!!.load(context, R.raw.call_ringing, 1)

        sp!!.setOnLoadCompleteListener { _, id, status ->
            if (status == 0 && id == ringingId && state == State.RINGING) {
                streamId = sp!!.play(id, 1f, 1f, 0, -1, 1f)
            }
        }
    }

    fun stop() {
        sp?.stop(streamId)
        sp?.release()
        sp = null
        state = State.IDLE
    }

    private fun ensurePool() {
        if (sp != null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        sp = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()
    }
}
object EndTone {
    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var isLoaded = false

    fun init(context: Context) {
        if (soundPool != null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.setOnLoadCompleteListener { _, id, status ->
            if (status == 0 && id == soundId) isLoaded = true
        }

//        soundId = soundPool!!.load(context, R.raw.call_disconnect, 1)
    }

    fun play() {
        if (!isLoaded) return
        soundPool?.play(soundId, 0.9f, 0.9f, 0, 0, 1f)
    }
}