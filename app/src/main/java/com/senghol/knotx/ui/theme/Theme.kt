package com.senghol.knotx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.runtime.*

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.utils.enums.SettingPrefKeys


private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)


private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)


data class DootawasColors(
    // existing...
    val dialogBackground: Color,
    val dialogHeader: Color,
    val dialogText: Color,
    val searchBarBackground: Color,
    val searchBarText: Color,
    val searchBarFocusedIndicator: Color,
    val searchBarUnfocusedIndicator: Color,
    val rightMessageText: Color,
    val rightMessageBackground: Color,
    val leftMessageBackground: Color,
    val leftMessageText: Color,
    val leftTimeBackGround: Color,
    val rightTimeBackGround: Color,
    val leftTimeText: Color,
    val rightTimeText: Color,
    val leftMessageBorderColor: Color,
    val rightMessageBorderColor: Color,
    val glassColor: Color,
    val replyBackground: Color,
    val replyText: Color,

    // 🆕 NEW

    // USER HEADER
    val userHeaderBackground: Color,
    val userHeaderText: Color,

    val userHeaderIcon: Color,

    // HOME
    val homeBackground: Color,
    val homeChatBackground: Color,
    val homeText: Color,

    // HOME HEADER
    val homeHeaderBackground: Color,
    val homeHeaderSelectedText: Color,
    val homeHeaderSelectedBackground: Color,
    val homeHeaderUnSelectedText: Color,
    val homeHeaderIcon: Color,

    // BUTTONS
    val buttonBackground: Color,
    val buttonText: Color,

    // WALLPAPER
    val wallpaperBackground: Color, //todo delete this
    val wallpaperOverlay: Color,
    val unreadBadgeColor: Color,
    val lastMessageColor: Color,
    val chatScreenBackground: Color,
    val chatScreenWallpaper: Color
)

val LightDootawasColors = DootawasColors(
    dialogBackground = Color.White,
    dialogHeader = md_theme_light_primary,
    dialogText = md_theme_light_onSurface,
    searchBarBackground = Color(0xFFDDE7E1), //this changed
    searchBarText = Color(0xFF000000),
    searchBarFocusedIndicator = Color(0xFF0E0E0E), //this changed
    searchBarUnfocusedIndicator = md_theme_light_outline,
    rightMessageText = Color(0xFFFFFFFF), //this changed
    leftMessageText = Color(0xFF000000),
    leftTimeBackGround = Color(0xFFFFFFFF),
    rightTimeBackGround = Color(0xFF363636),
    leftTimeText = Color(0xB34D4B4B),
    rightTimeText = Color(0xFFD2D2D2), //this changed
    leftMessageBorderColor = Color(0xFFFFFFFF),
    rightMessageBorderColor = Color(0xFFD9E8D6), //this changed
    rightMessageBackground = Color(0xFF2155F3), //this changed
    leftMessageBackground = Color(0xFFFFFFFF),
    glassColor = Color(0xD9313131),
    replyBackground = Color(0xFFFFFFFF),
    userHeaderBackground = Color(0xFFFFFFFF),
    userHeaderText = Color.Black,
    userHeaderIcon = Color(0xFFEB5B00),
    homeBackground = Color(0xFFF5FAF7),
    homeChatBackground = Color(0x00F5FAF7),
    homeText = Color.Black,
    homeHeaderBackground     = Color(0xFFF6FAF8),
    homeHeaderSelectedText   = Color(0xFFD81B60),
    homeHeaderSelectedBackground   = Color(0xFFF6FAF8),
    homeHeaderUnSelectedText = Color(0xFF3F3A3A),
    homeHeaderIcon           = Color(0xFF50357A),
    buttonBackground = Color(0xFFFFFFFF),
    buttonText = Color(0xFFD81B60),
    wallpaperBackground = Color.White,
    wallpaperOverlay = Color.Transparent,
    unreadBadgeColor = Color(0xFFD81B60),
    lastMessageColor = Color(0xFF4D463A),
    chatScreenBackground = Color(0xFFF5F2EB),//that
    chatScreenWallpaper = Color(0x414F4F4F),
    replyText = Color(0xFF1C1C1C)
)

