package com.senghol.knotx.ui.components

import android.graphics.BitmapFactory
import com.senghol.knotx.data.database.StoryEntity
import com.senghol.knotx.data.database.StoryMediaType

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.senghol.knotx.data.database.StoryCommentEntity
import com.senghol.knotx.data.database.StoryLikeEntity
import com.senghol.knotx.data.database.StoryViewEntity
import com.senghol.knotx.data.database.StoryVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun StoryPlayerScreen(
    stories: List<StoryEntity>,
    initialIndex: Int = 0,
    currentUserId: String,
    observeStoryComments: (storyId: String) -> Flow<List<StoryCommentEntity>>,
    observeStoryViewers: (storyId: String) -> Flow<List<StoryViewEntity>>,
    observeStoryLikers: (storyId: String) -> Flow<List<StoryLikeEntity>>,
    onStoryViewed: (storyId: String,ownerId: String) -> Unit,
    onToggleLike: (storyId: String) -> Unit,
    onAddComment: (storyId: String, text: String) -> Unit,
    onDeleteComment: (storyId:String, commentId: String) -> Unit, // ADD THIS
    onUpdateVisibility: (storyId: String, visibility: StoryVisibility) -> Unit, // owner-only, changes who sees the STORY ? separate from comment-section privacy
    onSetCommentsPrivate: (storyId: String, isPrivate: Boolean) -> Unit, // owner-only, whole-section toggle ? not per-comment
    onSetCommentsPrivateGlobally: ((isPrivate: Boolean) -> Unit)? = null, // optional: sets the owner's default for all future stories, not just this one
    onDeleteStory: (storyId: String) -> Unit,
    onClose: () -> Unit
) {
    if (stories.isEmpty()) return

    BackHandler(onBack = onClose)

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, stories.lastIndex),
        pageCount = { stories.size }
    )

    var showComments by remember { mutableStateOf(false) }
    var showViewers by remember { mutableStateOf(false) }
    var showLikers by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentStory = stories[pagerState.currentPage]
    val comments by remember(currentStory.id) { observeStoryComments(currentStory.id) }
        .collectAsState(initial = emptyList())

    val viewers by remember(currentStory.id) { observeStoryViewers(currentStory.id) }
        .collectAsState(initial = emptyList())

    val likers by remember(currentStory.id) { observeStoryLikers(currentStory.id) }
        .collectAsState(initial = emptyList())
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { stories[it].id }
        ) { page ->
            val story = stories[page]
            val isCurrentPage = pagerState.currentPage == page
            val isSettled = pagerState.settledPage == page
            val currentStory = stories[pagerState.currentPage]
            StoryPlayerItem(
                story = story,
                isCurrentPage = isCurrentPage,
                isSettled = isSettled,
                storyCount = stories.size,
                storyIndex = page,
                currentUserId = currentUserId,
                forcePaused = showComments || showViewers || showLikers,
                onUpdateVisibility = { visibility -> onUpdateVisibility(story.id, visibility) },
                onStoryViewed = { onStoryViewed(story.id,story.ownerId) },
                onToggleLike = { onToggleLike(story.id) },
                onAddComment = { text -> onAddComment(story.id, text) },
                onDeleteStory = { onDeleteStory(story.id) },
                onCommentsClick = { showComments = true },
                onViewersClick = { showViewers = true },
                onLikersClick = { showLikers = true },
                onNextStory = {
                    scope.launch {
                        val next = if (pagerState.currentPage < stories.lastIndex) {
                            pagerState.currentPage + 1
                        } else {
                            0 // loop back to the first story instead of closing
                        }
                        pagerState.animateScrollToPage(next)
                    }
                },
                onPreviousStory = {
                    if (pagerState.currentPage > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onClose = onClose
            )
        }
    }

    if (showComments) {
        StoryCommentsBottomSheet(
            story = currentStory,
            comments = comments,
            currentUserId = currentUserId,
            onAddComment = { text -> onAddComment(currentStory.id, text) },
            onDeleteComment = { commentId -> onDeleteComment(currentStory.id, commentId) }, // ADD THIS
            onSetCommentsPrivate = { isPrivate -> onSetCommentsPrivate(currentStory.id, isPrivate) },
            onSetCommentsPrivateGlobally = onSetCommentsPrivateGlobally,
            onDismiss = { showComments = false }
        )
    }

    if (showViewers && currentStory.ownerId == currentUserId) {
        StoryViewersBottomSheet(
            viewers = viewers,
            onDismiss = { showViewers = false }
        )
    }

    if (showLikers) {
        StoryLikersBottomSheet(
            likers = likers,
            onDismiss = { showLikers = false }
        )
    }
}
@Composable
private fun StoryViewersBottomSheet(
    viewers: List<StoryViewEntity>,
    onDismiss: () -> Unit
) {
    @kotlin.OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${viewers.size} views",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (viewers.isEmpty()) {
                Text(
                    text = "No views yet",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(viewers, key = { "${it.storyId}_${it.viewerId}" }) { viewer ->
                        Text(
                            text = viewer.viewerId,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryLikersBottomSheet(
    likers: List<StoryLikeEntity>,
    onDismiss: () -> Unit
) {
    @kotlin.OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${likers.size} likes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (likers.isEmpty()) {
                Text(
                    text = "No likes yet",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(likers, key = { "${it.storyId}_${it.userId}" }) { liker ->
                        Text(
                            text = liker.userId,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun StoryProgressSegments(
    count: Int,
    currentIndex: Int,
    currentProgress: Float,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) { index ->
            val progress = when {
                index < currentIndex -> 1f
                index == currentIndex -> currentProgress
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Color.White)
                )
            }
        }
    }
}
@OptIn(UnstableApi::class)
@Composable
private fun StoryPlayerItem(
    story: StoryEntity,
    isCurrentPage: Boolean,
    isSettled: Boolean,
    storyCount: Int,
    storyIndex: Int,
    currentUserId: String,
    forcePaused: Boolean,
    onUpdateVisibility: (StoryVisibility) -> Unit,
    onStoryViewed: () -> Unit,
    onToggleLike: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteStory: () -> Unit,
    onCommentsClick: () -> Unit,
    onViewersClick: () -> Unit,
    onLikersClick: () -> Unit,
    onNextStory: () -> Unit,
    onPreviousStory: () -> Unit,
    onClose: () -> Unit,

    ) {
    val context = LocalContext.current
    var isPaused by remember { mutableStateOf(false) } // hold-to-pause (press and hold)
    var isManuallyPaused by remember { mutableStateOf(false) } // single-tap toggle pause/resume
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val progressAnim = remember { Animatable(0f) }

    // Optimistic local like state ? updates the heart instantly on tap instead
    // of waiting for story.isLikedByMe to come back through the data layer.
    var localIsLiked by remember(story.id) { mutableStateOf(story.isLikedByMe) }
    LaunchedEffect(story.isLikedByMe) { localIsLiked = story.isLikedByMe }

    // Double-tap heart: a purely visual pop-and-fade, independent of the real
    // like state — this plays every time you double-tap, even if already liked,
    // since it's just feedback that the tap registered.
    var showLikeHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    LaunchedEffect(showLikeHeart) {
        if (showLikeHeart) {
            heartScale.snapTo(0.3f)
            heartAlpha.snapTo(1f)
            launch {
                heartScale.animateTo(1.2f, tween(200, easing = LinearOutSlowInEasing))
                heartScale.animateTo(1f, tween(100))
            }
            delay(600)
            heartAlpha.animateTo(0f, tween(250))
            showLikeHeart = false
        }
    }

    // A new story always starts playing, regardless of how the last one ended.
    LaunchedEffect(story.id, isCurrentPage) {
        if (isCurrentPage) isManuallyPaused = false
    }
    val durationMs = story.durationMs ?: if (story.mediaType == StoryMediaType.VIDEO) 15_000L else 5_000L


    // ExoPlayer Setup for Video Stories
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    var videoLoadFailed by remember(story.id) { mutableStateOf(false) }

    if (story.mediaType == StoryMediaType.VIDEO) {
        DisposableEffect(story.id, isCurrentPage) {
            if (isCurrentPage) {
                val source = story.localPath ?: story.mediaUrl
                if (source.isNullOrBlank()) {
                    videoLoadFailed = true
                } else {
                    val player = ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(source)))
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                videoLoadFailed = true
                            }
                        })
                        prepare()
                        playWhenReady = true
                        repeatMode = Player.REPEAT_MODE_OFF
                    }
                    exoPlayer = player
                }
            }

            onDispose {
                exoPlayer?.release()
                exoPlayer = null
            }
        }
    }

    // Mark as viewed only once the swipe has settled on this page, not mid-drag.
    LaunchedEffect(story.id, isSettled) {
        if (isSettled) {
            onStoryViewed()
        }
    }

    val effectivelyPaused = isPaused || forcePaused || showDeleteConfirm || isManuallyPaused

    // Timer & Progress Animation Driver
    LaunchedEffect(story.id, isCurrentPage, effectivelyPaused) {
        if (!isCurrentPage) {
            progressAnim.snapTo(0f)
            return@LaunchedEffect
        }

        if (effectivelyPaused) {
            exoPlayer?.pause()
        } else {
            exoPlayer?.play()
            val remainingTime = ((1f - progressAnim.value) * durationMs).toLong()
            if (remainingTime > 0) {
                progressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingTime.toInt(),
                        easing = LinearEasing
                    )
                )
                if (progressAnim.value >= 1f) {
                    onNextStory()
                }
            } else {
                onNextStory() // already complete ? don't get stuck
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onDoubleTap = {
                        if (!localIsLiked) { // double-tap only LIKES, never removes a like
                            localIsLiked = true
                            onToggleLike()
                        }
                        showLikeHeart = true
                    },
                    onTap = { offset ->
                        val screenWidth = size.width
                        when {
                            offset.x < screenWidth * 0.3f -> onPreviousStory()
                            offset.x > screenWidth * 0.7f -> onNextStory()
                            else -> isManuallyPaused = !isManuallyPaused // middle third: single tap pause/resume
                        }
                    }
                )
            }
    ) {
        // --- 1. Story Media Renderer ---
        when (story.mediaType) {
            StoryMediaType.IMAGE -> {
                StoryImage(localPath = story.localPath, remoteUrl = story.mediaUrl)
            }
            StoryMediaType.VIDEO -> {
                when {
                    videoLoadFailed -> Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Video unavailable on this device", color = Color.White.copy(alpha = 0.6f))
                    }
                    exoPlayer != null -> AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { view -> view.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) // still preparing
                }
            }
            StoryMediaType.TEXT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.mediaUrl ?: "",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }

        // Brief play/pause icon flash so a manual tap has visible feedback
        AnimatedVisibility(
            visible = isManuallyPaused,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Paused", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        // Double-tap-to-like heart pop
        if (showLikeHeart) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .graphicsLayer(
                        scaleX = heartScale.value,
                        scaleY = heartScale.value,
                        alpha = heartAlpha.value
                    )
            )
        }

        // --- 2. Story Header & Progress Overlay ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            StoryProgressSegments(
                count = storyCount,
                currentIndex = storyIndex,
                currentProgress = progressAnim.value
            )

            Spacer(modifier = Modifier.height(10.dp))

            // User Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.ownerId,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(story.createdAt),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                if (story.ownerId == currentUserId) {
                    var showVisibilityMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showVisibilityMenu = true }) {
                            Icon(
                                imageVector = when (story.visibility) {
                                    StoryVisibility.EVERYONE -> Icons.Default.Public
                                    StoryVisibility.CUSTOM -> Icons.Default.Group
                                    StoryVisibility.PRIVATE -> Icons.Default.Lock
                                },
                                contentDescription = "Change who can see this",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(expanded = showVisibilityMenu, onDismissRequest = { showVisibilityMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Everyone") },
                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                                onClick = { onUpdateVisibility(StoryVisibility.EVERYONE); showVisibilityMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Contact List") },
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                onClick = { onUpdateVisibility(StoryVisibility.CUSTOM); showVisibilityMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Only me") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = { onUpdateVisibility(StoryVisibility.PRIVATE); showVisibilityMenu = false }
                            )
                        }
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // --- 3. Bottom Actions & Comments ---
// --- 3. Bottom Actions ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💬 ${story.commentCount}",
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onCommentsClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (story.ownerId == currentUserId) {
                Text(
                    text = "👁 ${story.viewCount}",
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onViewersClick)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "❤️ ${story.likeCount}",
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onLikersClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = {
                localIsLiked = !localIsLiked // tap toggles instantly, tap again to unlike
                onToggleLike()
            }) {
                Icon(
                    imageVector = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (localIsLiked) "Unlike" else "Like",
                    tint = if (localIsLiked) Color.Red else Color.White
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete this story?") },
                text = { Text("This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDeleteStory()
                    }) { Text("Delete", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}
/**
 * decodeFileDescriptor() is a distinct native entry point from decodeFile()/
 * decodeStream() — on some MediaTek devices it's reported to bypass a broken
 * vendor Skia codec-creation path that both of the other two hit (see the
 * "Failed to create image decoder with message 'unimplemented'" + DcfDecoder
 * trace this is working around). UNVERIFIED against real MediaTek hardware —
 * if this also fails, the failure is in the device's native Skia build itself,
 * below anything reachable from a public Android API, and would need either a
 * pure-Kotlin/JVM image decoder bundled into the app (bypassing native Skia
 * entirely) or acceptance that this specific device can't render local images
 * through the standard platform APIs.
 */
private fun decodeViaFileDescriptor(path: String): ImageBitmap? {
    ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, bounds)

        var sampleSize = 1
        val maxDimension = 2048
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        // Re-open — a FileDescriptor can't be "rewound" the way a fresh decode call needs.
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd2 ->
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            return BitmapFactory.decodeFileDescriptor(pfd2.fileDescriptor, null, decodeOptions)?.asImageBitmap()
        }
    }
}
/**
 * MediaTek devices intercept BitmapFactory.decodeStream() with an OEM DRM-check
 * hook (MTKDcfDecoderManagerImpl) that calls InputStream.mark()/reset() without
 * checking markSupported() first. Coil's AsyncImage decodes through exactly that
 * stream-based path, so on affected devices a local story image silently fails
 * to load ("mark/reset not supported"), leaving a blank frame — no crash, no
 * visible error, just nothing rendered. decodeFile() takes a different native
 * path that this hook doesn't intercept, so local files bypass Coil entirely
 * here; remote URLs (nothing local to decode from) still go through AsyncImage
 * as before, since there's no stream-vs-file choice to make for those anyway.
 */
/** localPath is stored as a file:// URI string (see StoryEntity), but java.io.File
 * doesn't parse URI schemes — File("file:///data/...").exists() is always false even
 * when the real file is sitting right there. This strips the scheme so File()/
 * ParcelFileDescriptor actually get a plain filesystem path. */
private fun uriStringToFilePath(localPath: String?): String? {
    if (localPath.isNullOrBlank()) return null
    return if (localPath.startsWith("file://")) Uri.parse(localPath).path else localPath
}

@Composable
private fun StoryImage(localPath: String?, remoteUrl: String?) {
    val actualPath = uriStringToFilePath(localPath)
    val localFileExists = !actualPath.isNullOrBlank() && File(actualPath).exists()

    if (localFileExists) {
        var bitmap by remember(actualPath) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(actualPath) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    decodeViaFileDescriptor(actualPath!!)
                } catch (e: Exception) {
                    Log.e("STORY_DEBUG", "StoryImage: decodeFileDescriptor failed for $actualPath", e)
                    null
                }
            }
        }

        var decodeFailed by remember(actualPath) { mutableStateOf(false) }
        LaunchedEffect(actualPath, bitmap) {
            if (bitmap == null) {
                delay(2000) // give the real decode attempt a couple seconds before assuming failure
                if (bitmap == null) decodeFailed = true
            }
        }

        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            decodeFailed -> Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E2C)),
                contentAlignment = Alignment.Center
            ) {
                Text("Photo unavailable on this device", color = Color.White.copy(alpha = 0.6f))
            }
            else -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) // still decoding
        }
    } else {
        AsyncImage(
            model = remoteUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = { Log.e("STORY_IMAGE_DEBUG", "AsyncImage failed to load remoteUrl=$remoteUrl: ${it.result.throwable}") },
            onSuccess = { Unit }
        )
    }
}
@Composable
private fun StoryCommentsBottomSheet(
    story: StoryEntity,
    comments: List<StoryCommentEntity>,
    currentUserId: String,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit, // ADD THIS
    onSetCommentsPrivate: (Boolean) -> Unit,
    onSetCommentsPrivateGlobally: ((Boolean) -> Unit)? = null, // optional: "apply to all my stories" default
    onDismiss: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isOwner = story.ownerId == currentUserId

    @kotlin.OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${comments.size} comments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Owner-only, whole-section toggle. Everyone can still comment;
                // when private, non-owners only see their own comments back
                // (filtered server-side) ? this just flips the setting.
                if (isOwner) {
                    IconButton(onClick = { showPrivacyDialog = true }) {
                        Icon(
                            imageVector = if (story.commentsPrivate) Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = if (story.commentsPrivate) {
                                "Comments are private ? tap to make public"
                            } else {
                                "Comments are public ? tap to make private"
                            },
                            tint = Color.Gray
                        )
                    }
                }
            }

            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(300.dp)
                ) {
                    items(
                        items = comments,
                        key = { it.id }
                    ) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comment.userId,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = comment.text,
                                    color = Color.Gray
                                )
                            }
                            if (comment.userId == currentUserId || story.ownerId == currentUserId) {
                                IconButton(
                                    onClick = { onDeleteComment(comment.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete comment",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text("Add a comment...")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText.trim())
                                commentText = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText.trim())
                            commentText = ""
                            keyboardController?.hide()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Add comment"
                    )
                }
            }
        }
    }

    if (showPrivacyDialog) {
        CommentsPrivacyDialog(
            currentlyPrivate = story.commentsPrivate,
            offerGlobalOption = onSetCommentsPrivateGlobally != null,
            onDismiss = { showPrivacyDialog = false },
            onConfirm = { newIsPrivate, applyGlobally ->
                onSetCommentsPrivate(newIsPrivate)
                if (applyGlobally) {
                    onSetCommentsPrivateGlobally?.invoke(newIsPrivate)
                }
                showPrivacyDialog = false
            }
        )
    }
}

