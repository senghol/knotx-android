package com.senghol.knotx.ui.components





import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.ui.dialogs.ContributeDialog
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

// ─── Timing constants ───────────────────────────────────────────────
private const val TWO_DAYS_MS = 2L * 24 * 60 * 60 * 1000

/** Next occurrence of day-of-month [targetDay] at 10:00, strictly after [now]. */
fun nextMonthlyTriggerMs(now: Long = System.currentTimeMillis(), targetDay: Int = 5): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, targetDay)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (cal.timeInMillis <= now) {
        cal.add(Calendar.MONTH, 1)
        // no need to re-set DAY_OF_MONTH — add(MONTH) preserves it
    }

    return cal.timeInMillis
}


/** "Not now" / donated — go quiet until next month's 5th. */
suspend fun recordDonationSnoozed(context: Context) {
    context.userDataStore.edit {
        it[SettingPrefKeys.DONATION_NEXT_SHOW_MS] = nextMonthlyTriggerMs()
    }
}

// ─── Composable card ─────────────────────────────────────────────────
@Composable
fun BoxScope.AskDonationReminderCard(
    knotx: Knotx
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }
    var showContributeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.userDataStore.data.first()
        var nextShow = prefs[SettingPrefKeys.DONATION_NEXT_SHOW_MS] ?: 0L

        // First ever launch — schedule the 2-day mark and persist immediately
        // so it survives the app being killed before 2 days pass.
        if (nextShow == 0L) {
            nextShow = System.currentTimeMillis() + TWO_DAYS_MS
            context.userDataStore.edit {
                it[SettingPrefKeys.DONATION_NEXT_SHOW_MS] = nextShow
            }
        }

        // Show every time the app opens on/after nextShow, until the user
        // taps "Not now" or donates (that's what pushes nextShow forward).
        if (System.currentTimeMillis() >= nextShow) {
            show = true
        }
    }

    if (showContributeDialog) {
        ContributeDialog(
            onDismiss = { showContributeDialog = false },
            onContribute = { amount ->
                knotx.createOrderID(amount)
                showContributeDialog = false
            }
        )
    }

    AnimatedVisibility(
        visible = show,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(end = 12.dp, bottom = 130.dp)
                .widthIn(max = 260.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "❤️ Proud non-profit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            show = false
                            scope.launch { recordDonationSnoozed(context) }
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    "No ads • No data selling • No spam",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )

                Text(
                    "We remind you once a month, around the 5th",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Card(
                    onClick = {
                        show = false
                        scope.launch { recordDonationSnoozed(context) }
                        showContributeDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Donate now",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Default.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    onClick = {
                        show = false
                        scope.launch { recordDonationSnoozed(context) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Not now",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}