val DarkDootawasColors = DootawasColors(
    dialogBackground = md_theme_dark_surface,
    dialogHeader = md_theme_dark_primary,
    dialogText = md_theme_dark_onSurface,
    searchBarBackground = Color(0xFF1E201E),
    searchBarText = md_theme_dark_onSurfaceVariant,
    searchBarFocusedIndicator = Color(0xFFFFB951),
    searchBarUnfocusedIndicator = Color(0xFFCAA227),
    rightMessageText = Color(0xFFF6F8FA),
    leftMessageText = Color(0xFFF6F8FA),
    leftTimeBackGround = Color(0xFF134D37),
    rightTimeBackGround = Color(0xFF2C302C),
    leftTimeText = Color(0xFFB0B0B0),
    rightTimeText = Color(0xFFA5A8AA),
    leftMessageBorderColor = Color(0xFF104A34),
    rightMessageBorderColor = Color(0xFF292D29),
    leftMessageBackground = Color(0xFF134D37),
    rightMessageBackground = Color(0xFF2C302C),
    glassColor = Color(0xFF4B4B4B),
    replyBackground = Color(0xFF292D29),
    userHeaderBackground = Color(0xFF1E201E), //that
    userHeaderText = Color(0xFFF6F8FA),
    userHeaderIcon = Color(0xFFEB5B00),
    homeBackground = Color(0xFF131313),
    homeChatBackground = Color(0x00121212), //that
    homeText = Color(0xE8E8E8E8),
    homeHeaderBackground     = Color(0xFF111111), //this changed
    homeHeaderSelectedText   = Color(0xFF21C61A),
    homeHeaderSelectedBackground   = Color(0xFF111111), //that
    homeHeaderUnSelectedText = Color(0xFFE0E0E0),//that
    homeHeaderIcon           = Color(0xFFF9FAFB),
    buttonBackground = Color(0xFF1E201E),
    buttonText = Color.White,
    wallpaperBackground = Color.Black,
    wallpaperOverlay = Color(0x66000000),
    unreadBadgeColor = Color(0xFFD81B60),
    lastMessageColor = Color(0xFFA5A8AA),
    chatScreenBackground = Color(0xFF121212),
    chatScreenWallpaper = Color(0x2E636363),
    replyText = Color(0xFFFFFFFF)
)


data class StoredThemeBundle(
    val light: StoredDootawasColors,
    val dark: StoredDootawasColors,
    val borderMode: Int = BorderMode.SEMI.ordinal,
    val useModernStyle: Boolean = true
)

