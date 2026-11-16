package com.senghol.knotx.ui.dialogs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun OptionalUpdateDialog(
    latestVersion: String,
    currentVersion: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Update Available") },
        text = {
            Text("A new version ($latestVersion) is available. You're using $currentVersion. Would you like to update?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Later")
            }
        }
    )
}
