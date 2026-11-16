package com.senghol.knotx.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToRevealListActions(
    onDelete: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 72.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxWidth()) {
        // Forward — revealed on the LEFT when the row is dragged right (positive offset).
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        onForward()
                        scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy)) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "Forward", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // Delete — revealed on the RIGHT when the row is dragged left (negative offset).
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable {
                        onDelete()
                        scope.launch { offsetX.snapTo(0f) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = when {
                                offsetX.value < -actionWidthPx / 2 -> -actionWidthPx
                                offsetX.value > actionWidthPx / 2 -> actionWidthPx
                                else -> 0f
                            }
                            scope.launch { offsetX.animateTo(target, spring(dampingRatio = Spring.DampingRatioNoBouncy)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newVal = (offsetX.value + dragAmount).coerceIn(-actionWidthPx, actionWidthPx)
                            scope.launch { offsetX.snapTo(newVal) }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
