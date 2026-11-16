package com.senghol.knotx.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun TimerIconsPreview() {
    val icons = listOf(
        "Timer" to Icons.Outlined.Timer,
        "Schedule" to Icons.Outlined.Schedule,
        "AvTimer" to Icons.Outlined.AvTimer,
        "Update" to Icons.Outlined.Update,
        "HourglassTop" to Icons.Outlined.HourglassTop,
        "HourglassEmpty" to Icons.Outlined.HourglassEmpty,
        "AutoDelete" to Icons.Outlined.AutoDelete
    )

    Column(Modifier.padding(16.dp)) {
        icons.forEach { (name, icon) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(name)
            }
        }
    }
}