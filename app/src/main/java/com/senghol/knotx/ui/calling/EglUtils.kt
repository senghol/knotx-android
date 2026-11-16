package com.senghol.knotx.ui.calling


import org.webrtc.EglBase

object EglUtils {
    val eglBase: EglBase by lazy {
        EglBase.create()
    }

    val eglBaseContext: EglBase.Context
        get() = eglBase.eglBaseContext
}
