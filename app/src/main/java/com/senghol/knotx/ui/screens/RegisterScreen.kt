package com.senghol.knotx.ui.screens
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.hbb20.CountryCodePicker
import java.security.SecureRandom
import com.google.i18n.phonenumbers.PhoneNumberUtil

import com.senghol.knotx.R

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.datastore.preferences.core.edit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.gson.JsonObject
import com.senghol.knotx.connection.Knotx

import com.senghol.knotx.ui.calling.ConnectionDootaws
import com.senghol.knotx.utils.LocaleHelper
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.objects.PrefKeys
import com.senghol.knotx.utils.objects.PrefsHelper
import com.senghol.knotx.utils.objects.SessionKeys
import com.senghol.knotx.utils.settingStore
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.DootawasDatabase.Companion.getDatabase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.filled.KeyboardArrowDown

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import kotlin.time.Duration.Companion.milliseconds

object OnboardingColors {
    // Light mode — aligned with homeBackground #F5FAF7
    val lightBackgroundTop = Color(0xFFFFFFFF)
    val lightBackgroundBottom = Color(0xFFF5FAF7)
    val lightGlow = Color(0xFFE3F4EC)
    val lightSurface = Color(0xFFFFFFFF)
    val lightText = Color(0xFF172B24)
    val lightSecondaryText = Color(0xFF718078)
    val lightFeatureBackground = Color(0xFFEDF6F0)
    val lightInputBackground = Color(0xFFF7FBF8)
    val lightBorder = Color(0xFFDCEAE1)

    // Dark mode — deep green-black
    val darkBackgroundTop = Color(0xFF101814)
    val darkBackgroundBottom = Color(0xFF151F19)
    val darkGlow = Color(0xFF234A38)
    val darkSurface = Color(0xFF1B2720)
    val darkText = Color(0xFFF0F7F2)
    val darkSecondaryText = Color(0xFFA5B5AA)
    val darkFeatureBackground = Color(0xFF22352A)
    val darkInputBackground = Color(0xFF202D25)
    val darkBorder = Color(0xFF35483B)

