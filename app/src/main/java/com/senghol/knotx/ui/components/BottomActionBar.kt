package com.senghol.knotx.ui.components



import androidx.compose.animation.animateColorAsState

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.model.LatLng
import com.senghol.knotx.ui.screens.SavedMedia
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import kotlinx.coroutines.delay

import androidx.compose.ui.unit.IntSize
import com.senghol.knotx.ui.theme.DootawasColors
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BottomActionBar(
    isConnected: Boolean,
    sosContacts: List<SosContact>,
    actionType: Int, // 0 = blank, 1 = group add, 2 = story, 3 = list
    onCreateGroup: () -> Unit,
    onCreateList: () -> Unit,
    sosTrigger: () -> Unit,
    onLinkDevice: () -> Unit,
    onStories: () -> Unit,
    onMenuClick: () -> Unit,
    onContactsClick: () -> Unit,
    dootawasColors: DootawasColors
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 16.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ? Dynamic Action Button based on actionType (0 = hidden, 1 = Group, 2 = Story, 3 = List)
            when (actionType) {
                0 -> {
                    AttentionButton(
                        onClick = onContactsClick,
                        onLongClick = { sosTrigger() },
                        icon = Icons.Default.PersonAddAlt1,
                        contentDescription = "Contacts. Hold to send SOS location.",
                        iconTint = dootawasColors.buttonText,
                        iconColor = dootawasColors.buttonBackground,
                    )
                }
                1 -> {
                    AttentionButton(
                        onClick = onCreateGroup,
                        icon = Icons.Default.GroupAdd,
                        contentDescription = "Create Group",
                        iconTint = dootawasColors.buttonText,
                        iconColor = dootawasColors.buttonBackground,
                    )
                }
                2 -> {
                    AttentionButton(
                        onClick = onStories,
                        icon = Icons.Default.DonutLarge,
                        contentDescription = "Stories",
                        iconTint = dootawasColors.buttonText,
                        iconColor = dootawasColors.buttonBackground,
                    )
                }
                4 -> {
                    AttentionButton(
                        onClick = onCreateList,
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Create List",
                        iconTint = dootawasColors.buttonText,
                        iconColor = dootawasColors.buttonBackground,
                    )
                }
                else -> {
                    // 0 or any other value: Blank / No icon shown
                }
            }

//            MenuStatusButton(
//                onClick = onMenuClick,
//                isConnected = isConnected,
//                buttonColor = dootawasColors.buttonBackground,
//                lineColor = dootawasColors.buttonText
//            )

            // ? Contacts Button — tap opens Contacts, hold triggers SOS quick-share

        }
    }
}
@Composable
fun MenuStatusButton(
    onClick: () -> Unit,
    isConnected: Boolean,
    buttonColor: Color,
    lineColor: Color,
    contentDescription: String = "Menu"
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "scale"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isConnected)
            Color(0xFF4CAF50)
        else
            Color(0xFFF44336),
        label = "statusColor"
    )

    val brush = rememberShineBrush(buttonColor)

    val isDark = isSystemInDarkTheme()

    val topHighlight = if (!isDark)
        Color.White.copy(alpha = 0.15f)
    else
        Color.White.copy(alpha = 0.35f)

    val midHighlight = if (!isDark)
        Color.White.copy(alpha = 0.05f)
    else
        Color.White.copy(alpha = 0.08f)


    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(54.dp)
            .bottomShadow(
                color = Color.Black.copy(alpha = 0.30f),
                cornerRadius = 12.dp,
                blurRadius = 10.dp,
                offsetY = 5.dp
            )
            .clip(shape)
            .background(brush)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        topHighlight,
                        midHighlight,
                        Color.Transparent
                    )
                ),
                shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(22.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            repeat(2) {
                Box(
                    Modifier
                        .width(24.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(lineColor)
                )
            }


        }
    }
}

fun Modifier.bottomShadow(
    color: Color = Color.Black.copy(alpha = 0.50f),
    cornerRadius: Dp = 10.dp,
    blurRadius: Dp = 10.dp,
    offsetY: Dp = 5.dp
): Modifier = composed {
    val shadowPaint = remember {
        android.graphics.Paint().apply {
            this.color = android.graphics.Color.TRANSPARENT
            isAntiAlias = true
        }
    }

    this.drawBehind {
        shadowPaint.setShadowLayer(
            blurRadius.toPx(),
            0f,
            offsetY.toPx(),
            color.toArgb()
        )
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadius.toPx(), cornerRadius.toPx(),
                shadowPaint
            )
        }
    }
}


