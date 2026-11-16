package com.senghol.knotx.ui.components// Simple contact model for this screen — swap for your existing Upbhogta/contact type if you have one

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi

import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.location.Geocoder
import android.location.Location
import android.provider.MediaStore
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.JsonObject
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.senghol.knotx.ui.components.cropAndSaveMapToMedia
import com.senghol.knotx.ui.screens.SavedMedia
import com.senghol.knotx.utils.enums.DootawasMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
// Simple contact model for this screen — swap for your existing Upbhogta/contact type if you have one


// Simple contact model for this screen — swap for your existing Upbhogta/contact type if you have one
data class SosContact(
    val id: String,
    val name: String,
    val avatarUrl: String? = null
)

private const val MAX_LOCATION_AGE_MS = 30_000L      // reject fixes older than this
private const val MAX_ACCEPTABLE_ACCURACY_M = 50f     // reject fixes worse than this
private const val LOCATION_TIMEOUT_MS = 25_000L       // give up waiting after this long

private sealed class LocationFixState {
    object Loading : LocationFixState()
    data class Ready(val latLng: LatLng, val address: String, val accuracy: Float) : LocationFixState()
    object TimedOut : LocationFixState()
}
fun createLocationMessage(
    latLng: LatLng,
    savedMedia: SavedMedia?,
    isLive: Boolean,
    expiryTime: Long?
): JsonObject {

    return JsonObject().apply {

        addProperty(
            DootawasMessage.ORIGINAL_URI,
            savedMedia?.localPath ?: ""
        )

        addProperty(
            DootawasMessage.DOWNLOAD_SIZE,
            savedMedia?.fileSize ?: 0L
        )

        addProperty(
            DootawasMessage.MIME_TYPE,
            savedMedia?.mimeType ?: "image/jpeg"
        )

        addProperty(
            DootawasMessage.LATITUDE,
            latLng.latitude
        )

        addProperty(
            DootawasMessage.LONGITUDE,
            latLng.longitude
        )

        addProperty(
            DootawasMessage.IS_LIVE,
            isLive
        )

        if (expiryTime != null) {

            addProperty(
                DootawasMessage.LIVE_UNTIL,
                expiryTime
            )

        } else {

            addProperty(
                DootawasMessage.LIVE_UNTIL,
                ""
            )
        }
    }
}
@Composable
fun BoxScope.SosQuickShareCard(
    sosContacts: List<SosContact>,
    onNavigateToAddSosContacts: () -> Unit,
    onSend: (JsonObject, List<String>) -> Unit, // same shape as ModernMapPicker's onLocationSelected, contacts appended
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    // Redirect immediately if there's nobody to send to — no point showing the rest of the UI
    LaunchedEffect(sosContacts) {
        if (sosContacts.isEmpty()) {
            onNavigateToAddSosContacts()
        }
    }
    if (sosContacts.isEmpty()) return

    var selectedIds by remember(sosContacts) {
        mutableStateOf(sosContacts.map { it.id }.toSet()) // default: everyone selected for SOS
    }
    var fixState by remember { mutableStateOf<LocationFixState>(LocationFixState.Loading) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var isSending by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
    // Acquire a FRESH fix — never reuse lastLocation for SOS, it can be stale/wrong.
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            fixState = LocationFixState.TimedOut
            return@LaunchedEffect
        }

        val result = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = com.google.android.gms.tasks.CancellationTokenSource()
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { loc -> cont.resume(loc) {} }
                        .addOnFailureListener { cont.resume(null) {} }
                } catch (_: SecurityException) {
                    cont.resume(null) {}
                }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }

        val ageMs = result?.let { System.currentTimeMillis() - it.time }

        if (result == null || result.accuracy > MAX_ACCEPTABLE_ACCURACY_M ||
            (ageMs != null && ageMs > MAX_LOCATION_AGE_MS)
        ) {
            fixState = LocationFixState.TimedOut
            return@LaunchedEffect
        }

        val latLng = LatLng(result.latitude, result.longitude)
        currentLocation = latLng
        val address = withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    ?.firstOrNull()?.getAddressLine(0) ?: "Unknown location"
            } catch (_: Exception) {
                "Unknown location"
            }
        }

        fixState = LocationFixState.Ready(latLng, address, result.accuracy)
    }

    // isLive=true always means 6 hours here — no duration picker, SOS should be one tap.
    fun sendNow(isLive: Boolean) {
        val ready = fixState as? LocationFixState.Ready ?: return
        if (isSending) return
        isSending = true

        val recipients = sosContacts.filter { it.id in selectedIds }
        val expiryTime = if (isLive) System.currentTimeMillis() + 6 * 60 * 60 * 1000L else null

        val userNumbers = recipients.map { it.id }
        val map = googleMap
        if (map == null) {
            onSend(
                createLocationMessage(
                    ready.latLng,
                    null,
                    isLive,
                    expiryTime
                ),
                userNumbers
            )
            return
        }

        val marker = map.addMarker(
            MarkerOptions()
                .position(ready.latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
        )

        coroutineScope.launch {
            delay(120) // let the marker render before snapshotting
            map.snapshot { bitmap ->
                marker?.remove()
                if (bitmap != null) {
                    cropAndSaveMapToMedia(context, bitmap) { savedMedia ->

                        onSend(
                            createLocationMessage(
                                ready.latLng,
                                savedMedia,
                                isLive,
                                expiryTime
                            ),
                            userNumbers
                        )
                    }
                } else {
                    onSend(
                        createLocationMessage(
                            ready.latLng,
                            null,
                            isLive,
                            expiryTime
                        ),
                        userNumbers
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(20.dp)
            .widthIn(max = 340.dp)
            .heightIn(max = 520.dp), // bigger than AskDonationReminderCard's 260dp-wide compact card, still not full-screen
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.Sos,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Send emergency location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Small, NON-interactive map preview — just for visual confirmation, not for repositioning.
            // liteMode + all gestures disabled keeps this cheap and prevents accidental drag-to-wrong-location.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                when (val state = fixState) {
                    is LocationFixState.Ready -> {
                        val markerState = remember { MarkerState(position = state.latLng) }
                        LaunchedEffect(state.latLng) {
                            markerState.position = state.latLng
                        }
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(state.latLng, 16f)
                            },
                            properties = MapProperties(
                                isMyLocationEnabled = hasLocationPermission
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                scrollGesturesEnabled = true,
                                zoomGesturesEnabled = true,
                                tiltGesturesEnabled = false,
                                rotationGesturesEnabled = false,
                                myLocationButtonEnabled = false,
                                mapToolbarEnabled = false
                            )
                        ) {
                            Marker(state = markerState)
                            MapEffect { map -> googleMap = map }
                        }
                        FloatingActionButton(
                            onClick = {
                                currentLocation?.let { location ->
                                    val camera = CameraPosition.fromLatLngZoom(location, 16f)

                                    googleMap?.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(camera)
                                    )
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(35.dp),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null
                            )
                        }
                    }
                    LocationFixState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                    LocationFixState.TimedOut -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Couldn't get an accurate fix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Status line — what's actually going to be sent, in plain text, before the person taps Send
            Text(
                text = when (val state = fixState) {
                    is LocationFixState.Ready -> "${state.address}  (±${state.accuracy.toInt()}m)"
                    LocationFixState.Loading -> "Getting precise location…"
                    LocationFixState.TimedOut -> "Check your GPS/network connection and try again"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "Send to (${selectedIds.size}/${sosContacts.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 180.dp)
            ) {
                items(sosContacts, key = { it.id }) { contact ->
                    val isChecked = contact.id in selectedIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedIds = if (isChecked) selectedIds - contact.id else selectedIds + contact.id
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                contact.name.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            contact.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                selectedIds = if (it) selectedIds + contact.id else selectedIds - contact.id
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val canSend = fixState is LocationFixState.Ready && selectedIds.isNotEmpty() && !isSending

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { sendNow(isLive = false) },
                    enabled = canSend,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Current")
                }
                FilledTonalButton(
                    onClick = { sendNow(isLive = true) }, // always 6 hours — no picker, one tap
                    enabled = canSend,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Live · 6h")
                }
            }
        }
    }
}