/**
 * Confirms the comment-privacy change before committing it, and — when the caller
 * wired up onSetCommentsPrivateGlobally — lets the owner also set this as the
 * default for every story they post going forward, not just the one they're
 * currently viewing.
 */
@Composable
private fun CommentsPrivacyDialog(
    currentlyPrivate: Boolean,
    offerGlobalOption: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newIsPrivate: Boolean, applyGlobally: Boolean) -> Unit
) {
    // What we're switching TO, not the current state.
    var targetIsPrivate by remember { mutableStateOf(!currentlyPrivate) }
    var applyGlobally by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comment privacy") },
        text = {
            Column {
                Text(
                    if (currentlyPrivate) {
                        "Comments on this story are currently private — only you can see everyone's comments; others only see their own."
                    } else {
                        "Comments on this story are currently public — everyone can see all comments."
                    },
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Choice of what to switch to
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetIsPrivate = false },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = !targetIsPrivate,
                        onClick = { targetIsPrivate = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Public", fontWeight = FontWeight.SemiBold)
                        Text("Everyone can see all comments", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetIsPrivate = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = targetIsPrivate,
                        onClick = { targetIsPrivate = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Private", fontWeight = FontWeight.SemiBold)
                        Text("Others only see their own comments", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (offerGlobalOption) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyGlobally = !applyGlobally },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = applyGlobally, onCheckedChange = { applyGlobally = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Use this as the default for all my future stories",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(targetIsPrivate, applyGlobally) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestampMs: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}