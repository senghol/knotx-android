package com.senghol.knotx.ui.dialogs
// Core Compose
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.senghol.knotx.R

@Composable
fun ContributeDialog(
    onDismiss: () -> Unit,
    onContribute: (Int) -> Unit
) {
    val presetAmounts = listOf(10, 20, 50, 100, 200)

    var selectedAmount by remember { mutableStateOf(20) }
    var isCustom by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val invalidAmountError = stringResource(R.string.contribute_error_invalid)
    val zeroAmountError = stringResource(R.string.contribute_error_zero)
    val maxAmountError = stringResource(R.string.contribute_error_max)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.contribute_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.contribute_subtitle),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetAmounts.forEach { amount ->
                        FilterChip(
                            selected = !isCustom && selectedAmount == amount,
                            onClick = {
                                selectedAmount = amount
                                isCustom = false
                                error = null
                            },
                            label = {
                                Text(
                                    text = "₹$amount",
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    FilterChip(
                        selected = isCustom,
                        onClick = {
                            isCustom = true
                            error = null
                        },
                        label = {
                            Text(stringResource(R.string.contribute_custom))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (isCustom) {
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it.filter { ch -> ch.isDigit() }.take(5)
                            error = null
                        },
                        label = {
                            Text(stringResource(R.string.contribute_enter_amount))
                        },
                        prefix = { Text("₹") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = if (isCustom) {
                        customInput.toIntOrNull()
                    } else {
                        selectedAmount
                    }

                    when {
                        amount == null -> error = invalidAmountError

                        amount <= 0 -> error = zeroAmountError

                        amount > 10000 -> error = maxAmountError

                        else -> {
                            error = null
                            onContribute(amount * 100)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.contribute_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
