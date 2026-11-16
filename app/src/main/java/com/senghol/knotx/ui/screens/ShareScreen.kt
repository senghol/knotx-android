package com.senghol.knotx.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*

import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.senghol.knotx.KnotxApp

import com.senghol.knotx.ui.contactPicker.ContactPickerRepository
import com.senghol.knotx.ui.contactPicker.ContactPickerViewModel
import com.senghol.knotx.ui.contactPicker.PickedContact
import com.senghol.knotx.ui.contactPicker.PickerSources

import com.senghol.knotx.ui.viewmodel.SandeshViewModel

import com.senghol.knotx.utils.objects.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


private enum class PickerFilter(val label: String) {
    ALL("All"), PEOPLE("People"), GROUPS("Groups"), PHONE("Phone")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShareScreen(
    sharedText: String?,
    sharedUris: List<Uri>?,
    sandeshViewModel: SandeshViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as KnotxApp

    val database = remember { application.dootawasDatabase }
    val dootawas = remember { application.knotx }
    val sandeshDao = remember { database.sandeshDao() }
    val app = LocalContext.current.applicationContext as KnotxApp
    val repository = remember {
        val db = app.dootawasDatabase
        ContactPickerRepository(
            contacts = db.contactDao().getAllContactsFlow(),
            upbhogtas = db.upbhogtaDao().getAllUpbhogtas(),   // adjust to your accessor name
            groups = db.groupDao().getAllGroups(),              // adjust to your accessor name
                    groupMembers = db.groupDao().getAllGroupMembersFlow()
        )
    }
    var isImportMode by rememberSaveable { mutableStateOf(false) }
    var caption by rememberSaveable { mutableStateOf("") }
    var openImportScreen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(PickerFilter.ALL) }

    // ---- ZIP detection -> shows the "Import chat" option ----
    val zipUri = remember(sharedUris) {
        sharedUris?.singleOrNull()?.takeIf { uri ->
            val mime = context.contentResolver.getType(uri)
            uri.toString().endsWith(".zip", true) ||
                    mime == "application/zip" ||
                    mime == "application/x-zip-compressed"
        }
    }
    val zipName = remember(zipUri) { zipUri?.let { queryDisplayName(context, it) } }

    // Sources are fixed per ViewModel; different key = fresh VM (selection resets on mode switch)
    val sources = remember(isImportMode) {
        if (isImportMode) PickerSources(numberContacts = true, usernameContacts = true)
        else PickerSources(
            numberContacts = true,
            usernameContacts = true,
            groups = true,
            deviceContacts = true
        )
    }
    val picker: ContactPickerViewModel = viewModel(
        key = "share_picker_$isImportMode",
        factory = ContactPickerViewModel.Factory(repository, sources, emptySet())
    )
    val state by picker.uiState.collectAsState()
    val hasSelection = state.selected.isNotEmpty()
    val hasMedia = !sharedUris.isNullOrEmpty()

    fun onToggle(contact: PickedContact) {
        if (isImportMode) {
            state.selected.filter { it != contact.userNumber }.forEach(picker::toggle)
        }
        picker.toggle(contact.userNumber)
    }

    fun send() {
        val targets = picker.result()
        if (targets.isEmpty()) return
        if (isImportMode) {
            openImportScreen = true
        } else {
            val selectedList = targets.map { it.userNumber to it.isGroup }
            sandeshViewModel.processShareSend(
                context = context,
                sharedText = sharedText,
                sharedUris = sharedUris,
                selectedTargets = selectedList,
                caption = caption.trim()   // ASSUMPTION: new param
            )
            PrefsHelper.setDirectId(context, selectedList.first().first)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Share to",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = hasSelection,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                SharePanel(
                    selectedItems = state.selectedItems,
                    onRemove = { picker.toggle(it.userNumber) },
                    isImportMode = isImportMode,
                    sharedText = sharedText,
                    sharedUris = sharedUris,
                    hasMedia = hasMedia,
                    caption = caption,
                    onCaptionChange = { caption = it },
                    onSend = ::send
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PillTextField(
                value = state.query,
                onValueChange = picker::setQuery,
                placeholder = "Search people & groups",
                leading = { Icon(Icons.Default.Search, null) },
                trailing = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { picker.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (zipUri != null) {
                ZipImportCard(
                    fileName = zipName ?: "chat.zip",
                    isImport = isImportMode,
                    onModeChange = { isImportMode = it }
                )
            }

            if (!isImportMode) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PickerFilter.values().toList()) { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }

            val s = state.sections
            if (s.isEmpty) {
                EmptyState(isSearching = state.query.isNotBlank())
            } else {
                val all = isImportMode || filter == PickerFilter.ALL
                val people = all || filter == PickerFilter.PEOPLE
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (people) pickerSection("Contacts", s.numberContacts, state.selected, ::onToggle)
                    if (people) pickerSection("Usernames", s.usernameContacts, state.selected, ::onToggle)
                    if (all || filter == PickerFilter.GROUPS)
                        pickerSection("Groups", s.groups, state.selected, ::onToggle)
                    if (all || filter == PickerFilter.PHONE)
                        pickerSection("On your phone", s.deviceContacts, state.selected, ::onToggle)
                }
            }
        }
    }

