package com.senghol.knotx.utils.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class SearchFilter(val label: String, val icon: ImageVector) {
    TEXT("Messages", Icons.Default.ChatBubbleOutline),
    IMAGE("Images", Icons.Default.Image),
    VIDEO("Videos", Icons.Default.Videocam),
    AUDIO("Audio", Icons.Default.Audiotrack),
    DOCUMENT("Docs", Icons.Default.Description),
    CONTACT("Contacts", Icons.Default.Person),
    LOCATION("Location", Icons.Default.LocationOn),
    AI("AI", Icons.Default.AutoAwesome)
}