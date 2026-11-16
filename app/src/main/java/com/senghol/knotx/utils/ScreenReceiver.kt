package com.senghol.knotx.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class ScreenReceiver(
    private val onScreenOff: () -> Unit,
    private val onScreenOn: () -> Unit,
    private val onUserPresent: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
            Intent.ACTION_SCREEN_ON -> onScreenOn()
            Intent.ACTION_USER_PRESENT -> onUserPresent()
        }
    }
}
