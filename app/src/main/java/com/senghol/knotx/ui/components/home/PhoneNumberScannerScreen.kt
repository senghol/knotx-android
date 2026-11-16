package com.senghol.knotx.ui.components.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.senghol.knotx.R
import com.senghol.knotx.ui.viewmodel.HomeViewModel
import com.senghol.knotx.utils.data.CheckedContact
import com.senghol.knotx.utils.data.ContactState

private const val USERNAME_PREFIX = "@"
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun PhoneNumberScannerScreen(
    viewModel: HomeViewModel = viewModel(),
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var manualInput by remember { mutableStateOf(TextFieldValue("")) }

    var usernameMode by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }

    val contactState by viewModel.contactState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Keep reference to the bound camera (for torch control)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isKeyboardOpen) {
        isScanning = !isKeyboardOpen
    }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted
        }

    fun sanitizeUsername(value: TextFieldValue): TextFieldValue {
        val username = value.text
            .replace(USERNAME_PREFIX, "")
            .lowercase()
            .filter { it in 'a'..'z' || it in '0'..'9' }
            .take(32)

        val text = USERNAME_PREFIX + username
        val cursor = value.selection.end
            .coerceIn(USERNAME_PREFIX.length, text.length)

        return TextFieldValue(
            text = text,
            selection = TextRange(cursor)
        )
    }

    BackHandler(enabled = true) {
        onCancel()
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(isScanning) {
        if (isScanning) {
            focusManager.clearFocus()
        }
    }
    LaunchedEffect(isScanning, torchEnabled, hasCameraPermission) {

        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get() ?: return@LaunchedEffect

        if (!isScanning) {
            cameraProvider.unbindAll()
            camera = null
            return@LaunchedEffect
        }

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView?.surfaceProvider
        }

        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128)
                .build()
        )

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
            val mediaImage = proxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)

                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.trim()?.let { value ->
                            when {
                                // Username QR
                                value.matches(Regex("^${Regex.escape(USERNAME_PREFIX)}[A-Za-z0-9_.]{3,32}$")) -> {
                                    viewModel.checkUser(value)
                                    isScanning = false
                                }

                                // Phone number QR
                                value.matches(Regex("^[+0-9\\-\\s()]{8,20}$")) -> {
                                    val number = normalizePhone(value)
                                    viewModel.checkUser(number)
                                    isScanning = false
                                }
                            }
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            } else {
                proxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            camera?.cameraControl?.enableTorch(torchEnabled)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            @kotlin.OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(R.string.add_new_chat)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ───────── Search mode ─────────

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {

                SegmentedButton(
                    selected = !usernameMode,
                    onClick = {
                        usernameMode = false
                        manualInput = TextFieldValue(
                            text = "",
                            selection = TextRange.Zero
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    label = {
                        Text(stringResource(R.string.mode_phone))
                    }
                )

                SegmentedButton(
                    selected = usernameMode,
                    onClick = {
                        usernameMode = true
                        manualInput = TextFieldValue(
                            text = USERNAME_PREFIX,
                            selection = TextRange(USERNAME_PREFIX.length)
                        )
                    },
                    icon = {
                        Text(USERNAME_PREFIX, style = MaterialTheme.typography.titleMedium)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = {
                        Text(stringResource(R.string.mode_username))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ───────── Input ─────────

            OutlinedTextField(
                value = manualInput,

                onValueChange = { newValue ->
                    manualInput = if (usernameMode) {
                        sanitizeUsername(newValue)
                    } else {
                        val filtered = newValue.text.filter {
                            it.isDigit() || it == '+'
                        }
                        newValue.copy(
                            text = filtered,
                            selection = TextRange(
                                newValue.selection.end.coerceAtMost(filtered.length)
                            )
                        )
                    }
                },

                label = {
                    Text(
                        if (usernameMode)
                            stringResource(R.string.label_username)
                        else
                            stringResource(R.string.label_phone_number)
                    )
                },

                placeholder = {
                    Text(
                        if (usernameMode)
                            USERNAME_PREFIX + stringResource(R.string.placeholder_username)
                        else
                            stringResource(R.string.placeholder_phone_number)
                    )
                },

                leadingIcon = {
                    if (usernameMode) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    } else {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    }
                },

                supportingText = {
                    if (usernameMode) {
                        Text(stringResource(R.string.username_supporting_text))
                    }
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = if (usernameMode) KeyboardType.Ascii else KeyboardType.Phone,
                    imeAction = ImeAction.Search
                ),

                keyboardActions = KeyboardActions(
                    onSearch = {
                        val input = manualInput.text

                        val canSearch = if (usernameMode) {
                            input.length > 1
                        } else {
                            input.isNotBlank()
                        }

                        if (canSearch) {
                            viewModel.checkUser(input)
                            focusManager.clearFocus()
                            isScanning = false
                        }
                    }
                ),

                singleLine = true,
                modifier = Modifier.fillMaxWidth(),

                trailingIcon = {
                    val canSearch = if (usernameMode) {
                        manualInput.text.length > 1
                    } else {
                        manualInput.text.isNotBlank()
                    }

                    if (canSearch) {
                        IconButton(
                            onClick = {
                                viewModel.checkUser(manualInput.text)
                                focusManager.clearFocus()
                                isScanning = false
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ───────── Scanner ─────────

            if (isScanning) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp)

                    Text(
                        text = stringResource(R.string.or_divider),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {

                    if (!hasCameraPermission) {
                        Text(
                            stringResource(R.string.camera_permission_required),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    AndroidView(
                        factory = {
                            PreviewView(it).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                previewView = this
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isScanning) 1f else 0f)
                    )

                    ScannerOverlay(
                        isScanning = isScanning,
                        torchEnabled = torchEnabled,
                        onTorchToggle = { torchEnabled = !torchEnabled }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ───────── Result ─────────

            contactState?.let { contact ->

                isScanning = false

                ResultProfileCard(
                    contact = contact,
                    viewModel = viewModel,
                    isOnClosed = { isScanning = true },
                    onDialogClosed = {
                        onCancel()
                        viewModel.clearCurrentContact()
                    }
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// Scanner Overlay with animated line & torch
// ────────────────────────────────────────────────

@Composable
fun ScannerOverlay(
    isScanning: Boolean,
    torchEnabled: Boolean,
    onTorchToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameSize = 260.dp
    val cornerLength = 30.dp
    val strokeWidth = 5.dp

    Box(modifier = modifier.fillMaxSize()) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameSizePx = frameSize.toPx()
            val left = (size.width - frameSizePx) / 2
            val top = (size.height - frameSizePx) / 2
            val right = left + frameSizePx
            val bottom = top + frameSizePx
            val corner = cornerLength.toPx()
            val stroke = strokeWidth.toPx()

            val color = Color.White

            // Top-left
            drawLine(color, Offset(left, top), Offset(left + corner, top), stroke)
            drawLine(color, Offset(left, top), Offset(left, top + corner), stroke)

            // Top-right
            drawLine(color, Offset(right, top), Offset(right - corner, top), stroke)
            drawLine(color, Offset(right, top), Offset(right, top + corner), stroke)

            // Bottom-left
            drawLine(color, Offset(left, bottom), Offset(left + corner, bottom), stroke)
            drawLine(color, Offset(left, bottom), Offset(left, bottom - corner), stroke)

            // Bottom-right
            drawLine(color, Offset(right, bottom), Offset(right - corner, bottom), stroke)
            drawLine(color, Offset(right, bottom), Offset(right, bottom - corner), stroke)
        }

        if (isScanning) {
            val infiniteTransition = rememberInfiniteTransition(label = "scan")
            val offset by infiniteTransition.animateFloat(
                initialValue = -0.4f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (frameSize * offset))
                    .width(frameSize * 0.85f)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Text(
            text = if (isScanning)
                stringResource(R.string.scanner_align_qr)
            else
                stringResource(R.string.scanner_paused),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        )

        FloatingActionButton(
            onClick = onTorchToggle,
            containerColor = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            Icon(
                imageVector = if (torchEnabled)
                    Icons.Default.FlashlightOn
                else
                    Icons.Default.FlashlightOff,
                contentDescription = stringResource(R.string.flash),
                tint = Color.Black
            )
        }
    }
}

@Composable
fun ResultProfileCard(
    contact: CheckedContact,
    isOnClosed: () -> Unit,
    onDialogClosed: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            IconButton(
                onClick = {
                    viewModel.clearCurrentContact()
                    isOnClosed()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear_current_contact),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Profile Avatar ──
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (contact.state == ContactState.Exists)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        contact.profileUri.isNotBlank() -> {
                            // TODO: Replace with real image loading (Coil / Glide / AsyncImage)
                            Text(
                                stringResource(R.string.profile_image_placeholder),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        contact.state == ContactState.Exists && !contact.displayName.isNullOrBlank() -> {
                            Text(
                                text = contact.displayName.first().uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Phone number ──
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                // ── Name or status ──
                val statusText = when (contact.state) {
                    ContactState.Exists ->
                        contact.displayName?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.status_user_found)
                    ContactState.Checking -> stringResource(R.string.status_checking)
                    ContactState.NotFound -> stringResource(R.string.status_not_found)
                    ContactState.INVALID -> stringResource(R.string.status_invalid_phone)
                    ContactState.Error -> stringResource(R.string.status_error)
                    ContactState.AlreadyAdded -> stringResource(R.string.status_already_added)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = when (contact.state) {
                        ContactState.Exists -> Color(0xFF2E7D32)
                        ContactState.NotFound, ContactState.INVALID, ContactState.Error -> Color(0xFFD32F2F)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Single action button ──
                when (contact.state) {
                    ContactState.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    ContactState.Exists -> {
                        Button(
                            onClick = {
                                viewModel.onAdd(contact.phone)
                                onDialogClosed()
                            },
                            modifier = Modifier.fillMaxWidth(0.65f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                stringResource(R.string.action_add),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    ContactState.NotFound, ContactState.INVALID -> {
                        OutlinedButton(
                            onClick = { viewModel.onInvite(contact.phone) },
                            modifier = Modifier.fillMaxWidth(0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                stringResource(R.string.action_invite),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    ContactState.Error -> {
                        Button(
                            onClick = { viewModel.checkUser(contact.phone) },
                            modifier = Modifier.fillMaxWidth(0.65f)
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }

                    ContactState.AlreadyAdded -> {
                        OutlinedButton(
                            onClick = { /* maybe view profile */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(0.65f)
                        ) {
                            Text(stringResource(R.string.action_already_added))
                        }
                    }
                }
            }
        }
    }
}

private fun normalizePhone(raw: String): String {
    return raw.replace(Regex("[^0-9+]"), "")
}