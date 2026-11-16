package com.senghol.knotx.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.senghol.knotx.connection.Knotx
import kotlinx.coroutines.launch
import java.io.File
import com.senghol.knotx.utils.objects.PrefsHelper
import kotlinx.coroutines.flow.map
import androidx.core.net.toUri
import com.senghol.knotx.R
import com.senghol.knotx.ui.screens.DootawasOnboardingBackground
import com.senghol.knotx.utils.data.UserNameIdCheckState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.senghol.knotx.editor.CropState
import com.senghol.knotx.editor.EditorCropScreen
import com.senghol.knotx.editor.loadOrientedBitmap
import com.senghol.knotx.editor.renderCrop

import java.io.FileOutputStream
val badWords = listOf(
    // English
    "fuck", "shit", "bitch", "ass", "bastard", "dick", "pussy",
    "cunt", "whore", "slut", "asshole", "motherfucker", "cock", "twat", "prick",

    // Short forms
    "fck", "wtf", "stfu", "gtfo", "bs",

    // Indian short abusive forms (IMPORTANT)
    "bc", "mc", "bkl", "bsdk", "tmkc", "mkc", "ckmbb",

    // Hindi / Hinglish roots
    "chutiya", "chuti", "gand", "lund", "randi", "kamina", "kutta", "lauda", "land",
    "harami", "nalayak", "bewakoof", "pagal", "bhan ke laude", "madarchod",
    "bhosdike", "bhosda", "lawde", "laude", "bhenchod", "chodu", "gaand",
    "tatti", "haramzada", "raand", "mutthal", "jhant", "kaminey","behanchod","bahanchod","lodu","betichod",

    // Variations / masking
    "fck", "f*ck", "sh*t", "b!tch"
)


fun containsBadWord(userInput: String): Boolean {

    val cleanedInput = userInput
        .lowercase()
        .replace(Regex("[\\W_]"), "")

    val words = userInput.lowercase().split(Regex("\\s+"))

    for (word in badWords) {

        val pattern = word.lowercase()
            .map { Regex.escape(it.toString()) + "+" }
            .joinToString("")

        val regex = Regex(pattern)

        // 1. Check aggressive cleaned match (for bypass)
        if (regex.containsMatchIn(cleanedInput)) {
            return true
        }

        // 2. Check proper word match (avoid false positives)
        for (w in words) {
            if (regex.matches(w)) {
                return true
            }
        }
    }

    return false
}


@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.isFocused) {
                scope.launch {
                    delay(300) // wait for keyboard animation
                    requester.bringIntoView()
                }
            }
        }
}
// ─────────────────────────────────────────────────────────────
// ProfileSetupScreen — full-screen replacement for ProfileSetupDialog.
// Adds a live username-id editor alongside the existing display-name field,
// both wired to the same Knotx flows/check-state used in AccountFieldsSection.
// ─────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────
// ProfileSetupScreen — full-screen replacement for ProfileSetupDialog.
// Adds a live username-id editor alongside the existing display-name field,
// both wired to the same Knotx flows/check-state used in AccountFieldsSection.
// ─────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────
// ProfileSetupScreen — full-screen replacement for ProfileSetupDialog.
// Adds a live username-id editor alongside the existing display-name field,
// both wired to the same Knotx flows/check-state used in AccountFieldsSection.
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    phoneNumber: String,
    knotx: Knotx
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val usernameSectionRequester = remember { BringIntoViewRequester() }
    val aboutSectionRequester = remember { BringIntoViewRequester() }

    var usernameSectionSize by remember { mutableStateOf(IntSize.Zero) }
    var aboutSectionSize by remember { mutableStateOf(IntSize.Zero) }
    val extraMarginPx = with(LocalDensity.current) { 40.dp.toPx() } // change 40.dp for more or less gap

