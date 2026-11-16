package com.senghol.knotx.ui.components


import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
fun PermissionHandler(
    onAllPermissionsGranted: () -> Unit,
    onPermissionsDenied: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val permissions = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)

    }

    // State to track dialog visibility
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var deniedPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Launcher for permission requests
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allGranted = permissionsResult.all { it.value }
        val denied = permissionsResult.filter { !it.value }.keys.toList()
        if (allGranted) {
            onAllPermissionsGranted()
        } else {
            deniedPermissions = denied
            // Check if we should show rationale or permanently denied dialog
            val shouldShowRationale = denied.any { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PermissionChecker.PERMISSION_GRANTED &&
                        shouldShowRequestPermissionRationale(context, permission)
            }
            val permanentlyDenied = denied.any { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PermissionChecker.PERMISSION_GRANTED &&
                        !shouldShowRequestPermissionRationale(context, permission)
            }

            if (shouldShowRationale) {
                showRationaleDialog = true
            } else if (permanentlyDenied) {
                showPermanentlyDeniedDialog = true
            }
            onPermissionsDenied(denied)
        }
    }

    // Check permission state on composition
    LaunchedEffect(Unit) {
        val allPermissionsGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            onAllPermissionsGranted()
        } else {
            // Optionally show a rationale before requesting
            val shouldShowRationale = permissions.any { permission ->
                shouldShowRequestPermissionRationale(context, permission)
            }
            if (shouldShowRationale) {
                showRationaleDialog = true
            } else {
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    // Rationale Dialog
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    permissionLauncher.launch(permissions.toTypedArray())
                }) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Permissions Required") },
            text = {
                Text(
                    "This app requires the following permissions for proper functionality:\n" +
                            deniedPermissions.joinToString("\n") { "- ${getReadablePermissionName(it)}" }
                )
            }
        )
    }

    // Permanently Denied Dialog
    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showPermanentlyDeniedDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentlyDeniedDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Permissions Permanently Denied") },
            text = {
                Text(
                    "You've permanently denied the following permissions. Please enable them in settings:\n" +
                            deniedPermissions.joinToString("\n") { "- ${getReadablePermissionName(it)}" }
                )
            }
        )
    }
}

private fun shouldShowRequestPermissionRationale(context: android.content.Context, permission: String): Boolean {
    val activity = (context as? androidx.activity.ComponentActivity) ?: return false
    return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

// Helper to map permissions to user-friendly names
fun getReadablePermissionName(permission: String): String {
    return when (permission) {
        Manifest.permission.RECORD_AUDIO -> "Audio"
        Manifest.permission.CAMERA -> "Camera"
        else -> permission.split(".").last()
    }
}