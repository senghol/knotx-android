package com.senghol.knotx.ui.screens


import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// For Uri
import android.net.Uri
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold

import com.senghol.knotx.ui.viewmodel.MediaViewModel

import com.senghol.knotx.utils.enums.SandeshType

import androidx.compose.foundation.lazy.grid.items
import androidx.core.net.toUri

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.senghol.knotx.ui.components.single.AudioBubble
import com.senghol.knotx.ui.components.ContactBubble
import com.senghol.knotx.ui.components.DocumentBubble
import com.senghol.knotx.ui.components.ImageBubble
import com.senghol.knotx.ui.components.LocationBubble
import com.senghol.knotx.ui.components.VideoBubble
import com.senghol.knotx.ui.dialogs.ImagePreviewScreen
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.data.ParsedAudioSandesh
import com.senghol.knotx.utils.data.ParsedContactSandesh
import com.senghol.knotx.utils.data.ParsedDocumentSandesh
import com.senghol.knotx.utils.data.ParsedImageSandesh
import com.senghol.knotx.utils.data.ParsedLocationSandesh
import com.senghol.knotx.utils.data.ParsedVideoSandesh


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    mediaViewModel: MediaViewModel,
    mediaType: SandeshType,
    modifier: Modifier = Modifier,
    navController: NavController
) {

    val mediaLabel = mediaType.getLabel()
    val parsedList by mediaViewModel.parsedSandeshItems.collectAsStateWithLifecycle()
    val selectedMessages = remember { mutableStateSetOf<String>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewIndex by remember { mutableIntStateOf(0) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var hideOnSend by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val theme = LocalDootawasTheme.current
    val dootawasColors = theme.colors
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mediaLabel) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        if (parsedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No ${mediaLabel.lowercase()} found.")
            }
        } else {
            // 📸 Use grid for image/video, list for others
            val isGrid = when (mediaType) {
                SandeshType.IMAGE, SandeshType.REPLY_IMAGE,
                SandeshType.VIDEO, SandeshType.REPLY_VIDEO -> true
                else -> false
            }

            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(parsedList) { parsedItem ->
                        when (parsedItem) {
                            is ParsedImageSandesh -> {
                                val duplicateUriToUse = if (!parsedItem.displayUri.isNullOrBlank()) {
                                    parsedItem.displayUri
                                } else {
                                    
                                    parsedItem.displayUri
                                }

                                ImageBubble(
                                    isSelf = parsedItem.isSelf,
                                    imageUris = parsedItem.imageUris,
                                    blurFileUri = parsedItem.blurUri,
                                    paath = parsedItem.paath,
                                    downloadPercent = parsedItem.downloadPercent,
                                    downloadSize = parsedItem.downloadSize,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    isSelected = selectedMessages.contains(parsedItem.sandeshId),
                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(parsedItem.sandeshId)
                                    },

                                    onImageClick = { uris, clickedIndex ->

                                        // 🚫 Ignore preview during selection mode
                                        if (isSelectionMode) {

                                            if (selectedMessages.contains(parsedItem.sandeshId)) {
                                                selectedMessages.remove(parsedItem.sandeshId)
                                            } else {
                                                selectedMessages.add(parsedItem.sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }

                                        } else {

                                            previewImages = uris
                                            previewIndex = clickedIndex
                                            showImagePreview = true
                                        }
                                    },
                                    onDownloadClick = {
//                                        mediaViewModel.downloadFile(
//                                            sandeshId = parsedItem.sandeshId,
//                                            userNumber = userNumber
//                                        )
                                        
                                    },
                                    onReplyClick = { messageIdReply ->
                                        
//                                        scrollToMessage(messageIdReply)
                                    },
                                    isHighlighted = parsedItem.sandeshId == highlightedMessageId,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,
                                    imageLoader = mediaViewModel.imageLoader,
                                    onCancelClick = {},
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {}
                                )
                            }

                            is ParsedVideoSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem = selectedMessages.contains(sandeshId)
                                val isHighlightedItem = sandeshId == highlightedMessageId

                                VideoBubble(
                                    sandeshId = sandeshId,
                                    isSelf = parsedItem.isSelf,
                                    sandeshType = parsedItem.sandeshType,
                                    paath = parsedItem.paath,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    formattedDate = parsedItem.formattedDate,
                                    blurUri = parsedItem.blurUri,
                                    downloadPercent = parsedItem.downloadPercent,
                                    downloadSize = parsedItem.downloadSize,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,
                                    isSelected = isSelectedItem,
                                    isHighlighted = isHighlightedItem,
                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(sandeshId)
                                    },
                                    onToggleSelection = { videoURI ->

                                        if (isSelectionMode) {

                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }

                                        } else {

                                            videoURI?.let {
                                                selectedVideoUri = it.toUri()
                                                hideOnSend = true
                                            }

                                        }
                                    },

                                    onDownloadClick = {
//                                        sandeshViewModel.downloadFile(
//                                            sandeshId = sandeshId,
//                                            userNumber = userNumber
//                                        )
                                    },
                                    onReplyClick = { id ->
//                                        scrollToMessage(id)
                                    },
                                    onCancelDownload = {
//                                        sandeshViewModel.resetDownloadPercentage(sandeshId)
                                    },
                                    dootawasColors = dootawasColors,
                                    videoUris = parsedItem.videoUris,
                                    previewUris = parsedItem.previewUris,
                                            onReplySlide = {}
                                )
                            }                            else -> Unit
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(parsedList) { parsedItem ->
                        when (parsedItem) {
                            is ParsedAudioSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val savedFileUri = parsedItem.savedFileUri

                                AudioBubble(
                                    sandeshId = sandeshId,
                                    savedFileUri = savedFileUri,
                                    isPlaying = false,
                                    exoPlayer = null,
                                    currentlyPlayingId = null,
                                    onPlayClick = {
//                                        savedFileUri?.let {
//                                            sandeshViewModel.togglePlay(sandeshId, it)
//                                        }
                                    },
                                    downloadPercent = parsedItem.downloadPercent,
                                    downloadSize = parsedItem.downloadSize,
                                    isSelf = parsedItem.isSelf,

                                    sandeshStithi = parsedItem.sandeshStithi,
                                    timeLong = parsedItem.time,

                                    formattedTime = parsedItem.formattedTime,

                                    replyOriginalMessage = parsedItem.replyOriginalMessage,
                                    isSelectionMode = isSelectionMode,

                                    isSelected = selectedMessages.contains(sandeshId),

                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(sandeshId)
                                    },

                                    onToggleSelection = {
                                        if (isSelectionMode) {
                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }
                                        }
                                    },

                                    onDownloadClick = {
//                                        sandeshViewModel.downloadFile(
//                                            sandeshId = sandeshId,
//                                            userNumber = userNumber
//                                        )
                                    },

                                    onReplyClick = { id ->
//                                        scrollToMessage(id)
                                    },
                                    isHighlighted = sandeshId == highlightedMessageId,
                                    duration = parsedItem.duration,
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {}
                                    )
                            }
                            is ParsedDocumentSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem = selectedMessages.contains(sandeshId)
                                val isHighlightedItem = sandeshId == highlightedMessageId

                                DocumentBubble(
                                    sandeshId = sandeshId,
                                    isSelf = parsedItem.isSelf,
                                    sandeshType = parsedItem.sandeshType,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    formattedDate = parsedItem.formattedDate,
                                    savedFileUri = parsedItem.savedFileUri,
                                    duplicateUri = parsedItem.duplicateUri,
                                    mimeType = parsedItem.mimeType,
                                    downloadPercent = parsedItem.downloadPercent,
                                    downloadSize = parsedItem.downloadSize,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,

                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelectedItem,
                                    isHighlighted = isHighlightedItem,

                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(sandeshId)
                                    },

                                    onToggleSelection = { docURI, mimeType ->

                                        if (isSelectionMode) {

                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }

                                        } else {

                                            docURI?.let { openDocument(context, it, mimeType) }

                                        }
                                    },

                                    onDownloadClick = {
//                                        sandeshViewModel.downloadFile(
//                                            sandeshId = sandeshId,
//                                            userNumber = userNumber
//                                        )
                                    },

                                    onReplyClick = { id ->
//                                        scrollToMessage(id)
                                    },

                                    onCancelDownload = {
//                                        sandeshViewModel.resetDownloadPercentage(sandeshId)
                                    },
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {}
                                )
                            }
                            is ParsedContactSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem = selectedMessages.contains(sandeshId)
                                val isHighlightedItem = sandeshId == highlightedMessageId

                                ContactBubble(
                                    sandeshId = sandeshId,
                                    isSelf = parsedItem.isSelf,
                                    sandeshType = parsedItem.sandeshType,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    formattedDate = parsedItem.formattedDate,
                                    contacts = parsedItem.contacts,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,

                                    isSelected = isSelectedItem,
                                    isHighlighted = isHighlightedItem,

                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(sandeshId)
                                    },

                                    onToggleSelection = {
                                        if (isSelectionMode) {

                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }
                                        }
                                    },

                                    onReplyClick = { id ->
//                                        scrollToMessage(id)
                                    }, dootawasColors = dootawasColors,
                                    onReplySlide = {}
                                )
                            }

                            is ParsedLocationSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isHighlightedItem = sandeshId == highlightedMessageId
                                val isSelectedItem = selectedMessages.contains(sandeshId)
                                LocationBubble(
                                    sandeshId = sandeshId,
                                    isSelf = parsedItem.isSelf,
                                    sandeshType = parsedItem.sandeshType,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    formattedDate = parsedItem.formattedDate,
                                    savedFileUri = parsedItem.savedFileUri,
                                    duplicateUri = parsedItem.duplicateUri,
                                    latitude = parsedItem.latitude,
                                    longitude = parsedItem.longitude,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,

                                    isSelectionMode = isSelectionMode,
                                    isHighlighted = isHighlightedItem,

                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(sandeshId)
                                    },

                                    onToggleSelection = { geoUri ->

                                        if (isSelectionMode) {

                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }

                                        } else {

                                            geoUri?.let {
                                                val intent = Intent(Intent.ACTION_VIEW, it)
                                                intent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(intent)
                                            }

                                        }
                                    },

                                    onDownloadClick = {
//                                        sandeshViewModel.downloadFile(
//                                            sandeshId = sandeshId,
//                                            userNumber = userNumber
//                                        )
                                    },

                                    onReplyClick = { id ->
//                                        scrollToMessage(id)
                                    },
                                    isLive = parsedItem.isLive,
                                    liveUntil = 0,
                                    isStopped = true,
                                    currentTime = 0,
                                    onStopLiveLocation = {  }
                                    , dootawasColors = dootawasColors,
                                    isSelected = isSelectedItem,
                                    onReplySlide = {}
                                )
                            }
                            else -> {}
                        }


                    }
                }
            }
        }
    }
    if (showImagePreview) {
        
        
        ImagePreviewScreen(
            imageList = previewImages,
            startIndex = previewIndex,
            onDismiss = { showImagePreview = false }
        )
    }
}
