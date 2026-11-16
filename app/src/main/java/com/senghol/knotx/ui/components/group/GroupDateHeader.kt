package com.senghol.knotx.ui.components.group

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.GroupDootawasColors
import com.senghol.knotx.utils.data.ParsedDateHeaderSandesh

@Composable
fun GroupDateHeader(
    key: String,
    shortText: String,
    detailedText: String,
    modifier: Modifier = Modifier,
    groupDootawasColors: GroupDootawasColors
) {
    var expanded by remember(key) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (expanded) detailedText else shortText,
            color = groupDootawasColors.userHeaderText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            modifier = Modifier
                .shadow(1.dp, RoundedCornerShape(8.dp))
                .background(groupDootawasColors.userHeaderBackground, RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}