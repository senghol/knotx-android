package com.senghol.knotx.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PasscodeScreen(
    onPasscodeEntered: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val dotScales = List(4) { Animatable(1f) }
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Enter 4-Digit Passcode",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Passcode dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .scale(dotScales[index].value)
                            .clip(CircleShape)
                            .background(
                                color = if (index < input.length)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            }

            // Error message
            if (error) {
                Text(
                    text = "Please enter a valid 4-digit passcode",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Keypad Layout
            if (isLandscape) {
                // Landscape → 4 buttons per row
// Keypad with 6 buttons per row
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        listOf("1", "2", "3", "4", "5", "⌫"),
                        listOf("6", "7", "8", "9", "0", "✔")
                    ).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    key = key,
                                    enabled = key.isNotEmpty(),
                                    modifier = Modifier.size(56.dp), // 🔥 fixed size
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        when (key) {
                                            "⌫" -> {
                                                if (input.isNotEmpty()) {
                                                    input = input.dropLast(1)
                                                    coroutineScope.launch {
                                                        dotScales[input.length].animateTo(1.3f, tween(100))
                                                        dotScales[input.length].animateTo(1f, tween(100))
                                                    }
                                                }
                                            }
                                            "✔" -> {
                                                if (input.length == 4) {
                                                    onPasscodeEntered(input)
                                                    error = false
                                                } else {
                                                    error = true
                                                }
                                            }
                                            else -> {
                                                if (input.length < 4) {
                                                    input += key
                                                    coroutineScope.launch {
                                                        dotScales[input.length - 1].animateTo(1.3f, tween(100))
                                                        dotScales[input.length - 1].animateTo(1f, tween(100))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait → Standard keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("⌫", "0", "✔")
                    ).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), // center + spacing
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    key = key,
                                    enabled = key.isNotEmpty(),
                                    modifier = Modifier.size(72.dp), // fixed size
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        when (key) {
                                            "⌫" -> {
                                                if (input.isNotEmpty()) {
                                                    input = input.dropLast(1)
                                                    coroutineScope.launch {
                                                        dotScales[input.length].animateTo(1.3f, tween(100))
                                                        dotScales[input.length].animateTo(1f, tween(100))
                                                    }
                                                }
                                            }
                                            "✔" -> {
                                                if (input.length == 4) {
                                                    onPasscodeEntered(input)
                                                    error = false
                                                } else {
                                                    error = true
                                                }
                                            }
                                            else -> {
                                                if (input.length < 4) {
                                                    input += key
                                                    coroutineScope.launch {
                                                        dotScales[input.length - 1].animateTo(1.3f, tween(100))
                                                        dotScales[input.length - 1].animateTo(1f, tween(100))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    key: String,
    enabled: Boolean,
    modifier: Modifier = Modifier, // 🔥 allow parent to control size/weight
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // keep square shape
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { if (enabled) onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (key.isNotEmpty()) {
            Text(
                text = key,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun handleKeyPress(
    key: String,
    onPasscodeEntered: (String) -> Unit,
    coroutineScope: CoroutineScope,
    dotScales: List<Animatable<Float, AnimationVector1D>>,
    input: String,
    setInput: (String) -> Unit,
    setError: (Boolean) -> Unit
) {
    when (key) {
        "⌫" -> {
            if (input.isNotEmpty()) {
                setInput(input.dropLast(1))
                coroutineScope.launch {
                    dotScales[input.length - 1].animateTo(1.3f, tween(100))
                    dotScales[input.length - 1].animateTo(1f, tween(100))
                }
            }
        }
        "✔" -> {
            if (input.length == 4) {
                onPasscodeEntered(input)
                setError(false)
            } else {
                setError(true)
            }
        }
        else -> {
            if (input.length < 4) {
                val newInput = input + key
                setInput(newInput)
                coroutineScope.launch {
                    dotScales[newInput.length - 1].animateTo(1.3f, tween(100))
                    dotScales[newInput.length - 1].animateTo(1f, tween(100))
                }
            }
        }
    }
}
