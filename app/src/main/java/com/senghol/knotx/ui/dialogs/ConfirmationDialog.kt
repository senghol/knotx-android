package com.senghol.knotx.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.senghol.knotx.R

@Composable
fun ConfirmationDialog(
    message: String,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {

    var deleteBoth by remember { mutableStateOf(false) }
    var showWarning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            deleteBoth = false
            showWarning = false
            onDismiss()
        },
        title = {
            Text(
                stringResource(R.string.confirmation),
                color = Color.White
            )
        },
        text = {
            Column {

                Text(message, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked = deleteBoth,
                        onCheckedChange = { deleteBoth = it }
                    )

                    Text(
                        stringResource(R.string.delete_both_sides),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // ⚠ Warning Icon
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Warning",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                showWarning = !showWarning
                            }
                    )
                }

                // 🔥 Warning Text (shown when icon clicked)
                if (showWarning) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Delete for everyone is only available for messages you have sent.",
                        color = Color(0xFFFFA000),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(deleteBoth)
            }) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        },
        containerColor = Color(0xFF202124)
    )
}
@Composable
fun ConfirmationDialogA(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.confirm_action),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    )
}

