package com.senghol.knotx.ui.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.R
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.utils.data.ParsedLedgerSandesh
import kotlin.math.abs
import kotlin.math.roundToInt

// Required imports (add whatever your file is missing):
// import androidx.compose.foundation.*
// import androidx.compose.foundation.gestures.detectHorizontalDragGestures
// import androidx.compose.foundation.interaction.MutableInteractionSource
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.*
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.geometry.Offset
// import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.StrokeCap
// import androidx.compose.ui.input.pointer.pointerInput
// import androidx.compose.ui.platform.LocalDensity
// import androidx.compose.ui.res.painterResource
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextOverflow
// import androidx.compose.ui.unit.IntOffset
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import kotlin.math.abs
// import kotlin.math.roundToInt

// ParsedLedgerSandesh needs these new fields:
//   val billTotal: Double = 0.0,
//   val amountPaid: Double = 0.0,
//   val invoiceNumber: String = "",

@Composable
fun LedgerBubble(
    item: ParsedLedgerSandesh,
    dootawasColors: DootawasColors,
    currentBalance: Double,
    isDark: Boolean = true,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onAccept: (sandeshId: String, resolvedBalance: Double) -> Unit,
    onReject: (sandeshId: String, resolvedBalance: Double) -> Unit,
    onClick: (ParsedLedgerSandesh) -> Unit,
    onReplySlide: () -> Unit
) {
    var showRelativeTime by remember { mutableStateOf(false) }
    val showActions = item.ledgerStatus == LedgerStatus.PENDING && !item.isSelf

    val previewBalance = when (item.ledgerType) {
        LedgerType.BILL, LedgerType.CREDIT ->
            if (item.isSelf) currentBalance + item.amount else currentBalance - item.amount
        LedgerType.DEBIT ->
            if (item.isSelf) currentBalance - item.amount else currentBalance + item.amount
        else -> currentBalance
    }

    // ── Bill-derived state ───────────────────────────────────────────
    val isBill = item.ledgerType == LedgerType.BILL
    val isFullyPaid = isBill && item.billTotal > 0 && abs(item.amountPaid - item.billTotal) < 0.005
    val isOverPaid = isBill && item.amountPaid > item.billTotal + 0.005
    val paidFraction =
        if (item.billTotal > 0) (item.amountPaid / item.billTotal).toFloat().coerceIn(0f, 1f) else 0f

    val green = Color(0xFF4CAF50)
    val red = Color(0xFFE53935)

    val amountCaption = when {
        !isBill -> null
        isFullyPaid -> "Settled"
        isOverPaid -> "Overpaid by"
        item.isSelf -> "Remaining to receive"
        else -> "Remaining to pay"
    }

    val typeIcon = when {
        item.ledgerType == LedgerType.BILL -> Icons.Filled.Receipt
        item.ledgerType == LedgerType.LEDGER_ACK_BILL_CREDIT ||
                item.ledgerType == LedgerType.LEDGER_ACK_DEBIT -> Icons.Default.Check
        item.isPositive -> Icons.Default.Add
        else -> Icons.Default.Remove
    }

    val typeLabel = when (item.ledgerType) {
        LedgerType.BILL -> if (item.isSelf) "Bill sent" else "Bill received"
        else -> "Balance"
    }

    val acceptLabel = if (isBill) "Accept" else "Confirm"

    val statusColor = when (item.ledgerStatus) {
        LedgerStatus.PENDING -> Color(0xFFFF9800)
        LedgerStatus.ACCEPTED -> green
        LedgerStatus.REJECTED -> red
    }

    val bgColor = when {
        isHighlighted -> Color(0xFFD50000).copy(alpha = 0.1f)
        isSelected -> Color(0xFFB3E5FC)
        isDark -> Color(0xFF1E1E1E)
        else -> Color(0xFFF5F5F5)
    }

    val borderColor = when {
        isHighlighted -> Color(0xFFD50000)
        isSelected -> Color.Blue
        isDark -> Color(0xFF525151)
        else -> Color(0xFFE0E0E0)
    }

    // green if good for viewer, red if bad
    val accentColor = if (item.isPositive) green else red
    val amountColor = if (isFullyPaid) green else accentColor

    val textColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val timeTextColor = if (isDark) Color(0xFF888888) else Color(0xFF999999)

    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = if (item.isSelf) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount > 0 || offsetX > 0f) {
                                offsetX = (offsetX + dragAmount).coerceIn(0f, maxSwipe)
                            }
                        },
                        onDragEnd = {
                            if (offsetX > trigger) onReplySlide()
                            offsetX = 0f
                        }
                    )
                }
                .widthIn(min = 220.dp, max = 280.dp)
                .background(bgColor, RoundedCornerShape(16.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelection() else onClick(item)
                    },
                    onLongClick = onLongPress
                )
        ) {
            Column {

                // ── Header strip ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .background(accentColor, CircleShape)
                        ) {
                            Icon(
                                typeIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                typeLabel,
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(0.15f), RoundedCornerShape(50))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    item.ledgerStatus.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (item.invoiceNumber.isNotBlank()) {
                            Text(
                                item.invoiceNumber,
                                fontSize = 10.sp,
                                color = timeTextColor
                            )
                        }
                    }
                }

                // ── Amount + bill breakdown + note + balance ─────────
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (amountCaption != null) {
                        Text(amountCaption, color = timeTextColor, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                    }

                    Text(
                        if (isFullyPaid) "₹0.00"
                        else (if (item.isPositive) "+₹%.2f" else "-₹%.2f").format(item.amount),
                        color = amountColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isBill) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { paidFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = green,
                            trackColor = timeTextColor.copy(alpha = 0.25f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(8.dp))
                        BillRow("Bill total", "₹%.2f".format(item.billTotal), textColor, timeTextColor)
                        BillRow("Paid", "₹%.2f".format(item.amountPaid), green, timeTextColor)
                        BillRow(
                            label = if (isOverPaid) "Overpaid" else "Remaining",
                            value = "₹%.2f".format(item.amount),
                            valueColor = amountColor,
                            labelColor = timeTextColor
                        )
                    }

                    if (item.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.note,
                            color = textColor,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Balance strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                accentColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (item.ledgerStatus) {
                            LedgerStatus.ACCEPTED -> {
                                Text("New balance", fontSize = 10.sp, color = timeTextColor)
                                Text(
                                    "₹%.2f".format(item.resolvedBalance ?: 0.0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            LedgerStatus.PENDING -> {
                                Text("Balance after accepting", fontSize = 10.sp, color = timeTextColor)
                                Text(
                                    "₹%.2f".format(previewBalance),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            LedgerStatus.REJECTED -> {
                                Text("Not applied to balance", fontSize = 10.sp, color = timeTextColor)
                            }
                        }
                    }
                }

                // ── Accept / Reject ──────────────────────────────────
                if (showActions) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f), thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionButton(
                            label = "Decline",
                            icon = Icons.Default.Close,
                            tint = red,
                            modifier = Modifier.weight(1f),
                            onClick = { onReject(item.sandeshId, previewBalance) }
                        )

                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(38.dp)
                                .align(Alignment.CenterVertically)
                                .background(borderColor.copy(alpha = 0.4f))
                        )

                        ActionButton(
                            label = acceptLabel,
                            icon = Icons.Default.Check,
                            tint = green,
                            modifier = Modifier.weight(1f),
                            onClick = { onAccept(item.sandeshId, previewBalance) }
                        )
                    }
                }

                // ── Time row ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 10.dp, bottom = 7.dp, top = if (showActions) 0.dp else 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showRelativeTime) item.formattedDate else item.formattedTime,
                        fontSize = 10.sp,
                        color = timeTextColor,
                        modifier = Modifier.clickable { showRelativeTime = !showRelativeTime }
                    )
                    if (item.isSelf) {
                        val iconTint = when (item.sandeshStithi) {
                            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
                            SandeshStithi.PREKSHIT -> Color.Gray
                            SandeshStithi.DHRIST -> Color(0xFF00C853)
                            else -> null
                        }
                        if (iconTint != null) {
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                painter = painterResource(R.drawable.icon_status),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = iconTint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: Color, labelColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = labelColor)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}