    if (openImportScreen) {
        val selectedUser = picker.result().firstOrNull()
        if (selectedUser != null && zipUri != null) {
            WaImportScreen(
                context = context,
                sandeshDao = sandeshDao,
                zipUri = zipUri,
                userNumber = selectedUser.userNumber,
                onImportComplete = {
                    openImportScreen = false
                    PrefsHelper.setDirectId(context, selectedUser.userNumber)
                }
            )
        }
    }
}

/* ----------------------------- ZIP IMPORT CARD ----------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZipImportCard(
    fileName: String,
    isImport: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        fileName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Looks like a chat export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isImport,
                    onClick = { onModeChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { SegmentedButtonDefaults.Icon(active = !isImport) }
                ) { Text("Send as file") }
                SegmentedButton(
                    selected = isImport,
                    onClick = { onModeChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { SegmentedButtonDefaults.Icon(active = isImport) }
                ) { Text("Import chat") }
            }
        }
    }
}

/* ------------------------------- LIST PIECES ------------------------------- */

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.pickerSection(
    title: String,
    items: List<PickedContact>,
    selected: Set<String>,
    onToggle: (PickedContact) -> Unit
) {
    if (items.isEmpty()) return
    stickyHeader(key = "header_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
    items(items, key = { "${title}_${it.userNumber}" }) { contact ->
        PickerRow(
            contact = contact,
            isSelected = contact.userNumber in selected,
            onClick = { onToggle(contact) }
        )
    }
}

@Composable
private fun PickerRow(
    contact: PickedContact,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "rowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectableAvatar(contact, isSelected, 50.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                contact.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = when {
                contact.isGroup -> "Group"
                contact.userName.isNotBlank() -> contact.userNumber
                else -> null
            }
            if (sub != null) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** The avatar IS the selector: tap a row and it flips to a check badge. */
@Composable
private fun SelectableAvatar(contact: PickedContact, selected: Boolean, size: Dp) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "avatarScale"
    )
    Box(Modifier.scale(scale)) {
        Crossfade(targetState = selected, label = "avatarSwap") { sel ->
            if (sel) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size * 0.5f)
                    )
                }
            } else {
                PickerAvatar(contact, size)
            }
        }
    }
}

