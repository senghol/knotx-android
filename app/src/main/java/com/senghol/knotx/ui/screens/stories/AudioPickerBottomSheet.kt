package com.senghol.knotx.ui.screens.stories
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPickerBottomSheet(
    tracks: List<AudioTrackItem>,
    isLoading: Boolean,
    selectedCategory: String,
    selectedAudio: SelectedStoryAudio?,
    onCategorySelected: (String) -> Unit,
    onAudioSelected: (AudioTrackItem) -> Unit,
    onRemoveAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = App Music, 1 = Device Storage

    val categories = listOf("Trending", "Chill", "Pop", "Beats", "Aesthetic")

    // Launcher for local storage files
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val track = AudioTrackItem(
                id = uri.toString(),
                title = uri.lastPathSegment ?: "Device Audio",
                artist = "Local Device",
                uri = uri,
                durationMs = 60_000L
            )
            onAudioSelected(track)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181C),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row with Remove Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Music",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedAudio != null) {
                    TextButton(onClick = onRemoveAudio) {
                        Text("Remove Audio", color = Color(0xFFFF512F), fontSize = 14.sp)
                    }
                }
            }

            // Tabs Header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFFF512F),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("App Music", color = if (selectedTab == 0) Color(0xFFFF512F) else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Device Audio", color = if (selectedTab == 1) Color(0xFFFF512F) else Color.Gray) }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category, color = if (isSelected) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                selectedContainerColor = Color.White
                            ),
                            shape = CircleShape
                        )
                    }
                }

                // Track Catalog Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color(0xFFFF512F))
                    } else if (tracks.isEmpty()) {
                        Text("No tracks available in this category", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(tracks) { track ->
                                val isCurrentlySelected = selectedAudio?.audioUri == track.uri

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isCurrentlySelected) Color(0xFFFF512F).copy(alpha = 0.15f)
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                        .clickable { onAudioSelected(track) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFF512F).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFFFF512F)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = track.artist,
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isCurrentlySelected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                        contentDescription = if (isCurrentlySelected) "Selected" else "Add",
                                        tint = if (isCurrentlySelected) Color(0xFFFF512F) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Device File Picker Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable { audioPickerLauncher.launch("audio/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFFFF512F),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Pick Audio File from Storage",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Supports MP3, WAV, AAC, M4A",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}