    // Shared brand colors — mint/teal
    val purple = Color(0xFF36845D)       // Existing name; now green accent
    val purpleDark = Color(0xFF246847)
    val purpleLight = Color(0xFF8AC9A5)
}
private fun registerPhoneAccount(context: Context) {
    try {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = PhoneAccountHandle(
            ComponentName(context, ConnectionDootaws::class.java),
            "DootawasVoIPAccount"
        )
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "DootawasVoIPAccount")
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .setShortDescription("VoIP Calling")
            .setSupportedUriSchemes(mutableListOf("tel"))
            .build()
        telecomManager.registerPhoneAccount(phoneAccount)
        
    } catch (e: Exception) {
        
        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimization(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val packageName = context.packageName

    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
        }
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    knotx: Knotx
) {
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var countryShortName by remember { mutableStateOf("IN") }
    var registrationMessageRes by remember { mutableStateOf<Int?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val maxPhoneNumberLength = 17
    val phoneUtil = remember { PhoneNumberUtil.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()
    var selectedLanguage by remember {
        mutableStateOf(
            PrefsHelper.getAppLanguage(context)
                ?: Locale.getDefault().language
        )
    }

    val languages = remember {
        listOf(
            Triple("as", "অসমীয়া", "অসমिया"),
            Triple("bn", "বাংলা", "बांग्ला"),
            Triple("brx", "बड़ो", "बोडो"),
            Triple("doi", "डोगरी", "डोगरी"),
            Triple("en", "English", "अंग्रेज़ी"),
            Triple("gu", "ગુજરાતી", "गुजराती"),
            Triple("hi", "हिन्दी", "हिन्दी"),
            Triple("kn", "ಕನ್ನಡ", "कन्नड़"),
            Triple("ks", "کٲشُر", "कश्मीरी"),
            Triple("kok", "कोंकणी", "कोंकणी"),
            Triple("mai", "मैथिली", "मैथिली"),
            Triple("ml", "മലയാളം", "मलयालम"),
            Triple("mni", "ꯃꯩꯇꯩꯂꯣꯟ", "मणिपुरी"),
            Triple("mr", "मराठी", "मराठी"),
            Triple("ne", "नेपाली", "नेपाली"),
            Triple("or", "ଓଡ଼ିଆ", "ओड़िया"),
            Triple("pa", "ਪੰਜਾਬੀ", "पंजाबी"),
            Triple("sa", "संस्कृतम्", "संस्कृत"),
            Triple("sat", "ᱥᱟᱱᱛᱟᱲᱤ", "संथाली"),
            Triple("sd", "سنڌي", "सिंधी"),
            Triple("ta", "தமிழ்", "तमिल"),
            Triple("te", "తెలుగు", "तेलुगु"),
            Triple("ur", "اردو", "उर्दू")
        )
    }
    LaunchedEffect(Unit) {
        val password = withContext(Dispatchers.IO) {
            generateOrRetrievePassword(context.userDataStore)
        }

        SessionKeys.password = password

        val dataStore = context.settingStore
        val prefs = dataStore.data.first()
        val isFirstLaunch = prefs[PrefKeys.IS_FIRST_RUN] != true

        if (isFirstLaunch) {
            withContext(Dispatchers.IO) {
                registerPhoneAccount(context)
                insertNotificationDefaultSettings(context)
                insertChatDefaultSettings(context)
                knotx.createNotificationChannels()
                knotx.setupMediaDirectories()
            }

            dataStore.edit {
                it[PrefKeys.IS_FIRST_RUN] = true
            }
        }
    }

    RequestCriticalPermissions(
        onAllPermissionsGranted = {},
        onPermissionsDenied = {}
    )
//    val scrollState = rememberScrollState()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) {
            scrollState.scrollTo(scrollState.maxValue)   // scrollTo, not animateScrollTo, so it tracks each frame
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        DootawasOnboardingBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val visibleHeight = maxHeight

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .heightIn(min = visibleHeight)   // content is at least as tall as the visible area
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // HEADER
                    RegisterHeader(
                        selectedLanguage = selectedLanguage,
                        languages = languages,
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = it
                        },
                        onLanguageSelected = { code ->
                            if (code != selectedLanguage) {
                                selectedLanguage = code

                                PrefsHelper.setUseSystemLanguage(
                                    context,
                                    false
                                )

                                PrefsHelper.setAppLanguage(
                                    context,
                                    code
                                )

                                LocaleHelper.setLocale(
                                    context,
                                    code
                                )

                                knotx.restartApp()
                            }

                            expanded = false
                        }
                    )

                    // ILLUSTRATION
                    Image(
                        painter = painterResource(
                            id = if (isDark)
                                R.drawable.onboarding_privacy_dark
                            else
                                R.drawable.onboarding_privacy_light
                        ),
                        contentDescription = "Knotx privacy illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Fit
                    )

// MAIN HEADLINE
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) { //todo changeToLan
                        Text(
                            text = "Your chats.",
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) OnboardingColors.darkText
                            else OnboardingColors.lightText
                        )

                        Text(
                            text = "Your privacy.",
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnboardingColors.purple
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    // FEATURES
                    PrivacyFeatures()

                    Spacer(Modifier.height(6.dp))

                    // PHONE CARD
                    PhoneVerificationCard(
                        phoneNumber = phoneNumber,
                        countryCode = countryCode,
                        countryShortName = countryShortName,
                        maxPhoneNumberLength = maxPhoneNumberLength,
                        onPhoneNumberChange = {
                            phoneNumber = it
                        },
                        onCountryChanged = {
                            countryCode = it
                        },
                        registrationMessageRes = registrationMessageRes,
                        onVerify = {

                            scope.launch {
                                try {

                                    val parsedNumber = phoneUtil.parse(
                                        phoneNumber,
                                        countryShortName
                                    )

                                    if (!phoneUtil.isValidNumber(parsedNumber)) {
                                        registrationMessageRes =
                                            R.string.invalid_phone_number
                                        return@launch
                                    }

                                } catch (e: Exception) {
                                    registrationMessageRes =
                                        R.string.invalid_phone_format
                                    return@launch
                                }

                                if (countryCode != "+91") {
                                    registrationMessageRes =
                                        R.string.invalid_country
                                    return@launch
                                }

                                val sessionKey =
                                    SessionKeys.serverSessionId

                                if (sessionKey == null) {

                                    registrationMessageRes =
                                        R.string.no_network

                                } else {

                                    navController.navigate(
                                        "otp_screen?encryptedMessage=dummy&phoneNumber=${countryCode}${phoneNumber}"
                                    ) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },

                    )

                    LaunchedEffect(registrationMessageRes) {
                        if (registrationMessageRes != null) {
                            delay(2000.milliseconds)
                            registrationMessageRes = null
                        }
                    }
                }

            }

        }
    }

