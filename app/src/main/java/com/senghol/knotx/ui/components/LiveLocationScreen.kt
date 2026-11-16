package com.senghol.knotx.ui.components
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import kotlinx.coroutines.launch
import com.google.maps.android.compose.MarkerComposable

fun distanceBetween(
    start: LatLng,
    end: LatLng
): Float {

    val results = FloatArray(1)

    Location.distanceBetween(
        start.latitude,
        start.longitude,
        end.latitude,
        end.longitude,
        results
    )
    return results[0]
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveLocationScreen(
    sandeshId: String,
    sandeshViewModel: SandeshViewModel,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val liveLocationItems by sandeshViewModel
        .liveLocationItems
        .collectAsState()

    val liveRoutes by sandeshViewModel
        .liveRoutes
        .collectAsState()

    val item =
        liveLocationItems[sandeshId]
            ?: return

    val routePoints =
        liveRoutes[sandeshId]
            ?: emptyList()

    var myLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    var isDirectionMode by remember {
        mutableStateOf(false)
    }
    val myTrailPoints = remember {
        mutableStateListOf<LatLng>()
    }
    val currentLocation = remember(
        item.latitude,
        item.longitude
    ) {
        LatLng(
            item.latitude,
            item.longitude
        )
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val hasLocationPermission = locationPermissionState.status.isGranted

    var skippedRouteUpdates by remember {
        mutableIntStateOf(0)
    }
    var lastRouteDestination by remember {
        mutableStateOf<LatLng?>(null)
    }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                currentLocation,
                16f
            )
        }

    val markerState =
        rememberUpdatedMarkerState(
            position = currentLocation
        )

    val fusedLocationClient =
        remember {
            LocationServices
                .getFusedLocationProviderClient(context)
        }

    var lastRouteFetchTime by remember {
        mutableLongStateOf(0L)
    }

    if (!hasLocationPermission) {
        LaunchedEffect(Unit) {
            locationPermissionState.launchPermissionRequest()
        }

        // Show a placeholder while waiting for the user's response
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Location permission required",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                // If user denied once, show a button to open settings
                if (locationPermissionState.status.shouldShowRationale) {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                }
            }
        }
        return
    }
    DisposableEffect(Unit) {

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000
            )
                .setMinUpdateDistanceMeters(5f)
                .build()

        val locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    val location =
                        result.lastLocation ?: return

                    val newLocation = LatLng(
                        location.latitude,
                        location.longitude
                    )

                    if (myLocation != null) {

                        val results = FloatArray(1)

                        Location.distanceBetween(
                            myLocation!!.latitude,
                            myLocation!!.longitude,
                            newLocation.latitude,
                            newLocation.longitude,
                            results
                        )

                        if (results[0] < 8f) {
                            return
                        }
                    }
                    myLocation = newLocation
                    myTrailPoints.add(
                        LatLng(
                            location.latitude,
                            location.longitude
                        )
                    )

                    if (myTrailPoints.size > 50) {
                        myTrailPoints.removeAt(0)
                    }
                }
            }

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        onDispose {

            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }
    }
    LaunchedEffect(
        item.latitude,
        item.longitude,
        isDirectionMode
    ) {

        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLng(
                currentLocation
            ),
            durationMs = 1000
        )

        if (isDirectionMode) {

            myLocation?.let { start ->

                val now =
                    System.currentTimeMillis()

                val shouldFetchByTime =
                    now - lastRouteFetchTime > 15_000

                val movedDistance =
                    lastRouteDestination?.let {
                        distanceBetween(
                            it,
                            currentLocation
                        )
                    } ?: Float.MAX_VALUE

                val shouldFetch =
                    movedDistance > 50f ||
                            skippedRouteUpdates >= 10

                if (
                    shouldFetchByTime &&
                    shouldFetch
                ) {

                    lastRouteFetchTime = now
                    lastRouteDestination = currentLocation
                    skippedRouteUpdates = 0

                    sandeshViewModel.requestLiveLocationRoute(
                        sandeshId = sandeshId,
                        originLat = start.latitude,
                        originLng = start.longitude,
                        destinationLat = currentLocation.latitude,
                        destinationLng = currentLocation.longitude
                    )

                } else if (shouldFetchByTime) {

                    lastRouteFetchTime = now
                    skippedRouteUpdates++
                }
            }
        }

    }
    val imageBitmap = remember(item.profileUri) {

        item.profileUri?.let { uri ->

            try {

                BitmapFactory.decodeFile(
                    uri.path
                )?.asImageBitmap()

            } catch (e: Exception) {

                e.printStackTrace()
                null
            }
        }
    }


    val snappedData =
        if (
            myLocation != null &&
            routePoints.isNotEmpty()
        ) {

            val (
                snappedLocation,
                snappedIndex
            ) = findNearestPointOnRoute(
                myLocation!!,
                routePoints
            )

            snappedLocation to (
                    listOf(snappedLocation) +
                            routePoints.drop(
                                snappedIndex + 1
                            )
                    )

        } else null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission  // ← was hardcoded `true`
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false
            )
        ) {

            routePoints.takeIf { it.isNotEmpty() }?.let {

                Polyline(
                    points = it,
                    width = 40f,
                    color = Color.White,
                    geodesic = true,
                    jointType = JointType.ROUND
                )

                Polyline(
                    points = it,
                    width = 32f,
                    color = Color(0xFF9D9D9D),
                    geodesic = true,
                    jointType = JointType.ROUND
                )
            }

            snappedData?.let {
                    (
                        snappedLocation,
                        remainingRoute
                    ) ->

                Polyline(
                    points = remainingRoute,
                    width = 32f,
                    color = Color(0xFF4285F4),
                    geodesic = true,
                    jointType = JointType.ROUND
                )

                Circle(
                    center = snappedLocation,
                    radius = 2.0,
                    fillColor = Color.White,
                    strokeColor = Color(0xFF4285F4),
                    strokeWidth = 6f
                )
            }

            routePoints.lastOrNull()?.let {

                Circle(
                    center = it,
                    radius = 2.0,
                    fillColor = Color(0xFFB4FF28),
                    strokeColor = Color.White,
                    strokeWidth = 6f
                )
            }

            MarkerComposable(
                state = markerState
            ) {

                LiveMarkerContent(
                    imageBitmap = imageBitmap,
                    displayName = item.displayName,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            FloatingActionButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 5.dp
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (item.profileUri != null) {

                        AsyncImage(
                            model = item.profileUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = item.displayName
                                    .firstOrNull()
                                    ?.uppercase() ?: "?",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = if (item.isLive) {
                                "Live location"
                            } else {
                                "Shared location"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    bottom = 52.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FloatingActionButton(
                onClick = {

                    isDirectionMode = true

                    myLocation?.let { start ->

                        sandeshViewModel.requestLiveLocationRoute(
                            sandeshId = sandeshId,
                            originLat = start.latitude,
                            originLng = start.longitude,
                            destinationLat = currentLocation.latitude,
                            destinationLng = currentLocation.longitude
                        )

                        coroutineScope.launch {

                            val bounds = LatLngBounds.builder()
                                .include(start)
                                .include(currentLocation)
                                .build()

                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngBounds(
                                    bounds,
                                    200
                                ),
                                durationMs = 1200
                            )
                        }
                    }
                }
            ) {

                Icon(
                    imageVector =
                        if (isDirectionMode) {
                            Icons.Default.Refresh
                        } else {
                            Icons.Default.Directions
                        },
                    contentDescription = null
                )
            }

            FloatingActionButton(
                onClick = {

                    coroutineScope.launch {

                        if (isDirectionMode && myLocation != null) {

                            val bounds = LatLngBounds.builder()
                                .include(myLocation!!)
                                .include(currentLocation)
                                .build()

                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngBounds(
                                    bounds,
                                    200
                                ),
                                durationMs = 1200
                            )

                        } else {

                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(
                                    currentLocation,
                                    17f
                                ),
                                durationMs = 1000
                            )
                        }
                    }
                }            ) {

                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun LiveMarkerContent(
    imageBitmap: ImageBitmap?,
    displayName: String,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Surface(
            shape = CircleShape,
            shadowElevation = 10.dp
        ) {

            if (imageBitmap != null) {

                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Box(
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(11.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .background(
                        Color(0xFF00E676),
                        CircleShape
                    )
            )
        }
    }
}

fun findNearestPointOnRoute(
    location: LatLng,
    route: List<LatLng>
): Pair<LatLng, Int> {

    var nearestPoint = route.first()
    var nearestIndex = 0
    var minDistance = Double.MAX_VALUE

    for (i in 0 until route.lastIndex) {

        val start = route[i]
        val end = route[i + 1]

        val projected =
            projectPointOnSegment(
                location,
                start,
                end
            )

        val results = FloatArray(1)

        Location.distanceBetween(
            location.latitude,
            location.longitude,
            projected.latitude,
            projected.longitude,
            results
        )

        if (results[0] < minDistance) {

            minDistance = results[0].toDouble()
            nearestPoint = projected
            nearestIndex = i
        }
    }

    return nearestPoint to nearestIndex
}


fun projectPointOnSegment(
    point: LatLng,
    start: LatLng,
    end: LatLng
): LatLng {

    val dx = end.longitude - start.longitude
    val dy = end.latitude - start.latitude

    if (dx == 0.0 && dy == 0.0) {
        return start
    }

    val t =
        (
                (
                        (point.longitude - start.longitude) * dx +
                                (point.latitude - start.latitude) * dy
                        ) /
                        (dx * dx + dy * dy)
                ).coerceIn(0.0, 1.0)

    return LatLng(
        start.latitude + t * dy,
        start.longitude + t * dx
    )
}