package com.senghol.knotx.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

@Composable
fun KeyboardAware(content: @Composable () -> Unit) {
    val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current) // 🔥 Get keyboard height
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0) {
            coroutineScope.launch {
                scrollState.scrollBy(keyboardHeight.toFloat()) // 🔥 Scroll content by keyboard height
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // 🔥 Allow scrolling when needed
            .imePadding() // 🔥 Moves content above the keyboard
    ) {
        content()
    }
}