data class StoredDootawasColors(
    // existing...
    val dialogBackground: Long,
    val dialogHeader: Long,
    val dialogText: Long,
    val searchBarBackground: Long,
    val searchBarText: Long,
    val searchBarFocusedIndicator: Long,
    val searchBarUnfocusedIndicator: Long,
    val rightMessageText: Long,
    val rightMessageBackground: Long,
    val leftMessageBackground: Long,
    val leftMessageText: Long,
    val leftTimeBackGround: Long,
    val rightTimeBackGround: Long,
    val leftTimeText: Long,
    val rightTimeText: Long,
    val leftMessageBorderColor: Long,
    val rightMessageBorderColor: Long,
    val glassColor: Long,
    val replyBackground: Long,
    val replyText: Long,

    // 🆕 NEW

    val userHeaderBackground: Long,
    val userHeaderText: Long,

    val userHeaderIcon: Long,

    val homeBackground: Long,
    val homeChatBackground: Long,
    val homeText: Long,

    val homeHeaderBackground: Long,
    val homeHeaderSelectedText: Long,
    val homeHeaderSelectedBackground: Long,
    val homeHeaderUnSelectedText: Long,
    val homeHeaderIcon: Long,

    val buttonBackground: Long,
    val buttonText: Long,

    val wallpaperBackground: Long, //todo delete this
    val wallpaperOverlay: Long, //todo delete this

    val unreadBadgeColor: Long,
    val lastMessageColor: Long,
    val chatScreenBackground: Long,
    val chatScreenWallpaper: Long
)
fun DootawasColors.toStored() = StoredDootawasColors(
    // existing...
    dialogBackground = dialogBackground.toArgb().toLong(),
    dialogHeader = dialogHeader.toArgb().toLong(),
    dialogText = dialogText.toArgb().toLong(),
    searchBarBackground = searchBarBackground.toArgb().toLong(),
    searchBarText = searchBarText.toArgb().toLong(),
    searchBarFocusedIndicator = searchBarFocusedIndicator.toArgb().toLong(),
    searchBarUnfocusedIndicator = searchBarUnfocusedIndicator.toArgb().toLong(),
    rightMessageText = rightMessageText.toArgb().toLong(),
    rightMessageBackground = rightMessageBackground.toArgb().toLong(),
    leftMessageBackground = leftMessageBackground.toArgb().toLong(),
    leftMessageText = leftMessageText.toArgb().toLong(),
    leftTimeBackGround = leftTimeBackGround.toArgb().toLong(),
    rightTimeBackGround = rightTimeBackGround.toArgb().toLong(),
    leftTimeText = leftTimeText.toArgb().toLong(),
    rightTimeText = rightTimeText.toArgb().toLong(),
    leftMessageBorderColor = leftMessageBorderColor.toArgb().toLong(),
    rightMessageBorderColor = rightMessageBorderColor.toArgb().toLong(),
    glassColor = glassColor.toArgb().toLong(),
    replyBackground = replyBackground.toArgb().toLong(),

    // 🆕 NEW
    userHeaderBackground = userHeaderBackground.toArgb().toLong(),
    userHeaderText = userHeaderText.toArgb().toLong(),

    userHeaderIcon = userHeaderIcon.toArgb().toLong(),

    homeBackground = homeBackground.toArgb().toLong(),
    homeChatBackground = homeChatBackground.toArgb().toLong(),
    homeText = homeText.toArgb().toLong(),

    homeHeaderBackground = homeHeaderBackground.toArgb().toLong(),
    homeHeaderSelectedText = homeHeaderSelectedText.toArgb().toLong(),
    homeHeaderSelectedBackground = homeHeaderSelectedBackground.toArgb().toLong(),
    homeHeaderUnSelectedText = homeHeaderUnSelectedText.toArgb().toLong(),
    homeHeaderIcon = homeHeaderIcon.toArgb().toLong(),

    buttonBackground = buttonBackground.toArgb().toLong(),
    buttonText = buttonText.toArgb().toLong(),

    wallpaperBackground = wallpaperBackground.toArgb().toLong(),
    wallpaperOverlay = wallpaperOverlay.toArgb().toLong(),
    unreadBadgeColor = unreadBadgeColor.toArgb().toLong(),
    lastMessageColor = lastMessageColor.toArgb().toLong(),
    chatScreenBackground = chatScreenBackground.toArgb().toLong(),
    chatScreenWallpaper = chatScreenWallpaper.toArgb().toLong(),
    replyText = replyText.toArgb().toLong()
)
fun StoredDootawasColors.toCompose() = DootawasColors(
    dialogBackground = Color(dialogBackground.toInt()),
    dialogHeader = Color(dialogHeader.toInt()),
    dialogText = Color(dialogText.toInt()),
    searchBarBackground = Color(searchBarBackground.toInt()),
    searchBarText = Color(searchBarText.toInt()),
    searchBarFocusedIndicator = Color(searchBarFocusedIndicator.toInt()),
    searchBarUnfocusedIndicator = Color(searchBarUnfocusedIndicator.toInt()),
    rightMessageText = Color(rightMessageText.toInt()),
    rightMessageBackground = Color(rightMessageBackground.toInt()),
    leftMessageBackground = Color(leftMessageBackground.toInt()),
    leftMessageText = Color(leftMessageText.toInt()),
    leftTimeBackGround = Color(leftTimeBackGround.toInt()),
    rightTimeBackGround = Color(rightTimeBackGround.toInt()),
    leftTimeText = Color(leftTimeText.toInt()),
    rightTimeText = Color(rightTimeText.toInt()),
    leftMessageBorderColor = Color(leftMessageBorderColor.toInt()),
    rightMessageBorderColor = Color(rightMessageBorderColor.toInt()),
    glassColor = Color(glassColor.toInt()),
    replyBackground = Color(replyBackground.toInt()),

    userHeaderBackground = Color(userHeaderBackground.toInt()),
    userHeaderText = Color(userHeaderText.toInt()),

    userHeaderIcon = Color(userHeaderIcon.toInt()),

    homeBackground = Color(homeBackground.toInt()),
    homeChatBackground = Color(homeChatBackground.toInt()),
    homeText = Color(homeText.toInt()),

    homeHeaderBackground = Color(homeHeaderBackground.toInt()),
    homeHeaderSelectedText = Color(homeHeaderSelectedText.toInt()),
    homeHeaderSelectedBackground = Color(homeHeaderSelectedBackground.toInt()),
    homeHeaderUnSelectedText = Color(homeHeaderUnSelectedText.toInt()),
    homeHeaderIcon = Color(homeHeaderIcon.toInt()),
    buttonBackground = Color(buttonBackground.toInt()),
    buttonText = Color(buttonText.toInt()),

    wallpaperBackground = Color(wallpaperBackground.toInt()),
    wallpaperOverlay = Color(wallpaperOverlay.toInt()),
    unreadBadgeColor = Color(unreadBadgeColor.toInt()),
    lastMessageColor = Color(lastMessageColor.toInt()),
    chatScreenBackground = Color(chatScreenBackground.toInt()),
    chatScreenWallpaper = Color(chatScreenWallpaper.toInt()),
    replyText = Color(replyText.toInt())
)

