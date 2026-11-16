package com.senghol.knotx.ui.components

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
import android.provider.MediaStore
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.MarkerOptions
import com.senghol.knotx.ui.screens.MediaCategory
import com.senghol.knotx.ui.screens.MediaVisibility
import com.senghol.knotx.ui.screens.SavedMedia
import com.senghol.knotx.ui.screens.saveMediaToAppStorageNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

fun cropAndSaveMapToMedia(
    context: Context,
    bitmap: Bitmap,
    onResult: (SavedMedia?) -> Unit
) {

    try {

        val targetWidth = 810
        val targetHeight = 600

        val cropX =
            (bitmap.width - targetWidth) / 2

        val cropY =
            (bitmap.height - targetHeight) / 2

        val croppedBitmap =
            Bitmap.createBitmap(
                bitmap,
                cropX.coerceAtLeast(0),
                cropY.coerceAtLeast(0),
                targetWidth.coerceAtMost(bitmap.width),
                targetHeight.coerceAtMost(bitmap.height)
            )

        // ------------------------------------------------
        // Temporary cache file
        // ------------------------------------------------
        val tempFile =
            File.createTempFile(
                "location_temp",
                ".jpg",
                context.cacheDir
            )

        FileOutputStream(tempFile).use { output ->

            croppedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                90,
                output
            )
        }

        croppedBitmap.recycle()

        // ------------------------------------------------
        // Save using unified storage system
        // ------------------------------------------------
        val savedMedia =
            saveMediaToAppStorageNext(
                context = context,
                sourceUri = tempFile.toUri(),
                storageType = MediaCategory.IMAGE,
                visibility = MediaVisibility.PRIVATE,
                overrideMimeType = "image/jpeg"
            )

        tempFile.delete()

        onResult(savedMedia)

    } catch (e: Exception) {

        e.printStackTrace()

        onResult(null)
    }
}

@OptIn(MapsComposeExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModernMapPicker(
    onLocationSelected: (
        LatLng,
        SavedMedia?,
        Boolean,
        Long?
    ) -> Unit,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultLocation = LatLng(28.6139, 77.2090)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
    }
    var showLiveDurationSheet by remember {
        mutableStateOf(false)
    }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var selectedAddress by remember { mutableStateOf("Move map to select location") }
    var isMoving by remember { mutableStateOf(false) }
    val fusedLocationClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    BackHandler(onBack = onDismiss)

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasLocationPermission = isGranted
        }

// 🔥 Ask permission
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            hasLocationPermission = true
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

