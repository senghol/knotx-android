package com.senghol.knotx.ui.screens


import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.JsonObject
import com.senghol.knotx.R
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.ui.viewmodel.OtpViewModel
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.JsonKeys
import kotlinx.coroutines.delay
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun OtpScreen(
    navController: NavController,
    knotx: Knotx,
    phoneNumber: String
) {
    val viewModel: OtpViewModel = viewModel()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val screenText = if (isDark) OnboardingColors.darkText else OnboardingColors.lightText
    val secondaryText = if (isDark) OnboardingColors.darkSecondaryText else OnboardingColors.lightSecondaryText
    val headerSurface = if (isDark) OnboardingColors.purpleLight else OnboardingColors.purpleDark
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var otpValue by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(viewModel.getRemainingSeconds()) }
    var isOtpResent by remember { mutableStateOf(false) }
    val autoOtp by viewModel.autoOtp.collectAsState()
    viewModel.startTimerIfNeeded()

    // -------------------------
    // INIT
    // -------------------------
    LaunchedEffect(Unit) {
        viewModel.startObserving(knotx, context, phoneNumber, "IN")
    }

    LaunchedEffect(Unit) {
        viewModel.startSmsObserver(context)
    }
    LaunchedEffect(phoneNumber) {
        viewModel.askNewOTP(knotx, phoneNumber)
    }

    // -------------------------
    // TIMER LOOP
    // -------------------------
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000.milliseconds)
            countdown = viewModel.getRemainingSeconds()

            if (countdown == 0) {
                knotx.loginDootawas()
                navController.popBackStack()
                break
            }
        }
    }

    // -------------------------
    // UI EVENTS

    LaunchedEffect(isSuccess) {
        if (isSuccess) {

            verifySuccessMoveToProfile(navController, phoneNumber)
        }
    }

    // -------------------------
    // UI
    // -------------------------
    Box(modifier = Modifier.fillMaxSize()) {
        DootawasOnboardingBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, end = 1.dp)
        ) {

            Spacer(modifier = Modifier.width(8.dp)) // Push selector to right
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = headerSurface,
                modifier = Modifier.padding(start = 1.dp)
            )

        }

            // 🔝 Flexible top spacing (no fixed dp)
            Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = phoneNumber,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = screenText
        )

        Spacer(modifier = Modifier.height(12.dp))

        // -------------------------
        // TIMER + RESEND
        // -------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "$countdown s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (countdown <= 40 && !isOtpResent) {
                Text(
                    text = "Resend OTP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = screenText,
                    modifier = Modifier.clickable {

                        val otpJson = JsonObject().apply {
                            addProperty(JsonKeys.USER_NUMBER, phoneNumber)
                            addProperty(JsonKeys.DATA_TYPE, DataType.SEND_OTP.value)
                        }

                        knotx.sendRegisterMessage(otpJson)
                        isOtpResent = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        StylishOtpInput(
            otpValue = otpValue,
            onOtpChange = { otpValue = it },
            isSubmitting = isLoading,
            onVerifyClick = {
                if (otpValue.length != 6) return@StylishOtpInput

                viewModel.submitOtp(
                    knotx,
                    phoneNumber,
                    otpValue,
                    context
                )
            },
            autoOtpValue = autoOtp,
        )

        uiMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }


        // 🔻 Flexible bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
    }
    }

}
@Composable
fun StylishOtpInput(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    autoOtpValue: String? = null,
    maxOtpLength: Int = 6,
    isSubmitting: Boolean,
    onVerifyClick: () -> Unit
) {
    val context = LocalContext.current

    val enterOtpText = stringResource(R.string.enter_complete_otp)
    // Tera magic animation – bilkul waise hi
    LaunchedEffect(autoOtpValue) {
        if (!autoOtpValue.isNullOrEmpty() && autoOtpValue.length == maxOtpLength) {
            onOtpChange("")
            autoOtpValue.forEachIndexed { index, c ->
                delay((90L * index).milliseconds)
                onOtpChange(autoOtpValue.take(index + 1))
            }
        }
    }

    LaunchedEffect(otpValue) {
        if (otpValue.length == maxOtpLength) {
            delay(400.milliseconds)
            onVerifyClick()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        Spacer(modifier = Modifier.height(12.dp))

        BasicTextField(
            value = otpValue,
            onValueChange = {
                if (it.length <= maxOtpLength && it.all { ch -> ch.isDigit() }) {
                    onOtpChange(it)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            decorationBox = {
                // YE HAI ASLI JUGAAD – 100% safe, no wrapping ever
                ResponsiveOtpBoxes(
                    otpValue = otpValue,
                    maxOtpLength = maxOtpLength
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (otpValue.length == maxOtpLength) {
                    onVerifyClick()
                } else {
                    Toast.makeText(context, enterOtpText, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.verify_otp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ResponsiveOtpBoxes(otpValue: String, maxOtpLength: Int) {
    Layout(
        content = {
            repeat(maxOtpLength) { index ->
                val char = otpValue.getOrNull(index)?.toString() ?: ""
                val isFocused = index == otpValue.length

                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (scaleIn(tween(100), 0.8f) + fadeIn(tween(100))) togetherWith
                                (scaleOut(tween(100)) + fadeOut(tween(100)))
                    },
                    label = "digit_anim"
                ) { digit ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFocused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val spacingPx = 10.dp.roundToPx()
        val minSize = 44.dp.roundToPx()
        val maxSize = 56.dp.roundToPx()

        val totalSpacing = spacingPx * (maxOtpLength - 1)
        val available = constraints.maxWidth - totalSpacing
        var boxSize = available / maxOtpLength

        boxSize = boxSize.coerceIn(minSize, maxSize)

        val placeables = measurables.map {
            it.measure(Constraints.fixed(boxSize, boxSize))
        }

        val totalWidth = boxSize * maxOtpLength + totalSpacing
        val height = boxSize

        layout(constraints.maxWidth, height + 20.dp.roundToPx()) { // extra padding safe rakha
            var xPos = (constraints.maxWidth - totalWidth) / 2
            placeables.forEach { placeable ->
                placeable.placeRelative(xPos, (height - placeable.height) / 2)
                xPos += boxSize + spacingPx
            }
        }
    }
}