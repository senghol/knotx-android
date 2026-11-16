package com.senghol.knotx.calling.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.senghol.knotx.calling.domain.model.CallType

class CallPermissionManager(private val context: Context) {

    fun getMissingPermissions(callType: CallType): List<String> {
        val permissions = when (callType) {
            CallType.VOICE -> listOf(
                Manifest.permission.RECORD_AUDIO
            )
            CallType.VIDEO -> listOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        }

        return permissions.filter {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPermissions(callType: CallType): Boolean {
        return getMissingPermissions(callType).isEmpty()
    }
}