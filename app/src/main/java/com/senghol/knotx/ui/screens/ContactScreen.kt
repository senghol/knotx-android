package com.senghol.knotx.ui.screens
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.senghol.knotx.R
import com.senghol.knotx.ui.components.ContactItem
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.ui.viewmodel.ContactViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val MEMBERS_TO_STOP_HINT = 5
private const val HINT_ROTATE_DELAY_MS = 4000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(contactViewModel: ContactViewModel, navController: NavController) {

    val contacts by contactViewModel.filteredContacts.collectAsState()
    val appUserCount by contactViewModel.appUserCount.collectAsState()
    val refreshing by contactViewModel.isRefreshing.collectAsState()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    val theme = LocalDootawasTheme.current
    val dootawasColors = theme.colors
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val refreshScope = rememberCoroutineScope()

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // ───────── Rotating placeholder hint ─────────
    // Shown while fewer than 5 contacts are on the app (ADD / ADDED)
    val hintResIds = listOf(
        R.string.search_placeholder_invite_hint,   // shown first
        R.string.search_placeholder_sync_hint
    )
    var hintIndex by remember { mutableIntStateOf(0) }
    val showRotatingHint = appUserCount < MEMBERS_TO_STOP_HINT

    LaunchedEffect(showRotatingHint) {
        while (showRotatingHint) {
            delay(HINT_ROTATE_DELAY_MS.milliseconds)
            hintIndex = (hintIndex + 1) % hintResIds.size
        }
    }

    // ───────── Effects ─────────
    LaunchedEffect(Unit) {
        contactViewModel.readContacts()
    }

    // Observe visible items
    LaunchedEffect(listState, contacts) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                visibleItems.forEach { item ->
                    contacts.getOrNull(item.index)?.let {
                        contactViewModel.addItemToUpdate(it.number)
                    }
                }
            }
    }

    BackHandler {
        navController.popBackStack()
    }

    fun triggerRefresh() {
        refreshScope.launch {
            contactViewModel.refreshContacts()
        }
    }

    // ───────── UI ─────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dootawasColors.homeBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                DootawasSearchBar(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        contactViewModel.search(it.text)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (!it.isFocused) keyboardController?.hide()
                        },
                    placeholder = if (showRotatingHint) {
                        stringResource(id = hintResIds[hintIndex])
                    } else {
                        stringResource(id = R.string.search)
                    },
                    colors = dootawasColors,
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )

                IconButton(
                    onClick = { saveContact(context, "", "") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_contact),
                        tint = Color(0xFFD50000),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = refreshing,
                state = pullRefreshState,
                onRefresh = { triggerRefresh() }
            ) {
                if (refreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.TopCenter),
                        color = Color(0xFFAEEA00),
                        trackColor = Color.Transparent
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(
                        items = contacts,
                        key = { it.number }   // stable keys; remove if numbers can repeat
                    ) { contact ->
                        ContactItem(contact, contactViewModel, navController)
                    }
                }
            }
        }
    }
}



//        // 🔙 Go Back Floating Button
//        FloatingActionButton(
//            onClick = { navController.popBackStack() },
//            containerColor = Color.Red,
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp)
//        ) {
//            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back", tint = Color.White)
//        }


fun saveContact(context: Context, name: String, number: String) {
    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.NAME, name)
        putExtra(ContactsContract.Intents.Insert.PHONE, number)
    }
    context.startActivity(intent)
}