//    AppDialogs.InfoDialog(
//        show = showDialog,
//        onDismiss = {
//            showDialog = false
//        }
//    )
}
@Composable
fun DootawasOnboardingBackground() {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            OnboardingColors.darkBackgroundTop,
                            OnboardingColors.darkBackgroundTop,
                            OnboardingColors.darkBackgroundBottom
                        )
                    } else {
                        listOf(
                            OnboardingColors.lightBackgroundTop,
                            OnboardingColors.lightBackgroundTop,
                            Color(0xFFFEFCFF),
                            OnboardingColors.lightBackgroundBottom
                        )
                    }
                )
            )
    ) {
        // Subtle ambient glow; stronger in dark mode
        Box(
            modifier = Modifier
                .size(if (isDark) 380.dp else 420.dp)
                .align(Alignment.TopCenter)
                .offset(y = 220.dp)
                .blur(if (isDark) 120.dp else 140.dp)
                .background(
                    color = if (isDark) {
                        OnboardingColors.darkGlow.copy(alpha = 0.22f)
                    } else {
                        OnboardingColors.lightGlow.copy(alpha = 0.18f)
                    },
                    shape = CircleShape
                )
        )
    }
}
@Composable
fun RegisterHeader(
    selectedLanguage: String,
    languages: List<Triple<String, String, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) OnboardingColors.darkText else OnboardingColors.lightText
    val secondaryTextColor = if (isDark) OnboardingColors.darkSecondaryText else OnboardingColors.lightSecondaryText
    val surfaceColor = if (isDark) OnboardingColors.darkSurface else OnboardingColors.lightSurface
    val borderColor = if (isDark) OnboardingColors.darkBorder else OnboardingColors.lightBorder
    val accentColor = if (isDark) OnboardingColors.purpleLight else OnboardingColors.purpleDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Text(
                    text = "Private. Simple. Yours.",
                    fontSize = 13.sp,
                    color = secondaryTextColor
                )
            }
        }

        Box {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor.copy(alpha = if (isDark) 0.85f else 0.72f),
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))

                    val selected = languages.firstOrNull { it.first == selectedLanguage }

                    Column {
                        Text(
                            text = selected?.second ?: "English",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = selected?.third ?: "English",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )
                    }

                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = secondaryTextColor
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = language.second,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Text(
                                    text = language.third,
                                    fontSize = 12.sp,
                                    color = secondaryTextColor
                                )
                            }
                        },
                        onClick = { onLanguageSelected(language.first) }
                    )
                }
            }
        }
    }
}
@Composable
fun PrivacyFeatures() { //todo changeToLan

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {

        PrivacyFeature(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Lock,
            title = "End-to-end",
            subtitle = "encrypted"
        )

        PrivacyFeature(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Person,
            title = "Username",
            subtitle = "not your number"
        )

        PrivacyFeature(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.VisibilityOff,
            title = "Disappearing",
            subtitle = "messages"
        )
    }
}
@Composable
fun PrivacyFeature(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val isDark = isSystemInDarkTheme()
    val iconBackground = if (isDark) OnboardingColors.darkFeatureBackground else OnboardingColors.lightFeatureBackground
    val iconColor = OnboardingColors.purple
    val titleColor = if (isDark) OnboardingColors.darkText else OnboardingColors.lightText
    val subtitleColor = if (isDark) OnboardingColors.darkSecondaryText else OnboardingColors.lightSecondaryText

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = subtitleColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PhoneVerificationCard(
    phoneNumber: String,
    countryCode: String,
    countryShortName: String, // currently unused, kept so the call site doesn't break
    maxPhoneNumberLength: Int,
    onPhoneNumberChange: (String) -> Unit,
    onCountryChanged: (String) -> Unit,
    onVerify: () -> Unit,
    @StringRes registrationMessageRes: Int? = null,   // new
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF673AB7).copy(alpha = 0.08f),
                spotColor = Color(0xFF673AB7).copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // avoid double shadow
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.82f)
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Text(
                text = "Enter your phone number",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF29242D)
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    if (
                        input.all { it.isDigit() } &&
                        input.length <= maxPhoneNumberLength
                    ) {
                        onPhoneNumberChange(input)
                    }
                },
                placeholder = {
                    Text(
                        text = "Phone number",
                        color = Color(0xFFA49EAA)
                    )
                },
                textStyle = TextStyle(
                    color = Color(0xFF29242D),
                    fontSize = 18.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                leadingIcon = {
                    if (LocalInspectionMode.current) {
                        Text(
                            text = "+91",
                            color = Color(0xFF673AB7),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                CountryCodePicker(ctx).apply {
                                    setTextSize(32)
                                    setContentColor(Color(0xFF1E1C25).toArgb())
                                    setOnCountryChangeListener {
                                        onCountryChanged(selectedCountryCodeWithPlus)
                                    }
                                }
                            },
                            update = {
                                try {
                                    it.setCountryForPhoneCode(
                                        countryCode.removePrefix("+").toInt()
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                },
                trailingIcon = {
                    Text(
                        text = phoneNumber.length.toString(),
                        color = Color(0xFF8A8490),
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFBF9FD),
                    unfocusedContainerColor = Color(0xFFFBF9FD),
                    focusedBorderColor = Color(0xFF673AB7),
                    unfocusedBorderColor = Color(0xFFE2DCE8),
                    cursorColor = Color(0xFF673AB7)
                )
            )

            Spacer(Modifier.height(16.dp))

            // VERIFY BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF673AB7),
                                Color(0xFF7B2FD3)
                            )
                        )
                    )
                    .clickable { onVerify() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.verify),
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(Modifier.height(12.dp))





            registrationMessageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "We'll send an OTP to verify your number",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color(0xFF77727A)
            )
        }
    }
}
@Composable
fun DootawasBottomWave(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
    ) {
        val path = Path().apply {

            moveTo(0f, size.height * 0.55f)

            cubicTo(
                size.width * 0.25f,
                size.height * 0.10f,
                size.width * 0.45f,
                size.height * 0.10f,
                size.width * 0.65f,
                size.height * 0.50f
            )

            cubicTo(
                size.width * 0.80f,
                size.height * 0.80f,
                size.width * 0.92f,
                size.height * 0.75f,
                size.width,
                size.height * 0.45f
            )

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFFE8E0FF).copy(alpha = 0.65f),
                    Color(0xFFF1E8FF).copy(alpha = 0.65f)
                )
            )
        )
    }
}

