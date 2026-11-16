package com.senghol.knotx.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.ui.theme.md_theme_dark_surface

data class GroupDootawasColors(
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
    val replyBackground: Color,
    val replyText: Color,

    val userHeaderBackground: Color,
    val userHeaderText: Color,

    val chatScreenBackground: Color,
    val chatScreenWallpaper: Color
)

val LightGroupDootawasColors = GroupDootawasColors(
    rightMessageText = Color(0xFFFFFFFF), //this changed
    leftMessageText = Color(0xFF000000),
    leftTimeBackGround = Color(0xFFFFFFFF),
    rightTimeBackGround = Color(0xFF363636),
    leftTimeText = Color(0xB34D4B4B),
    rightTimeText = Color(0xFFD2D2D2), //this changed
    leftMessageBorderColor = Color(0xFFFDEFD7),
    rightMessageBorderColor = Color(0xFF3E8F41), //this changed
    rightMessageBackground = Color(0xFF2155F3), //this changed
    leftMessageBackground = Color(0xFFFFFFFF),
    replyBackground = Color(0xFFFFFFFF),
    userHeaderBackground = Color(0xFFFFFFFF),
    userHeaderText = Color.Black,
    chatScreenBackground = Color(0xFFF5F2EB),//that
    chatScreenWallpaper = Color(0x414F4F4F),
    replyText = Color(0xFF1C1C1C)
)
val DarkGroupDootawasColors = GroupDootawasColors(

    rightMessageText = Color(0xFFF6F8FA),
    leftMessageText = Color(0xFFF6F8FA),
    leftTimeBackGround = Color(0xFF134D37),
    rightTimeBackGround = Color(0xFF2C302C),
    leftTimeText = Color(0xFFA5A8AA),
    rightTimeText = Color(0xFFA5A8AA),
    leftMessageBorderColor = Color(0xFF104A34),
    rightMessageBorderColor = Color(0xFF292D29),
    leftMessageBackground = Color(0xFF134D37),
    rightMessageBackground = Color(0xFF2C302C),
    replyBackground = Color(0xFF292D29),
    userHeaderBackground = Color(0xFF1E201E), //that
    userHeaderText = Color(0xFFF6F8FA),
    chatScreenBackground = Color(0xFF121212),
    chatScreenWallpaper = Color(0x2E636363),
    replyText = Color(0xFFFFFFFF)
)
data class StoredGroupColors(
    val rightMessageText: Long, val rightMessageBackground: Long,
    val leftMessageBackground: Long, val leftMessageText: Long,
    val leftTimeBackGround: Long, val rightTimeBackGround: Long,
    val leftTimeText: Long, val rightTimeText: Long,
    val leftMessageBorderColor: Long, val rightMessageBorderColor: Long,
    val replyBackground: Long, val replyText: Long,
    val userHeaderBackground: Long,
    val userHeaderText: Long,
    val chatScreenBackground: Long, val chatScreenWallpaper: Long
)

private fun Color.l() = toArgb().toLong()
private fun Long.c() = Color(toInt())

fun GroupDootawasColors.toStored() = StoredGroupColors(
    rightMessageText.l(), rightMessageBackground.l(),
    leftMessageBackground.l(), leftMessageText.l(),
    leftTimeBackGround.l(), rightTimeBackGround.l(),
    leftTimeText.l(), rightTimeText.l(),
    leftMessageBorderColor.l(), rightMessageBorderColor.l(),
    replyBackground.l(), replyText.l(),
    userHeaderBackground.l(), userHeaderText.l(),
    chatScreenBackground.l(), chatScreenWallpaper.l()
)

fun StoredGroupColors.toCompose() = GroupDootawasColors(
    rightMessageText.c(), rightMessageBackground.c(),
    leftMessageBackground.c(), leftMessageText.c(),
    leftTimeBackGround.c(), rightTimeBackGround.c(),
    leftTimeText.c(), rightTimeText.c(),
    leftMessageBorderColor.c(), rightMessageBorderColor.c(),
    replyBackground.c(), replyText.c(),
    userHeaderBackground.c(), userHeaderText.c(),
    chatScreenBackground.c(), chatScreenWallpaper.c()
)

fun GroupDootawasColors.toChatColors(base: DootawasColors) = base.copy(
    rightMessageText = rightMessageText,
    rightMessageBackground = rightMessageBackground,
    leftMessageBackground = leftMessageBackground,
    leftMessageText = leftMessageText,
    leftTimeBackGround = leftTimeBackGround,
    rightTimeBackGround = rightTimeBackGround,
    leftTimeText = leftTimeText,
    rightTimeText = rightTimeText,
    leftMessageBorderColor = leftMessageBorderColor,
    rightMessageBorderColor = rightMessageBorderColor,
    replyBackground = replyBackground,
    replyText = replyText,
    userHeaderBackground = userHeaderBackground,
    userHeaderText = userHeaderText,
    chatScreenBackground = chatScreenBackground,
    chatScreenWallpaper = chatScreenWallpaper
)

data class StoredGroupThemeBundle(
    val light: StoredGroupColors,
    val dark: StoredGroupColors,
    val borderMode: Int = BorderMode.NONE.ordinal,
    val useModernStyle: Boolean = false
)

fun defaultGroupBundle() = StoredGroupThemeBundle(
    light = LightGroupDootawasColors.toStored(),
    dark = DarkGroupDootawasColors.toStored()
)

fun DootawasColors.toGroupColors() = GroupDootawasColors(
    rightMessageText = rightMessageText,
    rightMessageBackground = rightMessageBackground,
    leftMessageBackground = leftMessageBackground,
    leftMessageText = leftMessageText,
    leftTimeBackGround = leftTimeBackGround,
    rightTimeBackGround = rightTimeBackGround,
    leftTimeText = leftTimeText,
    rightTimeText = rightTimeText,
    leftMessageBorderColor = leftMessageBorderColor,
    rightMessageBorderColor = rightMessageBorderColor,
    replyBackground = replyBackground,
    replyText = replyText,
    userHeaderBackground = userHeaderBackground,
    userHeaderText = userHeaderText,
    chatScreenBackground = chatScreenBackground,
    chatScreenWallpaper = chatScreenWallpaper
)