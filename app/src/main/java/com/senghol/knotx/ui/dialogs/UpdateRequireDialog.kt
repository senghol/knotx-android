package com.senghol.knotx.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun UpdateRequiredDialog(
    minimumVersion: String,
    currentVersion: String,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null // Optional: close app or stay blocked
) {
    AlertDialog(
        onDismissRequest = { /* Disable dismissing by tapping outside */ },
        title = {
            Text(
                text = "Update Required",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text("Your current app version ($currentVersion) is outdated.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Minimum required version is $minimumVersion.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please update the app to continue using it.")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Update Now")
            }
        },
        dismissButton = {
            onCancel?.let {
                TextButton(onClick = it) {
                    Text("Close")
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