val LocalDootawasTheme = staticCompositionLocalOf {
    DootawasThemeState(
        colors = LightDootawasColors,
        borderMode = BorderMode.NONE,
        useModernStyle = false,
        groupColors = LightGroupDootawasColors,
        groupBorderMode = BorderMode.NONE,
        groupUseModernStyle = false
    )
}
data class DootawasThemeState(
    val colors: DootawasColors,
    val borderMode: BorderMode,
    val useModernStyle: Boolean,

    // GROUP
    val groupColors: GroupDootawasColors,
    val groupBorderMode: BorderMode,
    val groupUseModernStyle: Boolean
)
@Composable
fun DootawasTheme(
    viewModel: Knotx,
    content: @Composable () -> Unit
) {
    val settings by viewModel.settingsFlow.collectAsState()
    val themeValue = (settings[SettingPrefKeys.APP_THEME] as? String) ?: "system"

    val isDark = when (themeValue.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val bundle = viewModel.themeBundle.collectAsState().value ?: return
    val groupBundle = viewModel.groupThemeBundle.collectAsState().value ?: defaultGroupBundle()

    val dootawasTheme = DootawasThemeState(
        colors = if (isDark) bundle.dark.toCompose() else bundle.light.toCompose(),
        borderMode = BorderMode.entries[bundle.borderMode],
        useModernStyle = bundle.useModernStyle,
        groupColors = if (isDark) groupBundle.dark.toCompose() else groupBundle.light.toCompose(),
        groupBorderMode = BorderMode.entries.getOrElse(groupBundle.borderMode) { BorderMode.SEMI },
        groupUseModernStyle = groupBundle.useModernStyle
    )

    val colors = if (!isDark) LightColors else DarkColors

    CompositionLocalProvider(
        LocalDootawasTheme provides dootawasTheme
    ) {
        MaterialTheme(
            colorScheme = colors,
            content = content
        )
    }
}