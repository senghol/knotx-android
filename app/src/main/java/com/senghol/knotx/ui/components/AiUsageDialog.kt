package com.senghol.knotx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.senghol.knotx.utils.data.AiWalletBalance
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class AiPack(val id: String, val priceInr: Int, val credits: Long)

// Keep in sync with AiCreditConfig.PACKS on the server (the server credits by the amount actually paid)
val AI_PACKS = listOf(
    AiPack("pack_9", 9, 900),
    AiPack("pack_49", 49, 5_734),
    AiPack("pack_99", 99, 12_450),
    AiPack("pack_199", 199, 26_842),
    AiPack("pack_499", 499, 76_207)
)

private const val INPUT_TOKENS_PER_CREDIT = 1_000_000L / 1_797L
private const val OUTPUT_TOKENS_PER_CREDIT = 1_000_000L / 8_982L

// ---------- palette (own colors, light + dark) ----------
private class AiPalette(
    val sheet: Color, val card: Color, val stroke: Color,
    val text: Color, val muted: Color,
    val accent: Color, val heroA: Color, val heroB: Color
)

private val DarkPalette = AiPalette(
    sheet = Color(0xFF111111), card = Color(0xFF121212), stroke = Color(0xFF2E2E40),
    text = Color(0xFFF2F2F8), muted = Color(0xFF9B9BB4),
    accent = Color(0xFF8B7CFF), heroA = Color(0xFF4F3FD6), heroB = Color(0xFF9A4DEB)
)
private val LightPalette = AiPalette(
    sheet = Color(0xFFFFFFFF), card = Color(0xFFF4F2FC), stroke = Color(0xFFE2DFF3),
    text = Color(0xFF1B1B2B), muted = Color(0xFF6E6E86),
    accent = Color(0xFF5B4BDB), heroA = Color(0xFF5B4BDB), heroB = Color(0xFF9A4DEB)
)

private fun compact(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(Locale.US, n / 1_000_000.0).replace(".0M", "M")
    n >= 10_000 -> "${n / 1_000}K"
    n >= 1_000 -> "%.1fK".format(Locale.US, n / 1_000.0).replace(".0K", "K")
    else -> n.toString()
}

private fun bonusPercent(p: AiPack): Int =
    (((p.credits.toDouble() / (p.priceInr * 100)) - 1) * 100).roundToInt()

@Composable
fun AiUsageDialog(
    balance: AiWalletBalance,
    isRefreshing: Boolean = false,
    packs: List<AiPack> = AI_PACKS,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onBuy: (AiPack) -> Unit
) {
    val c = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    var showTokens by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val credits = balance.balance.coerceAtLeast(0L)
    val inputTokens = credits * INPUT_TOKENS_PER_CREDIT
    val outputTokens = credits * OUTPUT_TOKENS_PER_CREDIT

    val shown by animateFloatAsState(
        targetValue = credits.toFloat(),
        animationSpec = tween(600),
        label = "balance"
    )
    val angle by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle"
    )
    val unit = if (showTokens) "tokens" else "credits"
    fun fmt(creditsValue: Long): String {
        return if (showTokens) {
            val input = compact(creditsValue * INPUT_TOKENS_PER_CREDIT)
            val output = compact(creditsValue * OUTPUT_TOKENS_PER_CREDIT)
            "≈ $input input · $output output"
        } else {
            NumberFormat.getIntegerInstance().format(creditsValue)
        }
    }
    val bestId = packs.maxByOrNull { bonusPercent(it) }?.id

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.sheet)
                .border(1.dp, c.stroke, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- hero ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(c.heroA, c.heroB)))
                    .padding(18.dp)
            ) {
                // soft decorative circles
                Box(
                    Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                )
                Box(
                    Modifier.size(70.dp).align(Alignment.BottomEnd).offset(x = (-20).dp, y = 30.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UnitSwitch(showTokens) { showTokens = it }
                        Spacer(Modifier.weight(1f))
                        RoundIcon(onClick = onRefresh, enabled = !isRefreshing) {
                            Icon(
                                Icons.Default.Refresh, "Refresh balance", tint = Color.White,
                                modifier = Modifier.size(18.dp)
                                    .graphicsLayer { rotationZ = if (isRefreshing) angle else 0f }
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        RoundIcon(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Ariksh balance", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                    if (showTokens) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "≈ ${compact(inputTokens)} input",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "≈ ${compact(outputTokens)} output",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Text(
                            NumberFormat.getIntegerInstance().format(shown.toLong()),
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = if (showTokens) "Estimated separately using Luna's input and output rates" else "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }


            // ---------- add credits ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.accent)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Add credits", color = Color.White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = Color.White
                )
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    packs.forEach { pack ->
                        val best = pack.id == bestId
                        val bonus = bonusPercent(pack)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.card)
                                .border(
                                    if (best) 1.5.dp else 1.dp,
                                    if (best) c.accent else c.stroke,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onBuy(pack) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("₹${pack.priceInr}", color = c.text, fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(72.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${fmt(pack.credits)} $unit", color = c.text, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold)
                                if (bonus > 0) {
                                    Text(
                                        if (best) "Best value · +$bonus% extra" else "+$bonus% extra",
                                        color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                "Buy", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(c.accent)
                                    .padding(horizontal = 18.dp, vertical = 7.dp)
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun RoundIcon(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun UnitSwitch(showTokens: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(3.dp)
    ) {
        listOf(
            false to "Credits",
            true to "Tokens"
        ).forEach { (tokens, label) ->
            val selected = showTokens == tokens

            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF3D2FB8)
                else Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onChange(tokens) }
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            )
        }
    }
}