// ── Test mode: only for this specific phone number ──
    val isTestMode = remember(phoneNumber) {
        phoneNumber.filter { it.isDigit() }.endsWith("8512015149")
    }

    fun revealAfterKeyboard(
        requester: BringIntoViewRequester,
        sizeProvider: () -> IntSize
    ) {
        scope.launch {
            delay(350) // let the keyboard animation finish
            val size = sizeProvider()
            requester.bringIntoView(
                Rect(0f, 0f, size.width.toFloat(), size.height + extraMarginPx)
            )
        }
    }

    // ?? Display name state
    // ── Display name state — live from Knotx ──
    val userName by knotx.userNameKhud.collectAsState(initial = null)
    var displayName by remember { mutableStateOf(TextFieldValue("")) }
    var displayNameTouched by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    // ── About state — live from Knotx, same pattern ──
    val about by knotx.aboutKhud.collectAsState(initial = null)
    var tempAbout by remember { mutableStateOf(TextFieldValue("")) }
    var aboutTouched by remember { mutableStateOf(false) }

    // ── Username id state — mirrors AccountFieldsSection's pattern ──
    val userNameId by knotx.userNameIdKhud.collectAsState(initial = null)
    val userNameIdCheckState by knotx.userNameIdCheckState.collectAsState()
    val userNameIdCooldownUntil by knotx.userNameIdCooldownUntil.collectAsState(initial = null)
    val isUserNameIdOnCooldown = userNameIdCooldownUntil != null && userNameIdCooldownUntil!! > System.currentTimeMillis()
    var tempUserNameId by remember {
        mutableStateOf(TextFieldValue(text = "@", selection = TextRange(1)))
    }
    var userNameIdTouched by remember { mutableStateOf(false) } // becomes true once the user types past "@"

    var isUpdated by remember { mutableStateOf(false) }
    var showThemeSetup by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val profileImageFlow = remember(knotx.profileImagePathFlow) {
        knotx.profileImagePathFlow.map { path ->
            if (path.isNullOrBlank()) null else path.toUri()
        }
    }
    val profileImageUri by profileImageFlow.collectAsState(initial = null)

    LaunchedEffect(knotx) {
        knotx.getSelfProfile()
    }

    LaunchedEffect(userName) {
        if (!displayNameTouched && !userName.isNullOrBlank()) {
            displayName = TextFieldValue(text = userName!!, selection = TextRange(userName!!.length))
        }
    }

    LaunchedEffect(about) {
        if (!aboutTouched && !about.isNullOrBlank()) {
            tempAbout = TextFieldValue(text = about!!, selection = TextRange(about!!.length))
        }
    }

    // sync tempUserNameId if a value ever arrives from the flow (e.g. resumed setup)
    LaunchedEffect(userNameId) {
        if (!userNameIdTouched && !userNameId.isNullOrBlank()) {
            tempUserNameId = TextFieldValue(text = userNameId!!, selection = TextRange(userNameId!!.length))
        }
    }

    // debounced live availability check, same shape as AccountFieldsSection
    LaunchedEffect(tempUserNameId.text) {
        if (isTestMode) return@LaunchedEffect // test mode: skip availability + bad word checks
        if (isUserNameIdOnCooldown) return@LaunchedEffect // field is read-only in this state — nothing to check
        // Skip if this change came from the initial store sync, not the user typing —
        // don't hit the server checking availability of a username we already own.
        if (!userNameIdTouched) return@LaunchedEffect

        val candidate = tempUserNameId.text
        val body = candidate.removePrefix("@")

        if (body.isBlank()) {
            knotx.resetUserNameIdCheck(candidate)
            return@LaunchedEffect
        }

        // User edited the field but landed back on their existing username — already known-good.
        if (body == userNameId?.removePrefix("@")) {
            knotx.resetUserNameIdCheck(candidate)
            return@LaunchedEffect
        }

        if (containsBadWord(body)) {
            Toast.makeText(context, "Improper words are not allowed", Toast.LENGTH_SHORT).show()
            knotx.setUserNameIdInvalid("Username not allowed", candidate)
            return@LaunchedEffect
        }

        delay(400L)
        knotx.checkUserNameIdAvailability(candidate)
    }

