package com.senghol.knotx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups3
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.senghol.knotx.R
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.GroupDootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import com.senghol.knotx.utils.enums.SearchFilter
import kotlin.Boolean
@Composable
fun SearchBox(
    isSearchMode: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    sandeshViewModel: SandeshViewModel,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    if (!isSearchMode) return

    val selectedFilters by sandeshViewModel.searchFilters.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = searchText,
                onValueChange = {
                    onSearchTextChange(it)
                    sandeshViewModel.filterSearch(it)
                },
                placeholder = { Text("Search...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp), // default minimum is 56.dp
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    IconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = {
                            onSearchTextChange("")
                            sandeshViewModel.filterSearch("")
                            sandeshViewModel.clearSearchFilters()
                            onCloseSearch()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Search")
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchFilter.entries.forEach { filter ->
                    val selected = filter in selectedFilters
                    FilterChip(
                        modifier = Modifier.height(28.dp),
                        selected = selected,
                        onClick = { sandeshViewModel.toggleSearchFilter(filter) },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (selected) Icons.Default.Check else filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun GroupSearchBox(
    isSearchMode: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    sandeshViewModel: SandeshViewModel
) {
    if (isSearchMode) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
//                .padding(8.dp)
        ) {
            TextField(
                value = searchText,
                onValueChange = {
                    onSearchTextChange(it)
                    sandeshViewModel.filterGroupSearch(it)
                },
                placeholder = { Text("Search...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                trailingIcon = {
                    IconButton(onClick = {
                        onSearchTextChange("")
                        sandeshViewModel.filterGroupSearch("")
                        onCloseSearch()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Search")
                    }
                }
            )
        }
    }
}

@Composable
fun UserHeaderPortrait(
    photoUrl: String?,
    validUserName: String?,
    userNumber: String,
    onShowUserSettings: () -> Unit,
    onToggleSearch: () -> Unit,
    isGroup: Boolean = false,
    onBackClick: () -> Unit,
    dootawasColors: DootawasColors,
    isAi: Boolean = false,
    onShowAiUsage: () -> Unit
) {

    val shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = dootawasColors.userHeaderBackground, shape = shape)
            .clickable { onShowUserSettings() }
            .padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = { onBackClick() },
                modifier = Modifier.size(40.dp) // control button size
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = dootawasColors.userHeaderText,
                    modifier = Modifier.size(30.dp) // 🔥 increase icon size
                )
            }

            ProfilePicture(photoUrl,  isGroup = isGroup)

            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally // ✅ center text vertically
            ) {
                validUserName?.let {
                    Text(
                        text = it,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = dootawasColors.userHeaderText
                    )
                }
                if (!isAi && userNumber.isNotBlank()) {
                    Text(
                        text = userNumber,
                        fontSize = if (validUserName == null) 18.sp else 14.sp,
                        fontWeight = if (validUserName == null) FontWeight.Bold else FontWeight.Normal,
                        color =  dootawasColors.userHeaderText
                    )
                }
            }

            if (isAi) {
                IconButton(
                    onClick = onShowAiUsage,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "AI Usage",
                        tint = dootawasColors.userHeaderText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            IconButton(onClick = { onToggleSearch() }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = dootawasColors.userHeaderText
                )
            }
        }
    }
}


@Composable
fun UserGroupHeaderPortrait(
    photoUrl: String?,
    validUserName: String?,
    userNumber: String,
    onShowUserSettings: () -> Unit,
    onToggleSearch: () -> Unit,
    onBackClick: () -> Unit,
    groupColors: GroupDootawasColors
) {
    val shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = groupColors.userHeaderBackground, shape = shape)
            .clickable { onShowUserSettings() }
            .padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = { onBackClick() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = groupColors.userHeaderText,
                    modifier = Modifier.size(30.dp)
                )
            }

            ProfilePicture(photoUrl, isGroup = true)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                validUserName?.let {
                    Text(
                        text = it,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = groupColors.userHeaderText
                    )
                }
                if (userNumber.isNotBlank()) {
                    Text(
                        text = userNumber,
                        fontSize = if (validUserName == null) 18.sp else 14.sp,
                        fontWeight = if (validUserName == null) FontWeight.Bold else FontWeight.Normal,
                        color = groupColors.userHeaderText
                    )
                }
            }

            IconButton(onClick = { onToggleSearch() }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = groupColors.userHeaderText
                )
            }
        }
    }
}
@Composable
fun UserHeaderLandscape(
    photoUrl: String?,
    validUserName: String?,
    userNumber: String,
    onShowUserSettings: () -> Unit,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isGroup: Boolean = false,
    isNavBarOnRight: Boolean,
    onBackClick: () -> Unit,
    isAi: Boolean = false,
    onShowAiUsage: () -> Unit,
    userHeaderTextColor: Color
) {
    val shape = if (isNavBarOnRight) {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = modifier
            .width(52.dp)
            .shadow(8.dp, shape = shape, clip = false)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20), // dark green top
                        Color(0xFF004D40)  // deep teal bottom
                    )
                ),
                shape = shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .clickable { onShowUserSettings() }
            .padding(vertical = 16.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight()
        ) {
            IconButton(
                onClick = { onBackClick() },
                modifier = Modifier.size(40.dp) // control button size
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    modifier = Modifier.size(30.dp) // 🔥 increase icon size
                )
            }
            // Profile picture with its own click
            Box(
                modifier = Modifier
                    .clip(CircleShape)
            ) {
                ProfilePicture(photoUrl,  isGroup = isGroup)
            }

            if (isAi) {
                IconButton(
                    onClick = onShowAiUsage,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "AI Usage",
                        tint = userHeaderTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            IconButton(
                onClick = { onToggleSearch() },
                modifier = Modifier.
                padding(1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = userHeaderTextColor
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // User name & number
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentSize()
                    .rotate(270f)
            ) {
                validUserName?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }

                if (!isAi && userNumber.isNotBlank()) {
                    Text(
                        text = userNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.LightGray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }

    }
}
@Composable
fun ProfilePicture(
    photoUrl: String?,
    isGroup: Boolean
) {
    var showPreview by remember { mutableStateOf(false) }
    if (photoUrl.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGroup) Icons.Filled.Groups3 else Icons.Filled.Person,
                contentDescription = if (isGroup) "Default group photo" else "Default profile photo",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable {showPreview = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(photoUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }
    }
    // 🖼️ Full-size preview dialog
    if (showPreview && photoUrl != null) {
        
        ProfileImagePreviewDialog(
            imageUrl = photoUrl,
            onDismiss = { showPreview = false }
        )
    }
}
