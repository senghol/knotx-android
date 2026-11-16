package com.senghol.knotx.ui.dialogs


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import kotlinx.coroutines.launch

// keep your existing package line here

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.platform.LocalContext

import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
// import LocalDootawasTheme the same way you do today

/**
 * @param disappearSeconds the chat's disappearing-message setting (upbhogta.autoDisappearMsg).
 *                         The Download button is shown only when this is not 0.
 * @param onEdit           receives the current image as a Uri. Set it as your selected/edited image
 *                         and close this screen, so the editor opens. Null hides the Edit button.
 *
 * Edit and Download need a local image (file path, file:// or content://). Remote http(s) images
 * simply don't show those two buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewScreen(
    imageList: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
    disappearSeconds: Int = 0,
    onEdit: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalDootawasTheme.current.colors

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (imageList.size - 1).coerceAtLeast(0)),
        pageCount = { imageList.size }
    )
    var showUI by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    val currentUri: Uri? = remember(pagerState.currentPage, imageList) {
        imageList.getOrNull(pagerState.currentPage)?.toLocalUri()
    }
    val canEdit = onEdit != null && currentUri != null && disappearSeconds == 0
    val canDownload = disappearSeconds == 0 && currentUri != null

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.userHeaderBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !zoomed       // while zoomed, one finger pans the photo instead of swiping pages
        ) { page ->
            ZoomablePage(
                model = imageList[page],
                isCurrent = page == pagerState.currentPage,
                onZoomChange = { zoomed = it },
                onTap = { showUI = !showUI }
            )
        }

        // ---------------- Top: close, counter, actions ----------------
        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                GlassButton(Icons.Default.Close, "Close", Modifier.align(Alignment.CenterStart), onDismiss)

                if (imageList.size > 1) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${imageList.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                Row(
                    Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canDownload) {
                        if (saving) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }
                        } else {
                            GlassButton(Icons.Default.Download, "Save to gallery") {
                                val uri = currentUri ?: return@GlassButton
                                saving = true
                                scope.launch {
                                    val ok = saveToGallery(context, uri)
                                    saving = false
                                    Toast.makeText(
                                        context,
                                        if (ok) "Saved to gallery" else "Couldn't save the image",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    if (canEdit) {
                        GlassButton(Icons.Default.Edit, "Edit") { currentUri?.let { onEdit?.invoke(it) } }
                    }
                }
            }
        }

        // ---------------- Bottom: thumbnail strip ----------------
        AnimatedVisibility(
            visible = showUI && imageList.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(pagerState.currentPage) {
                listState.animateScrollToItem((pagerState.currentPage - 2).coerceAtLeast(0))
            }
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                itemsIndexed(imageList) { index, url ->
                    val selected = index == pagerState.currentPage
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .alpha(if (selected) 1f else 0.65f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* One zoomable page                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun ZoomablePage(
    model: String,
    isCurrent: Boolean,
    onZoomChange: (Boolean) -> Unit,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Tell the pager whether this page is zoomed (only the visible page decides)
    LaunchedEffect(scale, isCurrent) { if (isCurrent) onZoomChange(scale > 1f) }
    // A page that scrolls out of view goes back to normal size
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            val maxX = size.width * (scale - 1f) / 2f
            val maxY = size.height * (scale - 1f) / 2f
            offset = Offset(
                (offset.x + panChange.x).coerceIn(-maxX, maxX),
                (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .transformable(state = transformState, canPan = { scale > 1f })
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        )
    }
}

/* ------------------------------------------------------------------ */
/* Pieces                                                              */
/* ------------------------------------------------------------------ */

/** Round translucent button that stays readable over any photo or background. */
@Composable
private fun GlassButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */

/** Local path / file:// / content:// become a Uri; remote http(s) images return null. */
private fun String.toLocalUri(): Uri? = when {
    startsWith("http://") || startsWith("https://") -> null
    startsWith("/") -> Uri.fromFile(File(this))
    else -> Uri.parse(this)
}

/**
 * Copies the image into the device gallery (Pictures/Knotx) with MediaStore.
 * No permission needed on Android 10+ (your min SDK is 29).
 */
private suspend fun saveToGallery(context: Context, source: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val extFromName = source.lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val mime = resolver.getType(source)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extFromName)
            ?: "image/jpeg"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Dootawas_${System.currentTimeMillis()}.$ext")
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Knotx")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching false
        try {
            resolver.openInputStream(source)!!.use { input ->
                resolver.openOutputStream(target)!!.use { out -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(target, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            false
        }
    }.getOrDefault(false)
}