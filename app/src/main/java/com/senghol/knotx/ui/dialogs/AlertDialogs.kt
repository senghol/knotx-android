package com.senghol.knotx.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.senghol.knotx.R
object AppDialogs {

    @Composable
    fun PaymentSuccessDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,

            title = {
                Text(
                    text = "🎉 Payment Successful!",
                    style = MaterialTheme.typography.titleLarge
                )
            },

            text = {
                Column {
                    Text(
                        text = "Thank you for your support ❤️",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your contribution helps us keep improving 🚀",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Awesome 👍")
                }
            }
        )
    }
    @Composable
    fun DeleteProfileDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,

            title = {
                Text(
                    text = "⚠️ Delete Profile?",
                    style = MaterialTheme.typography.titleLarge
                )
            },

            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete your profile?",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This action is permanent and cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete ❌")
                }
            },

            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}