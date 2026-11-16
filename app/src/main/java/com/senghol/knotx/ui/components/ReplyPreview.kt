package com.senghol.knotx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senghol.knotx.utils.enums.SandeshType

@Composable
fun ReplyPreview(replyMessage: String, replyToType: SandeshType, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0E0E0)) // Light Gray background
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Replying to: ${replyToType.name}",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.weight(1f)) // Push cancel icon to the right

        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,  // ✅ Corrected parameter
                contentDescription = "Cancel Reply",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