@Composable
fun AttentionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector,
    contentDescription: String? = null,
    iconTint: Color,
    iconColor: Color,
) {
    val shape = RoundedCornerShape(16.dp) // Softer, more modern corner radius
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Tactile spring physics for a high-end bounce feel on press
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "attention_button_scale"
    )

    val brush = rememberShineBrush(iconColor)
    val isDark = isSystemInDarkTheme()

    // Dynamic highlights adjusted for better contrast in light/dark modes
    val topHighlight = if (isDark) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.8f)
    val bottomHighlight = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(56.dp) // Slightly scaled for better touch target ergonomics
            .bottomShadow(
                color = Color.Black.copy(alpha = if (isDark) 0.5f else 0.2f),
                cornerRadius = 16.dp,
                blurRadius = 12.dp,
                offsetY = 6.dp
            )
            .clip(shape)
            .background(brush)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        topHighlight,
                        Color.Transparent,
                        bottomHighlight
                    )
                ),
                shape = shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = iconTint),
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = contentDescription
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(28.dp) // Cleanly proportioned icon size
        )
    }
}

@Composable
fun rememberShineBrush(
    baseColor: Color = Color(0xFF051423)
): Brush {
    val sweepOffset = remember { Animatable(-1f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRunning by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isRunning = true
                Lifecycle.Event.ON_PAUSE -> isRunning = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            sweepOffset.stop()
            return@LaunchedEffect
        }
        while (isRunning) {
            val randomDelay = (20_000L..90_000L).random()
            delay(randomDelay.milliseconds)
            sweepOffset.animateTo(2f, tween(1500, easing = LinearEasing))
            delay(randomDelay.milliseconds)
            sweepOffset.animateTo(-1f, tween(1500, easing = LinearEasing))
        }
    }

    val baseSize = 120f
    val gradientHalfWidth = baseSize / 2f
    val shineCenter = baseSize * sweepOffset.value

    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            Color.White.copy(alpha = 0.35f),
            baseColor
        ),
        start = Offset(x = shineCenter - gradientHalfWidth, y = shineCenter - gradientHalfWidth),
        end = Offset(x = shineCenter + gradientHalfWidth, y = shineCenter + gradientHalfWidth)
    )
}

@Composable
fun rememberShineBrushSearch(
    baseColor: Color = Color(0xFFE8F1EC)
): Pair<Brush, Modifier> {
    val sweepOffset = remember { Animatable(-1f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRunning by remember { mutableStateOf(false) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isRunning = true
                Lifecycle.Event.ON_PAUSE -> isRunning = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val diagonal = kotlin.math.sqrt(
        (componentSize.width * componentSize.width +
                componentSize.height * componentSize.height).toFloat()
    ).coerceAtLeast(1f)

    val sweepDurationMs = ((diagonal / 120f) * 1500f)
        .toInt()
        .coerceIn(1500, 6000)

    LaunchedEffect(isRunning, sweepDurationMs) {
        if (!isRunning) {
            sweepOffset.stop()
            return@LaunchedEffect
        }

        while (isRunning) {
            val randomDelay = (20_000L..90_000L).random()
            delay(randomDelay.milliseconds)
            sweepOffset.animateTo(
                2f,
                tween(sweepDurationMs, easing = LinearEasing)
            )
            delay(randomDelay.milliseconds)
            sweepOffset.animateTo(
                -1f,
                tween(sweepDurationMs, easing = LinearEasing)
            )
        }
    }

    val gradientHalfWidth = diagonal / 2f
    val shineCenter = diagonal * sweepOffset.value

    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to baseColor,
            0.45f to Color.White.copy(alpha = 0.65f),
            1.0f to baseColor.copy(alpha = 0.85f)
        ),
        start = Offset(
            x = shineCenter - gradientHalfWidth,
            y = shineCenter - gradientHalfWidth
        ),
        end = Offset(
            x = shineCenter + gradientHalfWidth,
            y = shineCenter + gradientHalfWidth
        )
    )

    val sizeModifier = Modifier.onSizeChanged {
        componentSize = it
    }

    return brush to sizeModifier
}
