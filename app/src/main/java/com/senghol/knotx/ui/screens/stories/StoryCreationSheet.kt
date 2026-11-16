package com.senghol.knotx.ui.screens.stories

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.senghol.knotx.ui.components.CameraScreen
import com.senghol.knotx.ui.components.PermissionHandler
import java.io.File

/**
 * Entry point for creating a story. Bottom sheet with three ways in:
 * camera, gallery, or a blank text story.
 *
 * Wire it into your existing `showStoriesScreen` flag like this:
 *
 * if (showStoriesScreen) {
 *     StoryCreationSheet(
 *         onDismiss = { showStoriesScreen = false },
 *         onBackgroundReady = { background ->
 *             showStoriesScreen = false
 *             pendingStoryBackground = background   // hold in a remembered state
 *             showStoryEditor = true                // then show StoryEditorScreen
 *         }
 *     )
 * }
 *
 * Requires a FileProvider entry in your manifest for the camera capture to work:
 *
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="${applicationId}.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/file_paths" />
 * </provider>
 *
 * res/xml/file_paths.xml:
 * <paths><cache-path name="story_captures" path="." /></paths>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreationSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit, // <--- Add callback here
    onBackgroundReady: (StoryBackground) -> Unit
) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
            onBackgroundReady(StoryBackground.Media(uri, isVideo))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add to Story",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            CreationOptionRow(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                subtitle = "Take a new photo or video",
                onClick = {
                    onCameraClick() // <--- Open camera outside bottom sheet
                }
            )
            Spacer(Modifier.height(12.dp))
            CreationOptionRow(
                icon = Icons.Default.Image,
                label = "Gallery",
                subtitle = "Choose a photo or video",
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            CreationOptionRow(
                icon = Icons.Default.TextFields,
                label = "Text",
                subtitle = "Share a status with just words",
                onClick = { onBackgroundReady(StoryBackground.Gradient(defaultGradients.first())) }
            )
        }
    }
}

@Composable
private fun CreationOptionRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