@Composable
private fun PickerAvatar(contact: PickedContact, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        when {
            !contact.profileUri.isNullOrBlank() -> AsyncImage(
                model = contact.profileUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            contact.isGroup -> Icon(
                Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            else -> Text(
                contact.displayName.trimStart('@').firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearching) Icons.Default.SearchOff else Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (isSearching) "No results" else "No contacts yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ------------------------------- BOTTOM PANEL ------------------------------ */

@Composable
private fun SharePanel(
    selectedItems: List<PickedContact>,
    onRemove: (PickedContact) -> Unit,
    isImportMode: Boolean,
    sharedText: String?,
    sharedUris: List<Uri>?,
    hasMedia: Boolean,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(selectedItems, key = { it.userNumber }) { c ->
                    SelectedBubble(c) { onRemove(c) }
                }
            }

            if (!isImportMode && (sharedText != null || hasMedia)) {
                Spacer(Modifier.height(12.dp))
                SharePreviewStrip(sharedText, sharedUris)
            }

            Spacer(Modifier.height(12.dp))

            if (isImportMode) {
                Button(
                    onClick = onSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Import chat with ${selectedItems.firstOrNull()?.displayName.orEmpty()}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PillTextField(
                        value = caption,
                        onValueChange = onCaptionChange,
                        placeholder = if (hasMedia) "Add a caption…" else "Add a message…",
                        singleLine = false,
                        modifier = Modifier.weight(1f)
                    )
                    FloatingActionButton(
                        onClick = onSend,
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedBubble(contact: PickedContact, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onRemove),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            PickerAvatar(contact, 52.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.inverseSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            contact.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true
) {
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = bg,
            unfocusedContainerColor = bg,
            disabledContainerColor = bg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

/* --------------------------------- PREVIEW --------------------------------- */

@Composable
fun SharePreviewStrip(
    sharedText: String?,
    sharedUris: List<Uri>?
) {
    var preview by remember { mutableStateOf<LinkPreview?>(null) }
    val isUrl = sharedText?.startsWith("http") == true

    LaunchedEffect(sharedText) {
        if (isUrl && sharedText != null && preview == null) {
            preview = fetchLinkPreview(sharedText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sharedText != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    preview?.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        val title = preview?.title?.takeIf { it.isNotBlank() }
                        if (title != null) {
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            sharedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUrl) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (title != null) 1 else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (!sharedUris.isNullOrEmpty()) {
            val resolver = LocalContext.current.contentResolver
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sharedUris) { uri ->
                    val mimeType = remember(uri) { resolver.getType(uri) }
                    if (mimeType?.startsWith("image/") == true) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        DocumentPreviewCard(uri, mimeType)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentPreviewCard(uri: Uri, mimeType: String?) {
    val context = LocalContext.current
    val fileName = remember(uri) { queryDisplayName(context, uri) }

    Surface(
        modifier = Modifier.size(width = 160.dp, height = 72.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    mimeType ?: "file",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment ?: "File"

// Keep your existing LinkPreview data class and fetchLinkPreview() unchanged.



@Composable
private fun ImportToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Import as chat history", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Pick one person to import this export into",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

//
//// Keep your existing LinkPreview data class and fetchLinkPreview() unchanged.
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ShareScreen(
//    sharedText: String?,
//    sharedUris: List<Uri>?,
//    sandeshViewModel: SandeshViewModel,
//    onBack: () -> Unit
//) {
//
//    val context = LocalContext.current
//    val application = context.applicationContext as KnotxApp
//
//    val database = remember { application.dootawasDatabase}
//    val dootawasDB = remember { application.dootawasDB}
//    val contactDao = remember { database.contactDao() }
//    val sandeshDao = remember { database.sandeshDao() }
//    val singleRepository = remember { HomeSingleRepository(database) }
//    val groupRepository = remember { HomeGroupRepository(database) }
//    val callSessionRepository = remember { CallSessionRepository(dootawasDB,database) }
//    val knotx = remember { application.knotx }
//    val storyRepository = remember { StoryRepository(database,knotx) }
//
//    var isImportMode by rememberSaveable { mutableStateOf(false) }
//
//    val canImportChat =
//        sharedUris?.size == 1 &&
//                sharedUris.firstOrNull()
//                    ?.toString()?.endsWith("", true) == true
//
//    val contactViewModel: ContactViewModel = viewModel(
//        factory = ContactViewModelFactory(contactDao, knotx)
//    )
//
//    val homeViewModel: HomeViewModel = viewModel(
//        factory = HomeViewModelFactory(
//            singleRepository = singleRepository,
//            groupRepository = groupRepository,
//            callRepository = callSessionRepository,
//            storyRepository = storyRepository,
//            knotx = knotx
//        )
//    )
//
//
//    val upbhogtas by homeViewModel.filteredUpbhogtaItems.collectAsState()
//    val groups by homeViewModel.filteredGroupItems.collectAsState()
//    val allContacts by contactViewModel.filteredContacts.collectAsState()
//
//    var selectedContacts by remember { mutableStateOf<Set<ContactUiItem>>(emptySet()) }
//    var selectedUpbhogtas by remember { mutableStateOf<Set<UpbhogtaUiItem>>(emptySet()) }
//    var selectedGroups by remember { mutableStateOf<Set<GroupUiItem>>(emptySet()) }
//    var searchQuery by remember { mutableStateOf("") }
//    val scope = rememberCoroutineScope()
//    val listState = rememberLazyListState()
//    val addableContacts = remember(allContacts) {
//        allContacts.filter { it.buttonState == ContactButtonState.ADD }
//    }
//
//    LaunchedEffect(Unit) {
//        listState.scrollToItem(0)
//    }
//    var openImportScreen by remember { mutableStateOf(false) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Share To") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
//                    }
//                },
//                actions = {
//                    if (selectedContacts.isNotEmpty() ||
//                        selectedUpbhogtas.isNotEmpty() ||
//                        selectedGroups.isNotEmpty()
//                    ) {
//                        IconButton(
//                            onClick = {
//
//                                if (isImportMode) {
//                                    println("OPENING WAA")
//                                    openImportScreen = true
//
//                                } else {
//                                    val selectedList = buildList {
//                                        selectedContacts.forEach { add(it.number to false) }
//                                        selectedUpbhogtas.forEach { add(it.userNumber to false) }
//                                        selectedGroups.forEach { add(it.id to true) }
//                                    }
//                                    sandeshViewModel.processShareSend(
//                                        context = context,
//                                        sharedText = sharedText,
//                                        sharedUris = sharedUris,
//                                        selectedTargets = selectedList
//                                    )
//                                    if (selectedList.isNotEmpty()) {
//                                        val (id, _) = selectedList.first()
//                                        PrefsHelper.setDirectId(context, id)
//                                    }
//                                }
//                            }
//                        ) {
//                            Icon(Icons.AutoMirrored.Filled.Send, null)
//                        }
//                    }
//                }
//            )
//        }
//    ) { padding ->
//
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//        ) {
//
//
//            LazyColumn(
//                state = listState,
//                modifier = Modifier
//                    .weight(1f)   // 👈 IMPORTANT
//                    .fillMaxSize()
//            ) {
//                // 🔍 SEARCH
//                item {
//                    OutlinedTextField(
//                        value = searchQuery,
//                        onValueChange = {
//                            searchQuery = it
//                            contactViewModel.search(it)
//                            homeViewModel.searchUpbhogtas(it)
//                            homeViewModel.searchGroups(it)
//                        },
//                        label = { Text("Search Contacts & Groups") },
//                        leadingIcon = {
//                            Icon(Icons.Default.Search, null)
//                        },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(8.dp)
//                    )
//                }
//
//
//                if (canImportChat) {
//
//                    item {
//
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 12.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//
//                            Checkbox(
//                                checked = isImportMode,
//                                onCheckedChange = {
//
//                                    isImportMode = it
//
//                                    selectedContacts = emptySet()
//                                    selectedGroups = emptySet()
//                                    selectedUpbhogtas = emptySet()
//                                }
//                            )
//
//                            Text("Enable Chat Import")
//                        }
//                    }
//                }
//
//                items(upbhogtas) { upbhogta ->
//
//                    UpbhogtaItemA(
//                        upbhogta = upbhogta,
//                        isSelected = selectedUpbhogtas.contains(upbhogta),
//                        onToggleSelection = { selected ->
//                            selectedUpbhogtas = if (isImportMode) {
//                                if (selected) setOf(upbhogta)
//                                else emptySet()
//                            } else {
//                                if (selected) selectedUpbhogtas + upbhogta
//                                else selectedUpbhogtas - upbhogta
//                            }
//                        }
//                    )
//                }
//                if (!isImportMode) {
//
//                    // 👥 GROUPS
//                    items(groups) { group ->
//                        GroupItemA(
//                            group = group,
//                            isSelected = selectedGroups.contains(group),
//                            onToggleSelection = { selected ->
//                                selectedGroups =
//                                    if (selected) selectedGroups + group
//                                    else selectedGroups - group
//                            }
//                        )
//                    }
//                    // 📱 DEVICE CONTACTS
//                    items(addableContacts) { contact ->
//                        ContactItemA(
//                            contact = contact,
//                            isSelected = selectedContacts.contains(contact),
//                            onToggleSelection = { selected ->
//                                selectedContacts =
//                                    if (selected) selectedContacts + contact
//                                    else selectedContacts - contact
//                            }
//                        )
//                    }
//                }
//            }
//
//            // 🔥 FIXED PREVIEW (NOT SCROLLABLE)
//            if (sharedText != null || !sharedUris.isNullOrEmpty()) {
//
//                SharePreviewSection(sharedText, sharedUris)
//                HorizontalDivider()
//            }
//
//        }
//    }
//    if (openImportScreen) {
//        println("OPENING WAB")
//        val selectedUser =
//            selectedUpbhogtas.firstOrNull()
//                ?: return
//        println("OPENING WA")
//        WaImportScreen(
//            context = context,
//            sandeshDao = sandeshDao,
//            zipUri = sharedUris!!.first(),
//            userNumber = selectedUser.userNumber,
//            onImportComplete = {
//                openImportScreen = false
//                PrefsHelper.setDirectId(context, selectedUser.userNumber)
//            }
//        )
//    }
//}
data class LinkPreview(
    val title: String?,
    val imageUrl: String?
)
@Composable
fun SharePreviewSection(
    sharedText: String?,
    sharedUris: List<Uri>?
) {

    if (sharedText == null && sharedUris.isNullOrEmpty()) return

    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<LinkPreview?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isUrl = sharedText?.startsWith("http") == true

    // 🔥 Fetch link metadata
    LaunchedEffect(sharedText) {
        if (isUrl && preview == null) {
            isLoading = true
            preview = fetchLinkPreview(sharedText)
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {


            Spacer(modifier = Modifier.height(12.dp))

            // 🔥 LINK PREVIEW
            if (isUrl) {

                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }

                    preview != null -> {

                        Column {

                            // 🔹 Top Row (Image left, text right)
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                // 🔹 IMAGE LEFT
                                preview?.imageUrl?.let { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    // 🔹 TITLE (from preview OR fallback)
                                    Text(
                                        text = preview?.title?.takeIf { it.isNotBlank() } ?: sharedText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 🔹 DESCRIPTION = sharedText (URL or text)
                                    Text(
                                        text = sharedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 🔹 URL AT BOTTOM
                            Text(
                                text = sharedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    else -> {
                        Text(sharedText)
                    }
                }
            }

            // 🔥 NORMAL TEXT (non-link)
            if (!isUrl) {
                sharedText?.let {
                    Text(it)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔥 FILES / IMAGES
            sharedUris?.let { uris ->

                LazyRow {
                    items(uris) { uri ->

                        val mimeType = LocalContext.current
                            .contentResolver.getType(uri)

                        if (mimeType?.startsWith("image/") == true) {

                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(end = 8.dp)
                            )

                        } else {

                            DocumentPreviewCard(uri, mimeType)
                        }
                    }
                }
            }
        }
    }
}


suspend fun fetchLinkPreview(url: String): LinkPreview? {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .referrer("https://www.google.com")
                .timeout(10_000)
                .get()

            val title = doc.select("meta[property=og:title]").attr("content")
            val image = doc.select("meta[property=og:image]").attr("content")

            LinkPreview(
                title = title.ifBlank { doc.title() },
                imageUrl = image.takeIf { it.isNotBlank() }
            )

        } catch (e: Exception) {
            null
        }
    }
}
//
//@Composable
//fun DocumentPreviewCard(
//    uri: Uri,
//    mimeType: String?
//) {
//
//    val fileName = remember(uri) {
//        uri.lastPathSegment ?: "Document"
//    }
//
//    Card(
//        modifier = Modifier
//            .size(width = 140.dp, height = 100.dp)
//            .padding(end = 8.dp),
//        elevation = CardDefaults.cardElevation(4.dp)
//    ) {
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(10.dp),
//            verticalArrangement = Arrangement.Center
//        ) {
//
//            Icon(
//                imageVector = Icons.Default.Description,
//                contentDescription = null,
//                modifier = Modifier.size(32.dp),
//                tint = MaterialTheme.colorScheme.primary
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            Text(
//                text = fileName,
//                maxLines = 1,
//                style = MaterialTheme.typography.bodySmall
//            )
//
//            Spacer(modifier = Modifier.height(4.dp))
//
//            Text(
//                text = mimeType ?: "file",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.outline
//            )
//        }
//    }
//}