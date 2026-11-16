package com.senghol.knotx.services
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.JsonObject
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.R
import com.senghol.knotx.connection.SocketManager
import com.senghol.knotx.data.database.UnsentSandesh
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
class LiveLocationService : Service() {

    data class ActiveLiveLocation(
        val sandeshId: String,
        val targetId: String,
        val isGroup: Boolean,
        val expiresAt: Long  // System.currentTimeMillis() + durationMs
    )

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private val activeLiveLocations =
        mutableMapOf<String, ActiveLiveLocation>()

    private var isLoopRunning = false

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_ADD -> {
                SocketManager.isLocationRunning = true

                val sandeshId = intent.getStringExtra(EXTRA_SANDESH_ID) ?: return START_NOT_STICKY
                val targetId = intent.getStringExtra(EXTRA_TARGET_ID) ?: return START_NOT_STICKY
                val isGroup = intent.getBooleanExtra(EXTRA_IS_GROUP, false)
                val dootawas = (applicationContext as KnotxApp).knotx

                val existingLiveLocation = activeLiveLocations.values.firstOrNull {
                    it.targetId == targetId && it.isGroup == isGroup
                }

                if (existingLiveLocation != null) {
                    serviceScope.launch {
                        try {
                            dootawas.stopLiveLocationSharing(
                                context = applicationContext,
                                sandeshId = existingLiveLocation.sandeshId,
                                targetId = existingLiveLocation.targetId,
                                isGroup = existingLiveLocation.isGroup
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        activeLiveLocations.remove(existingLiveLocation.sandeshId)
                    }
                }

                val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, System.currentTimeMillis() + DURATION_20_MIN)

                activeLiveLocations[sandeshId] = ActiveLiveLocation(
                    sandeshId = sandeshId,
                    targetId = targetId,
                    isGroup = isGroup,
                    expiresAt = expiresAt  // already absolute, no math needed
                )
                if (!isLoopRunning) {
                    isLoopRunning = true
                    startForeground(1001, createNotification())
                    startLocationLoop()
                }
            }

            ACTION_REMOVE -> {

                val sandeshId =
                    intent.getStringExtra(
                        EXTRA_SANDESH_ID
                    ) ?: return START_NOT_STICKY

                activeLiveLocations.remove(
                    sandeshId
                )

                if (activeLiveLocations.isEmpty()) {

                    stopForeground(
                        STOP_FOREGROUND_REMOVE
                    )

// When location service stops
                    SocketManager.isLocationRunning = false
                    stopSelf()
                }
            }

            ACTION_STOP_ALL -> {

                val dootawas =
                    (applicationContext as KnotxApp)
                        .knotx

                serviceScope.launch {
                    SocketManager.isLocationRunning = false
                    activeLiveLocations.values
                        .toList()
                        .forEach {

                            dootawas.stopLiveLocationSharing(
                                context = applicationContext,
                                sandeshId = it.sandeshId,
                                targetId = it.targetId,
                                isGroup = it.isGroup
                            )
                        }

                    activeLiveLocations.clear()

                    stopForeground(
                        STOP_FOREGROUND_REMOVE
                    )

                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun startLocationLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val dootawas = (applicationContext as KnotxApp).knotx

                    // Find and stop all expired sessions
                    val expired = activeLiveLocations.values
                        .filter { now >= it.expiresAt }

                    expired.forEach { entry ->
                        try {
                            dootawas.stopLiveLocationSharing(
                                context = applicationContext,
                                sandeshId = entry.sandeshId,
                                targetId = entry.targetId,
                                isGroup = entry.isGroup
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        activeLiveLocations.remove(entry.sandeshId)
                    }

                    // If nothing left, stop the service
                    if (activeLiveLocations.isEmpty()) {
                        SocketManager.isLocationRunning = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }

                    // Send location for remaining active sessions
                    val location = getCurrentLocation()
                    if (location != null) {
                        SocketManager.isLocationRunning = true
                        activeLiveLocations.values.forEach { entry ->
                            val liveUpdateId = UUID.randomUUID().toString()
                            val liveLocationUpdate = JsonObject().apply {
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, entry.sandeshId)
                                addProperty(DootawasMessage.LATITUDE, location.latitude)
                                addProperty(DootawasMessage.LONGITUDE, location.longitude)
                                addProperty(DootawasMessage.IS_STOPPED, false)
                                addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIVE_LOCATION_UPDATE.value)
                                if (entry.isGroup) {
                                    addProperty(JsonKeys.GROUP_ID, entry.targetId)
                                }
                            }
                            val unsentSandesh = UnsentSandesh(
                                messId = liveUpdateId,
                                receiver = entry.targetId,
                                message = liveLocationUpdate.toString(),
                                dataType = DataType.MESSAGE,
                                isGroup = entry.isGroup
                            )
                            dootawas.sendMessage(unsentSandesh)
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(22_000)
            }
        }
    }
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation():
            Location? =
        suspendCancellableCoroutine { cont ->

            val cancellationTokenSource =
                CancellationTokenSource()

            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                .addOnSuccessListener {

                    cont.resume(it)
                }
                .addOnFailureListener {

                    cont.resume(null)
                }

            cont.invokeOnCancellation {

                cancellationTokenSource.cancel()
            }
        }
    private fun createNotification():
            Notification {

        val channelId =
            "live_location_channel"

        val channel =
            NotificationChannel(
                channelId,
                "Live Location",
                NotificationManager.IMPORTANCE_LOW
            ).apply {

                setShowBadge(false)

                lockscreenVisibility =
                    Notification.VISIBILITY_PRIVATE
            }

        getSystemService(
            NotificationManager::class.java
        ).createNotificationChannel(channel)

        val stopIntent =
            Intent(
                this,
                LiveLocationService::class.java
            ).apply {

                action = ACTION_STOP_ALL
            }

        val stopPendingIntent =
            PendingIntent.getService(
                this,
                1002,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            this,
            channelId
        )
            .setContentTitle(
                "Sharing live location"
            )
            .setContentText(
                "${activeLiveLocations.size} active live location"
            )
            .setSmallIcon(
                R.mipmap.ic_launcher_foreground
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .addAction(
                R.drawable.icon_close_24px,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    companion object {

        private const val ACTION_ADD =
            "action_add_live_location"

        private const val ACTION_REMOVE =
            "action_remove_live_location"

        private const val ACTION_STOP_ALL =
            "action_stop_all_live_location"

        private const val EXTRA_SANDESH_ID =
            "sandesh_id"

        private const val EXTRA_TARGET_ID =
            "target_id"

        private const val EXTRA_IS_GROUP =
            "is_group"
        private const val EXTRA_EXPIRES_AT = "expires_at"

        // Convenience constants callers can use
        const val DURATION_20_MIN = 20 * 60 * 1000L
        const val DURATION_1_HOUR = 60 * 60 * 1000L
        const val DURATION_8_HOURS = 8 * 60 * 60 * 1000L

        fun addLiveLocation(
            context: Context,
            sandeshId: String,
            targetId: String,
            isGroup: Boolean,
            expiresAt: Long? = null  // rename for clarity
        ) {
            val resolvedExpiresAt = expiresAt ?: (System.currentTimeMillis() + DURATION_20_MIN)

            val remainingMin = (resolvedExpiresAt - System.currentTimeMillis()) / 1000 / 60

            val intent = Intent(context, LiveLocationService::class.java).apply {
                action = ACTION_ADD
                putExtra(EXTRA_SANDESH_ID, sandeshId)
                putExtra(EXTRA_TARGET_ID, targetId)
                putExtra(EXTRA_IS_GROUP, isGroup)
                putExtra(EXTRA_EXPIRES_AT, resolvedExpiresAt)  // rename constant too
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun removeLiveLocation(
            context: Context,
            sandeshId: String
        ) {

            val intent =
                Intent(
                    context,
                    LiveLocationService::class.java
                ).apply {

                    action = ACTION_REMOVE

                    putExtra(
                        EXTRA_SANDESH_ID,
                        sandeshId
                    )
                }

            context.startService(intent)
        }

        fun stopAll(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    LiveLocationService::class.java
                ).apply {

                    action = ACTION_STOP_ALL
                }

            context.startService(intent)
        }
    }
}