// 🔥 Get location AFTER permission
    LaunchedEffect(hasLocationPermission) {

        if (!hasLocationPermission) return@LaunchedEffect

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                if (location != null) {

                    val latLng = LatLng(location.latitude, location.longitude)
                    currentLocation = latLng

                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                        )
                    }

                } else {
                    // 🔥 INLINE fallback (no separate function)

                    val request = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        1000
                    ).setMaxUpdates(1).build()

                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.lastLocation?.let {

                                val latLng = LatLng(it.latitude, it.longitude)
                                currentLocation = latLng

                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                    )
                                }
                            }

                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }

                    try {
                        fusedLocationClient.requestLocationUpdates(
                            request,
                            callback,
                            context.mainLooper
                        )
                    } catch (_: SecurityException) {}
                }
            }
        } catch (_: SecurityException) {}
    }

    // Detect map movement
    LaunchedEffect(cameraPositionState.isMoving) {

        isMoving = cameraPositionState.isMoving

        if (!cameraPositionState.isMoving) {

            val target = cameraPositionState.position.target

            withContext(Dispatchers.IO) {
                try {

                    val result = geocoder.getFromLocation(
                        target.latitude,
                        target.longitude,
                        1
                    )

                    result?.firstOrNull()?.let {
                        selectedAddress = it.getAddressLine(0)
                    }

                } catch (e: Exception) {
                    selectedAddress = "Unknown location"
                }
            }
        }
    }

    fun shareLiveLocation(
        durationMillis: Long
    ) {

        val expiryTime =
            System.currentTimeMillis() +
                    durationMillis

        val location =
            currentLocation
                ?: cameraPositionState.position.target

        googleMap?.let { map ->

            val marker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
            )

            coroutineScope.launch {

                delay(120)

                map.snapshot { bitmap ->

                    marker?.remove()

                    if (bitmap != null) {

                        cropAndSaveMapToMedia(
                            context,
                            bitmap
                        ) { savedMedia ->

                            onLocationSelected(
                                location,
                                savedMedia,
                                true,
                                expiryTime
                            )
                        }

                    } else {

                        onLocationSelected(
                            location,
                            null,
                            true,
                            expiryTime
                        )
                    }
                }
            }
        }
    }
    if (showLiveDurationSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                showLiveDurationSheet = false
            }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Share live location",
                    style = MaterialTheme.typography.titleLarge
                )

                LiveDurationItem(
                    text = "20 Minutes"
                ) {

                    showLiveDurationSheet = false

                    shareLiveLocation(
                        durationMillis = 20 * 60 * 1000L
                    )
                }

                LiveDurationItem(
                    text = "1 Hour"
                ) {

                    showLiveDurationSheet = false

                    shareLiveLocation(
                        durationMillis = 60 * 60 * 1000L
                    )
                }

                LiveDurationItem(
                    text = "6 Hours"
                ) {

                    showLiveDurationSheet = false

                    shareLiveLocation(
                        durationMillis = 6 * 60 * 60 * 1000L
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }
    Box(Modifier.fillMaxSize()) {

        // GOOGLE MAP
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false
            )
        ) {
            MapEffect { map ->
                googleMap = map
            }
        }

        // CENTER PIN
        val pinOffset by animateDpAsState(
            if (isMoving) 12.dp else 0.dp,
            label = ""
        )

        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -pinOffset)
                .size(42.dp)
        )

        // FLOATING CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.zoomIn()
                        )
                    }
                }
            ) {
                Icon(Icons.Default.Add, null)
            }
            FloatingActionButton(
                onClick = {

                    currentLocation?.let {

                        coroutineScope.launch {

                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(it, 16f)
                            )

                        }

                    }

                }
            ) {
                Icon(Icons.Default.MyLocation, null)
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.zoomOut()
                        )
                    }
                }
            ) {
                Icon(Icons.Default.Remove, null)
            }
        }

        // BOTTOM PANEL
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 8.dp
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    "Selected Location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(selectedAddress)

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // SEND LOCATION (70%)
                    Button(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {


                            val location = cameraPositionState.position.target

                            googleMap?.let { map ->

                                val marker = map.addMarker(
                                    MarkerOptions()
                                        .position(location)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                                )

                                coroutineScope.launch {

                                    delay(120) // allow map to render marker

                                    map.snapshot { bitmap ->

                                        marker?.remove()

                                        if (bitmap != null) {

                                            cropAndSaveMapToMedia(context, bitmap) { uri ->

                                                onLocationSelected(
                                                    location,
                                                    uri,
                                                    false,
                                                    null
                                                )

                                            }

                                        } else {

                                            onLocationSelected(
                                                location,
                                                null,
                                                false,
                                                null
                                            )

                                        }
                                    }
                                }
                            }                        }
                    ) {

                        Icon(Icons.AutoMirrored.Filled.Send, null)

                        Spacer(Modifier.width(6.dp))

                        Text("Current Location")
                    }

                    // LIVE LOCATION (30%)
                    FilledTonalButton(
                        modifier = Modifier
                            .weight(0.3f)
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        onClick = {

                                showLiveDurationSheet = true
                                                    }
                    ) {

                        Icon(Icons.Default.LocationOn, null)
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun LiveDurationItem(
    text: String,
    onClick: () -> Unit
) {

    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {

        Text(text)
    }
}