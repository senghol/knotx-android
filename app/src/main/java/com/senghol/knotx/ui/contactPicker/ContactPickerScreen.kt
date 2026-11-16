package com.senghol.knotx.ui.contactPicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.senghol.knotx.KnotxApp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.senghol.knotx.ui.theme.LocalDootawasTheme



/**
 * Universal contact picker.
 *
 * Group member selection is configured through PickerSources:
 * - groupMembers = false: group members are not included.
 * - groupMembers = true, groupId = null: members from all groups.
 * - groupMembers = true, groupId = "id": members from one group.
 */
@Composable
fun ContactPickerScreen(
    sources: PickerSources,
    onConfirm: (List<PickedContact>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelected: Set<String> = emptySet(),
    initialItems: List<PickedContact> = emptyList(),
    title: String = "Select contacts",
    confirmLabel: String = "Done",
    allowEmpty: Boolean = false
) {
    val app = LocalContext.current.applicationContext as KnotxApp

    val repository = remember {
        val db = app.dootawasDatabase
        ContactPickerRepository(
            contacts = db.contactDao().getAllContactsFlow(),
            upbhogtas = db.upbhogtaDao().getAllUpbhogtas(),
            groups = db.groupDao().getAllGroups(),
            groupMembers = db.groupDao().getAllGroupMembersFlow()
        )
    }

    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }

    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }

    val vm: ContactPickerViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = ContactPickerViewModel.Factory(
            repository = repository,
            sources = sources,
            initialSelected = initialSelected,
            initialItems = initialItems
        )
    )

    val ui by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBack)

    val baseColor = LocalDootawasTheme.current.colors.homeBackground

    Box(
        modifier
            .fillMaxSize()
            .background(baseColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
        ) {
            PickerHeader(
                title = title,
                count = ui.selected.size,
                onBack = onBack
            )

            AnimatedVisibility(
                visible = ui.selectedItems.isNotEmpty(),
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                SelectedStrip(
                    items = ui.selectedItems,
                    onRemove = vm::toggle
                )
            }

            SearchField(
                query = ui.query,
                onChange = vm::setQuery
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val s = ui.sections

                if (s.isEmpty) {
                    EmptyState(searching = ui.query.isNotBlank())
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 110.dp
                        )
                    ) {
                        section(
                            "Home contacts", "h",
                            s.numberContacts, ui.selected, vm::toggle
                        )
                        section(
                            "Username contacts", "u",
                            s.usernameContacts, ui.selected, vm::toggle
                        )
                        section(
                            "Groups", "g",
                            s.groups, ui.selected, vm::toggle
                        )
                        section(
                            "Device contacts", "d",
                            s.deviceContacts, ui.selected, vm::toggle
                        )
                        section(
                            "Group members", "gm",
                            s.groupMembers, ui.selected, vm::toggle
                        )
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = ui.selected.isNotEmpty() || allowEmpty,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ConfirmPill(
                        label = confirmLabel,
                        count = ui.selected.size,
                        onClick = { onConfirm(vm.result()) }
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Header                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun PickerHeader(
    title: String,
    count: Int,
    onBack: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(onBg.copy(alpha = 0.08f))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back",
                tint = onBg,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            if (count > 0) {
                Text(
                    "$count selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Selected contacts                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun SelectedStrip(
    items: List<PickedContact>,
    onRemove: (String) -> Unit
) {
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items, key = { it.userNumber }) { item ->
            Column(
                Modifier.width(60.dp).clickable {
                    onRemove(item.userNumber)
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Avatar(item, size = 54.dp)

                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                            )
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.background,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Remove",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    item.shortLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Search                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val container = onBg.copy(alpha = 0.07f)

    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = {
            Text(
                "Search name, number or group",
                color = onBg.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                null,
                tint = onBg.copy(alpha = 0.6f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        "Clear",
                        tint = onBg.copy(alpha = 0.6f)
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = onBg,
            unfocusedTextColor = onBg,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

/* ------------------------------------------------------------------ */
/* Sections and rows                                                   */
/* ------------------------------------------------------------------ */

private fun LazyListScope.section(
    title: String,
    keyPrefix: String,
    list: List<PickedContact>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    if (list.isEmpty()) return

    item(key = "$keyPrefix-header") {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier.padding(
                start = 14.dp,
                top = 18.dp,
                bottom = 6.dp
            )
        )
    }

    items(list, key = { "$keyPrefix-${it.userNumber}" }) { item ->
        PickerRow(item, item.userNumber in selected) {
            onToggle(item.userNumber)
        }
    }
}

@Composable
private fun PickerRow(
    item: PickedContact,
    selected: Boolean,
    onClick: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val shape = RoundedCornerShape(18.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(item, size = 50.dp, checked = selected)
        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                item.primaryText(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(1.dp))

            Text(
                item.secondaryText(),
                style = MaterialTheme.typography.bodySmall,
                color = onBg.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Empty state                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun EmptyState(searching: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            if (searching) "No matches" else "No contacts yet",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/* ------------------------------------------------------------------ */
/* Confirm button                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun ConfirmPill(
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.shadow(10.dp, CircleShape)
    ) {
        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))

        Text(
            if (count == 0) label else "$label · $count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ------------------------------------------------------------------ */
/* Avatar and text rules                                               */
/* ------------------------------------------------------------------ */

@Composable
private fun Avatar(
    item: PickedContact,
    size: Dp,
    checked: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(Modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.isGroup) {
                Icon(
                    Icons.Default.Group,
                    null,
                    tint = primary,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else {
                Text(
                    item.initial(),
                    color = primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!item.profileUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.profileUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (checked) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.42f)
                    .clip(CircleShape)
                    .background(primary)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.background,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.24f)
                )
            }
        }
    }
}

private fun PickedContact.primaryText(): String =
    if (isGroup) userName else userNumber

private fun PickedContact.secondaryText(): String =
    if (isGroup) "Group" else displayName

private fun PickedContact.shortLabel(): String = displayName

private fun PickedContact.initial(): String =
    displayName.trim().trimStart('@', '+').firstOrNull()?.uppercase() ?: "?"
