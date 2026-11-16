package com.senghol.knotx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senghol.knotx.KnotxApp

import com.senghol.knotx.ui.repository.HomeGroupRepository
import com.senghol.knotx.ui.repository.HomeSingleRepository
import com.senghol.knotx.ui.repository.StoryRepository
import com.senghol.knotx.ui.viewmodel.CallSessionRepository

import com.senghol.knotx.ui.viewmodel.ContactViewModel
import com.senghol.knotx.ui.viewmodel.ContactViewModelFactory
import com.senghol.knotx.ui.viewmodel.HomeViewModel
import com.senghol.knotx.ui.viewmodel.HomeViewModelFactory
import com.senghol.knotx.utils.data.ContactUiItem
import com.senghol.knotx.utils.data.UpbhogtaUiItem

import com.senghol.knotx.utils.enums.ContactButtonState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberContactPicker(
    onContactsSelected: (List<String>) -> Unit,
    onBack: () -> Unit,
    existingMembers: List<String>
) {
    val context = LocalContext.current
    val application = context.applicationContext as KnotxApp

    val database = remember { application.dootawasDatabase}
    val dootawasDB = remember { application.dootawasDB}
    val contactDao = remember { database.contactDao() }
    val singleRepository = remember { HomeSingleRepository(database) }
    val groupRepository = remember { HomeGroupRepository(database) }
    val callSessionRepository = remember { CallSessionRepository(dootawasDB,database) }
    val dootawas = remember { application.knotx }
    val dootawasStore = remember { application.dootawasStore }
    val storyRepository = remember { StoryRepository(database,dootawas) }


    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(contactDao, dootawas)
    )

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            singleRepository = singleRepository,
            groupRepository = groupRepository,
            callRepository = callSessionRepository,
            storyRepository = storyRepository,
            knotx = dootawas,
            dootawasStore = dootawasStore
        )
    )
    val upbhogtas by homeViewModel.filteredUpbhogtaItems.collectAsState()
    val allContacts by contactViewModel.filteredContacts.collectAsState()

    var selectedContacts by remember { mutableStateOf<Set<ContactUiItem>>(emptySet()) }
    var selectedUpbhogtas by remember { mutableStateOf<Set<UpbhogtaUiItem>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }

    // ✅ Filter Upbhogtas: remove existing members
    val filteredUpbhogtaItems = remember(upbhogtas, existingMembers) {
        upbhogtas.filter {
            !it.userNumber.startsWith("@") &&
                    it.userNumber !in existingMembers
        }
    }

    // ✅ Filter phonebook contacts: remove existing members
    val filteredAddableContacts = remember(allContacts, existingMembers) {
        allContacts
            .filter { it.buttonState == ContactButtonState.ADD }
            .filter { it.number !in existingMembers }
    }

    LaunchedEffect(allContacts, existingMembers) {
        
        
//        val filtered = allContacts.filter { it.userNumber !in existingMembers }
//        
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(12.dp)
            .heightIn(max = 480.dp) // <-- LIMIT HEIGHT HERE
//            .widthIn(max = LocalConfiguration.current.screenWidthDp.dp)
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Select Contacts")
                                if (selectedContacts.isNotEmpty() || selectedUpbhogtas.isNotEmpty()) {
                                    Text(
                                        text = "Selected: ${selectedContacts.size + selectedUpbhogtas.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (selectedContacts.isNotEmpty() || selectedUpbhogtas.isNotEmpty()) {
                                IconButton(onClick = {
                                    val userNumbers = buildSet {
                                        selectedContacts.forEach { add(it.number) }
                                        selectedUpbhogtas.forEach { add(it.userNumber) }
                                    }.toList()
                                    onContactsSelected(userNumbers)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add Selected")
                                }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .wrapContentHeight()   // ← allow Scaffold to shrink when empty
            ) { padding ->
                Column(modifier = Modifier.padding(padding)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            contactViewModel.search(it)
                            homeViewModel.searchUpbhogtas(it)
                        },
                        label = { Text("Search Contacts") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier
                            .padding(8.dp)
                    )

                    LazyColumn( modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)    // ← allows shrinking when few items
                        .padding(horizontal = 8.dp)) {
                        if (filteredUpbhogtaItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Home Contacts",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(filteredUpbhogtaItems) { upbhogta ->
                                UpbhogtaItemA(
                                    upbhogta = upbhogta,
                                    isSelected = selectedUpbhogtas.contains(upbhogta),
                                    onToggleSelection = { selected ->
                                        selectedUpbhogtas = if (selected) {
                                            selectedUpbhogtas + upbhogta
                                        } else {
                                            selectedUpbhogtas - upbhogta
                                        }
                                    }
                                )
                            }
                        }

                        if (filteredAddableContacts.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = DividerDefaults.Thickness,
                                    color = DividerDefaults.color
                                )
                                Text(
                                    text = "Phone Contacts",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(filteredAddableContacts) { contact ->
                                ContactItemA(
                                    contact = contact,
                                    isSelected = selectedContacts.contains(contact),
                                    onToggleSelection = { selected ->
                                        selectedContacts =
                                            if (selected) {
                                                selectedContacts + contact
                                            } else {
                                                selectedContacts - contact
                                            }
                                    }
                                )

                            }
                        }
                    }
                    Text(
                        text = "If user not visible, first update contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp, top = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Contacts with username are not supported yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
