package com.senghol.knotx.ui.components

// BatteryOptimizationWarning.kt
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircleOutline
import com.senghol.knotx.utils.enums.SettingPrefKeys
import kotlin.time.Duration.Companion.milliseconds


// ─── DataStore helpers ───────────────────────────────────────────────────────

suspend fun shouldShowBatteryWarning(context: Context): Boolean {
    val nextShow = context.userDataStore.data.first()[SettingPrefKeys.BATTERY_NEXT_SHOW_MS] ?: 0L
    return nextShow != -1L && System.currentTimeMillis() >= nextShow
}

suspend fun recordBatteryWarningShown(context: Context) {
    context.userDataStore.edit { it[SettingPrefKeys.BATTERY_NEXT_SHOW_MS] = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L }
}

suspend fun recordPermanentlyDismissed(context: Context) {
    context.userDataStore.edit { it[SettingPrefKeys.BATTERY_NEXT_SHOW_MS] = -1L }
}

// ─── Manufacturer intent ─────────────────────────────────────────────────────

fun getManufacturerSettingsIntent(context: Context): Intent? {
    fun intent(pkg: String, cls: String) = Intent().apply { component = ComponentName(pkg, cls) }
    fun Intent.orNull() = takeIf { isIntentResolvable(context, it) }

    return when {
        Build.MANUFACTURER.lowercase().containsAny("xiaomi", "redmi") ->
            intent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")

        Build.MANUFACTURER.lowercase().containsAny("oppo", "realme") ->
            intent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FakeActivity").orNull()
                ?: intent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")

        Build.MANUFACTURER.lowercase().contains("huawei") ->
            intent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity").orNull()
                ?: intent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")

        Build.MANUFACTURER.lowercase().contains("samsung") ->
            intent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity").orNull()
                ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = "package:${context.packageName}".toUri() }

        Build.MANUFACTURER.lowercase().contains("oneplus") ->
            intent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")

        Build.MANUFACTURER.lowercase().contains("vivo") ->
            intent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")

        Build.MANUFACTURER.lowercase().contains("asus") ->
            intent("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")

        Build.MANUFACTURER.lowercase().contains("meizu") ->
            intent("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity")

        else -> null
    }
}

private fun String.containsAny(vararg values: String) = values.any { contains(it) }

fun isIntentResolvable(context: Context, intent: Intent) =
    context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null

// ─── Composable ──────────────────────────────────────────────────────────────

@SuppressLint("BatteryLife")
@Composable
fun BoxScope.BatteryOptimizationWarning() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    var show by remember { mutableStateOf(false) }
    var isOptimized by remember { mutableStateOf(false) }

    // Check on every resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isOptimized) {
        if (!isOptimized) {
            return@LaunchedEffect
        }

        val nextShow = context.userDataStore.data.first()[SettingPrefKeys.BATTERY_NEXT_SHOW_MS] ?: 0L

        if (nextShow == -1L) {
            return@LaunchedEffect
        }

        val isFirstTime = nextShow == 0L

        if (isFirstTime) {
            delay(50_000L.milliseconds)
        } else if (System.currentTimeMillis() < nextShow) {
            Log.i("BatteryOpt", "Battery warning skipped — next show in ${(nextShow - System.currentTimeMillis()) / 1000 / 60} mins")
            return@LaunchedEffect
        }

        show = true
        recordBatteryWarningShown(context)
        Log.w("BatteryOpt", "Showing battery warning — next in 3 days")
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    val manufacturerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    AnimatedVisibility(
        visible = show,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(start = 12.dp, bottom = 130.dp)
                .widthIn(max = 260.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Title + close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { show = false },
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
                    "You might miss messages in background",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // Battery optimization button
                Card(
                    onClick = {
                        try {
                            batteryLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        } catch (e: ActivityNotFoundException) {
                            Log.e("BatteryOpt", "Falling back to settings list", e)
                            batteryLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Battery optimization",
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

                // Manufacturer button
                val manufacturerIntent = remember { getManufacturerSettingsIntent(context) }
                if (manufacturerIntent != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        onClick = {
                            try {
                                manufacturerLauncher.launch(manufacturerIntent)
                            } catch (e: ActivityNotFoundException) {
                                Log.e("BatteryOpt", "Manufacturer screen not available", e)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.AppSettingsAlt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Background activity",
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
                }

                // It's working fine
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = {
                        show = false
                        scope.launch { recordPermanentlyDismissed(context) }
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
                            Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "It's working fine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}