//    if (showThemeSetup) { //uncomment this if u need to show onbardig ediotr and comment bottom launched effect
//
//        val isDark = isSystemInDarkTheme()
//        OnboardingThemeEditorScreen(
//            viewModel = knotx,
//            isDark = isDark,
//            onComplete = {
//                navController.navigate("home") {
//                    popUpTo(0) { inclusive = true }
//                    launchSingleTop = true
//                }
//            }
//        )
//        return
//    }
    LaunchedEffect(showThemeSetup) {
        if (showThemeSetup) {
            navController.navigate("home") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> pickedUri = uri }

    LaunchedEffect(pickedUri) {
        val uri = pickedUri ?: return@LaunchedEffect
        sourceBitmap = withContext(Dispatchers.IO) { loadOrientedBitmap(context, uri, 2048) }
    }

    fun closeCrop() {
        sourceBitmap = null
        pickedUri = null
    }

    sourceBitmap?.let { bmp ->
        Dialog(
            onDismissRequest = { closeCrop() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                EditorCropScreen(
                    source = bmp,
                    initial = CropState(),
                    squareOnly = true,   // profile = 1:1 only
                    onClose = { closeCrop() },
                    onApply = { state ->
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                val out = renderCrop(bmp, state)
                                File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg").also { f ->
                                    FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                }
                            }
                            knotx.setProfileImageToStore(Uri.fromFile(file).toString())
                            isUpdated = true
                            closeCrop()
                        }
                    }
                )
            }
        }
    }

    val isUserNameIdAvailable = userNameIdCheckState is UserNameIdCheckState.Available
    val userNameIdBody = tempUserNameId.text.removePrefix("@")
    val isTestBypass = isTestMode && userNameIdBody.isNotBlank()
    val canFinish = displayName.text.isNotBlank() &&
            nameError == null &&
            !isSubmitting &&
            (isTestBypass ||
                    isUserNameIdOnCooldown ||
                    userNameIdBody == userNameId?.removePrefix("@") ||
                    (userNameIdBody.isNotBlank() && isUserNameIdAvailable))
    Box(modifier = Modifier.fillMaxSize()) {
        DootawasOnboardingBackground()
//        DootawasBottomWave(
//            modifier = Modifier.align(Alignment.BottomCenter)
//        )

        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp
                        )
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = canFinish,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.75f)
                            ),
                            onClick = {
                                val name = displayName.text.trim()
                                val hasAbuse = !isTestMode && containsBadWord(name)
                                nameError = when {
                                    name.isBlank() -> "Name can't be empty"
                                    hasAbuse -> "Name contains inappropriate words"
                                    else -> null
                                }


                                if (nameError != null) return@Button

                                val isUserNameIdUnchanged =
                                    tempUserNameId.text.removePrefix("@") == userNameId?.removePrefix(
                                        "@"
                                    )
                                if (!isTestBypass && !isUserNameIdOnCooldown && !isUserNameIdUnchanged && !isUserNameIdAvailable) return@Button

                                isSubmitting = true
                                scope.launch {
                                    knotx.updateUserName(name)

                                    val aboutText = tempAbout.text.trim()
                                    if (aboutText.isNotBlank()) {
                                        knotx.updateAbout(aboutText)
                                    }

                                    val isUserNameIdUnchanged =
                                        tempUserNameId.text.removePrefix("@") == userNameId?.removePrefix(
                                            "@"
                                        )
                                    if (!isUserNameIdOnCooldown && !isUserNameIdUnchanged) {
                                        val applied = knotx.updateUserNameId(tempUserNameId.text)
                                        if (!applied && isTestMode) {
                                            // Test mode: server may reject duplicates (e.g. "test"), so store it on the device anyway
                                            knotx.setUserNameIdLocalOnly(tempUserNameId.text)
                                        }
                                        if (!applied && !isTestMode) {
                                            isSubmitting = false
                                            Toast.makeText(
                                                context,
                                                "Couldn't set that username — please try another",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }
                                    }

                                    if (isUpdated) {
                                        knotx.setProfileImage(profileImageUri?.toString())
                                    }

                                    PrefsHelper.setIsRegistered(context, true)
                                    knotx.readContacts(true)
                                    knotx.createAndAddQuickTipsContact()
                                    knotx.createAndAddArikshAi(true)
                                    isSubmitting = false
                                    showThemeSetup = true
                                }
                            },
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    stringResource(R.string.finish),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.setup_profile),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.add_profile_picture),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(32.dp))

                // ── Avatar ──
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                CircleShape
                            )
                            .clickable {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        profileImageUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Icon(
                            Icons.Default.Person,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-6).dp, y = (-4).dp)
                            .size(40.dp)
                            .clickable {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (profileImageUri != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .size(34.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            shadowElevation = 6.dp,
                            onClick = {
                                knotx.setProfileImageToStore(null)
                                knotx.setProfileImage(null)
                                isUpdated = true
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(if (WindowInsets.isImeVisible) 16.dp else 36.dp))

                // ?? Display name field ??
                // ── Display name field ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.name),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (nameError != null)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayNameTouched = true
                                displayName = it
                                nameError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) revealAfterKeyboard(usernameSectionRequester) { usernameSectionSize }
                                },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            placeholder = { Text(stringResource(R.string.enter_name)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Badge,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            textStyle = TextStyle(fontSize = 17.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent
                            )
                        )
                    }

                    nameError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Username id field ──
                // ?? Username id field ??
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { usernameSectionSize = it }
                        .bringIntoViewRequester(usernameSectionRequester),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.username),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    val checkState =
                        userNameIdCheckState // local val — smart-cast doesn't work on delegated properties directly

                    val isUserNameIdError = checkState is UserNameIdCheckState.Taken ||
                            checkState is UserNameIdCheckState.Invalid ||
                            checkState is UserNameIdCheckState.Error

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isUserNameIdError)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        OutlinedTextField(
                            enabled = !isUserNameIdOnCooldown,
                            value = tempUserNameId,
                            onValueChange = { newValue ->
                                userNameIdTouched = true
                                val text = newValue.text
                                tempUserNameId = if (!text.startsWith("@")) {
                                    val fixed = "@$text"
                                    newValue.copy(
                                        text = fixed,
                                        selection = TextRange(
                                            (newValue.selection.start + 1).coerceIn(
                                                1,
                                                fixed.length
                                            )
                                        )
                                    )
                                } else {
                                    newValue
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) revealAfterKeyboard(aboutSectionRequester) { aboutSectionSize }
                                },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            placeholder = { Text(stringResource(R.string.choose_username)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AlternateEmail,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                when (checkState) {
                                    is UserNameIdCheckState.Checking ->
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )

                                    is UserNameIdCheckState.Available ->
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Available",
                                            tint = Color(0xFF2E7D32)
                                        )

                                    is UserNameIdCheckState.Taken ->
                                        Icon(
                                            Icons.Default.Cancel,
                                            contentDescription = "Taken",
                                            tint = MaterialTheme.colorScheme.error
                                        )

                                    is UserNameIdCheckState.Invalid ->
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = checkState.reason,
                                            tint = MaterialTheme.colorScheme.error
                                        )

                                    is UserNameIdCheckState.Error ->
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = checkState.message,
                                            tint = MaterialTheme.colorScheme.error
                                        )

                                    UserNameIdCheckState.Idle -> {}
                                }
                            },
                            textStyle = TextStyle(fontSize = 17.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent
                            )
                        )
                    }

                    val helperText = when {
                        isTestBypass -> "Test mode: username checks skipped"
                        isUserNameIdOnCooldown -> {
                            val daysLeft =
                                ((userNameIdCooldownUntil!! - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)) + 1
                            "You already have this username — it can be changed again in $daysLeft day${if (daysLeft != 1L) "s" else ""}"
                        }

                        checkState is UserNameIdCheckState.Taken -> stringResource(R.string.username_taken)
                        checkState is UserNameIdCheckState.Invalid -> checkState.reason
                        checkState is UserNameIdCheckState.Error -> checkState.message
                        checkState is UserNameIdCheckState.Available -> stringResource(R.string.username_available)
                        else -> "This is how people can find and message you"
                    }
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (userNameIdCheckState is UserNameIdCheckState.Available)
                            Color(0xFF2E7D32)
                        else if (isUserNameIdError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── About field (optional — local only until Finish, same as name/username) ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { aboutSectionSize = it }
                        .bringIntoViewRequester(aboutSectionRequester),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        OutlinedTextField(
                            value = tempAbout,
                            onValueChange = {
                                aboutTouched = true
                                tempAbout = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) revealAfterKeyboard(aboutSectionRequester) { aboutSectionSize }
                                },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            placeholder = { Text(stringResource(R.string.enter_about)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            textStyle = TextStyle(fontSize = 17.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent
                            )
                        )
                    }

                    Text(
                        text = "Optional — tell people a little about yourself",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(Modifier.height(96.dp))
            }
        }
    }
}