@Composable
private fun HeaderRow(
    selectedLanguage: String,
    languages: List<Triple<String, String, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App logo",
                modifier = Modifier.clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.widthIn(min = 100.dp, max = 120.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(true) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selected = languages.first { it.first == selectedLanguage }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selected.second,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selected.third,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select language",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.width(140.dp)
            ) {
                languages.forEach { (code, nativeName, hindiName) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedLanguage == code, onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = nativeName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = hindiName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        onClick = { onLanguageSelected(code) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewRegisterScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dootawasDatabase: DootawasDatabase by lazy {
        getDatabase(context)
    }
    val sharedPreferences = context.getSharedPreferences("DakiyaPrefs", Context.MODE_PRIVATE) // ✅ Fixed
    val dummyKnotx = Knotx(context,dootawasDatabase) // ✅ Pass SharedPreferences to Dakiya
    RegisterScreen(
        navController = navController,
        knotx = dummyKnotx
    )
}


suspend fun insertNotificationDefaultSettings(context: Context) {
    context.settingStore.edit { prefs ->
        prefs[SettingPrefKeys.NOTIFICATION_ENABLE_ALL] = true
        prefs[SettingPrefKeys.MESSAGE_SOUND] = true // or false if it's a boolean toggle
        prefs[SettingPrefKeys.MESSAGE_VIBRATION] = true
        prefs[SettingPrefKeys.NOTIFICATION_LED] = "off"

        prefs[SettingPrefKeys.NOTIFICATION_NEW_MESSAGES] = true
        prefs[SettingPrefKeys.NOTIFICATION_GROUP_CHATS] = true
        prefs[SettingPrefKeys.NOTIFICATION_MENTIONS] = true

        prefs[SettingPrefKeys.NOTIFICATION_INCOMING_CALLS] = true
        prefs[SettingPrefKeys.NOTIFICATION_MISSED_CALL_ALERTS] = true

        prefs[SettingPrefKeys.NOTIFICATION_POPUP_PREVIEWS] = true
        prefs[SettingPrefKeys.NOTIFICATION_BADGE_COUNT] = true

        prefs[SettingPrefKeys.NOTIFICATION_DND_ENABLE] = false
        prefs[SettingPrefKeys.NOTIFICATION_DND_CUSTOM_HOURS] = JsonObject().apply {
            addProperty("start_hour", 22)
            addProperty("end_hour", 7)
        }.toString()
        prefs[SettingPrefKeys.NOTIFICATION_DND_ALLOW_EXCEPTIONS] = true
    }
}
suspend fun insertChatDefaultSettings(context: Context) {
    context.settingStore.edit { prefs ->
        // Chat preferences
        prefs[SettingPrefKeys.CHAT_CHAT_THEME] = false
        prefs[SettingPrefKeys.CHAT_CHAT_WALLPAPER] = "default"
        prefs[SettingPrefKeys.CHAT_CHAT_FONT_SIZE] = 17
        prefs[SettingPrefKeys.CHAT_CHAT_ENTER_KEY_BEHAVIOR] = false // or "newline"

        // Message settings
        prefs[SettingPrefKeys.CHAT_MESSAGE_READ_RECEIPTS] = true
        prefs[SettingPrefKeys.CHAT_MESSAGE_TYPING_INDICATORS] = true
        prefs[SettingPrefKeys.CHAT_MESSAGE_BACKUP] = true
        prefs[SettingPrefKeys.CHAT_MESSAGE_AUTO_DELETE] = false

        // Media settings
        prefs[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_PHOTOS] = true
        prefs[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_VIDEOS] = true
        prefs[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_VOICE] = true
        prefs[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_FILES] = true
        prefs[SettingPrefKeys.CHAT_MEDIA_QUALITY] = "high"
        prefs[SettingPrefKeys.CHAT_MEDIA_SAVE_TO_GALLERY] = true

        // Group chat settings
        prefs[SettingPrefKeys.CHAT_GROUP_CHAT_DEFAULT_PERMISSIONS] = "default"
        prefs[SettingPrefKeys.CHAT_GROUP_CHAT_MUTE_ALL] = false

        // Blocked contacts
        prefs[SettingPrefKeys.CHAT_BLOCKED_CONTACTS_VIEW] = true
    }
}

fun verifySuccessMoveToProfile(navController: NavController, phoneNumber: String) {
    navController.navigate("profile_screen/$phoneNumber") {
        popUpTo("register") { inclusive = true } // Clears Register screen from the back stack
    }
}


suspend fun generateOrRetrievePassword(dataStore: DataStore<Preferences>): String {
    val existingPassword = dataStore.data.first()[PrefKeys.USER_PASS]
    dataStore.edit { it.remove(PrefKeys.USER_NUMBER) }
    dataStore.edit { it.remove(PrefKeys.USER_NAME) }
    dataStore.edit { it.remove(PrefKeys.USER_NAME_ID)}
    return if (existingPassword != null) {
        existingPassword
    } else {
        val newPassword = generateSecurePassword()
        dataStore.edit { prefs ->
            prefs[PrefKeys.USER_PASS] = newPassword
        }
        newPassword
    }
}


fun generateSecurePassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%^&*()-_=+"
    val random = SecureRandom()
    return (1..30).map { chars[random.nextInt(chars.length)] }.joinToString("")
}
