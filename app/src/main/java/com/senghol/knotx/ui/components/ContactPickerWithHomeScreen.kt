package com.senghol.knotx.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
import com.senghol.knotx.utils.data.GroupUiItem
import com.senghol.knotx.utils.data.UpbhogtaUiItem

import com.senghol.knotx.utils.enums.ContactButtonState
import com.senghol.knotx.utils.enums.SettingPrefKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerWithHomeScreen(
    onContactsSelected: (List<Pair<String, Boolean>>) -> Unit, // 👈 Pair(id, isGroup)
    onBack: () -> Unit
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

    val storyRepository = remember { StoryRepository(database,dootawas) }

    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(contactDao, dootawas)
    )
    val dootawasStore = remember { application.dootawasStore }

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
    var selectedContactNumbers by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    val upbhogtas by homeViewModel.filteredUpbhogtaItems.collectAsState()
    val groups by homeViewModel.filteredGroupItems.collectAsState()
    val allContacts by contactViewModel.filteredContacts.collectAsState()

    var selectedContacts by remember { mutableStateOf<Set<ContactUiItem>>(emptySet()) }
    var selectedUpbhogtas by remember { mutableStateOf<Set<UpbhogtaUiItem>>(emptySet()) }
    var selectedGroups by remember { mutableStateOf<Set<GroupUiItem>>(emptySet()) }

    var searchQuery by remember { mutableStateOf("") }

    val addableContacts = remember(allContacts) {
        allContacts.filter { it.buttonState == ContactButtonState.ADD }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Contacts")
                        if (selectedContacts.isNotEmpty() || selectedUpbhogtas.isNotEmpty() || selectedGroups.isNotEmpty()) {
                            Text(
                                text = "Selected: ${selectedContacts.size + selectedUpbhogtas.size + selectedGroups.size}",
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
                    if (selectedContacts.isNotEmpty() || selectedUpbhogtas.isNotEmpty() || selectedGroups.isNotEmpty()) {
                        IconButton(onClick = {
                            val selectedList = buildList {
                                selectedContacts.forEach { add(it.number to false) }     // 👈 false → not group
                                selectedUpbhogtas.forEach { add(it.userNumber to false) } // 👈 false → user
                                selectedGroups.forEach { add(it.id to true) }             // 👈 true → group
                            }

                            onContactsSelected(selectedList)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Selected")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    contactViewModel.search(it)
                    homeViewModel.searchUpbhogtas(it)
                    homeViewModel.searchGroups(it)
                },
                label = { Text("Search Contacts & Groups") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // Home contacts
                if (upbhogtas.isNotEmpty()) {
                    item {
                        Text(
                            text = "Home Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(upbhogtas) { upbhogta ->
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

                // Groups
                if (groups.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        Text(
                            text = "Groups",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(groups) { group ->
                        GroupItemA(
                            group = group,
                            isSelected = selectedGroups.contains(group),
                            onToggleSelection = { selected ->
                                selectedGroups = if (selected) {
                                    selectedGroups + group
                                } else {
                                    selectedGroups - group
                                }
                            }
                        )
                    }
                }

                // Device contacts
                if (addableContacts.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        Text(
                            text = "Device Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(addableContacts) { contact ->
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectorScreen(
    onBack: () -> Unit
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
    val storyRepository = remember { StoryRepository(database,dootawas) }


    val privacyList by dootawas
        .observeSetting(SettingPrefKeys.PRIVACY_LIST, emptySet())
        .collectAsState()

    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(contactDao, dootawas)
    )
    val dootawasStore = remember { application.dootawasStore }
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
    val allContacts by contactViewModel.filteredContacts.collectAsState()


    val upbhogtas by homeViewModel.filteredUpbhogtaItems.collectAsState()
    val groups by homeViewModel.filteredGroupItems.collectAsState()
    val addableContacts = remember(allContacts) {
        allContacts
            .filter { it.buttonState == ContactButtonState.ADD }
            .distinctBy { it.number }
    }


    var selectedContacts by remember { mutableStateOf<Set<ContactUiItem>>(emptySet()) }
    var selectedUpbhogtas by remember { mutableStateOf<Set<UpbhogtaUiItem>>(emptySet()) }
    var selectedGroups by remember { mutableStateOf<Set<GroupUiItem>>(emptySet()) }

    var searchQuery by remember { mutableStateOf("") }



    LaunchedEffect(privacyList, addableContacts, upbhogtas, groups) {
        

        val newSelectedContacts = mutableSetOf<ContactUiItem>()
        val newSelectedUpbhogtas = mutableSetOf<UpbhogtaUiItem>()
        val newSelectedGroups = mutableSetOf<GroupUiItem>()

        privacyList.forEach { value ->

            // 1️⃣ Check Upbhogta
            upbhogtas.firstOrNull { it.userNumber == value }?.let {
                newSelectedUpbhogtas.add(it)
                return@forEach
            }

            // 2️⃣ Check Group
            groups.firstOrNull { it.id == value }?.let {
                newSelectedGroups.add(it)
                return@forEach
            }

            // 3️⃣ Check Contact (number-wise unique already)
            addableContacts.firstOrNull { it.number == value }?.let {
                newSelectedContacts.add(it)
            }
        }

        selectedContacts = newSelectedContacts
        selectedUpbhogtas = newSelectedUpbhogtas
        selectedGroups = newSelectedGroups
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Contacts")
                        if (selectedContacts.isNotEmpty() || selectedUpbhogtas.isNotEmpty() || selectedGroups.isNotEmpty()) {
                            Text(
                                text = "Selected: ${selectedContacts.size + selectedUpbhogtas.size + selectedGroups.size}",
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
                    if (selectedContacts.isNotEmpty() || selectedUpbhogtas.isNotEmpty() || selectedGroups.isNotEmpty()) {
                        IconButton(onClick = {

                            

                            
                            selectedContacts.forEach {
                                
                            }

                            
                            selectedUpbhogtas.forEach {
                                
                            }

                            
                            selectedGroups.forEach {
                                
                            }

                            val updatedSet = buildSet {
                                selectedContacts.forEach { add(it.number) }
                                selectedUpbhogtas.forEach { add(it.userNumber) }
                                selectedGroups.forEach { add(it.id) }
                            }
                            val oldPrivacySet = privacyList.toSet()
                            val added = updatedSet - oldPrivacySet
                            val removed = oldPrivacySet - updatedSet

                            val contactsArray = JsonArray()

                            added.forEach { number ->
                                val obj = JsonObject().apply {
                                    addProperty("userNumber", number)
                                    addProperty("add", true)
                                }
                                contactsArray.add(obj)
                            }

                            removed.forEach { number ->
                                val obj = JsonObject().apply {
                                    addProperty("userNumber", number)
                                    addProperty("add", false)
                                }
                                contactsArray.add(obj)
                            }

                            val payload = JsonObject().apply {
                                add("contacts", contactsArray)
                            }

                            

                            dootawas.updateServerContacts(payload)


                            
                            updatedSet.forEach {
                                
                            }

                            dootawas.updateStringSetSetting(
                                SettingPrefKeys.PRIVACY_LIST,
                                updatedSet
                            )
                            
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Save List")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    contactViewModel.search(it)
                    homeViewModel.searchUpbhogtas(it)
                    homeViewModel.searchGroups(it)
                },
                label = { Text("Search Contacts & Groups") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // Home contacts
                if (upbhogtas.isNotEmpty()) {
                    item {
                        Text(
                            text = "Home Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(upbhogtas) { upbhogta ->
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

                // Groups
                if (groups.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        Text(
                            text = "Groups",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(groups) { group ->
                        GroupItemA(
                            group = group,
                            isSelected = selectedGroups.contains(group),
                            onToggleSelection = { selected ->
                                selectedGroups = if (selected) {
                                    selectedGroups + group
                                } else {
                                    selectedGroups - group
                                }
                            }
                        )
                    }
                }

                // Device contacts
                if (addableContacts.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        Text(
                            text = "Device Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(addableContacts) { contact ->
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
        }
    }
}


@Composable
fun UpbhogtaItemA(
    upbhogta: UpbhogtaUiItem,
    isSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection(!isSelected) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (!upbhogta.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = upbhogta.imageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {

            Text(
                text = upbhogta.name.ifBlank { upbhogta.userNumber },
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = upbhogta.userNumber,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}


@Composable
fun ContactItemA(
    contact: ContactUiItem,
    isSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection(!isSelected) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (!contact.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = contact.imageUrl,
                contentDescription = "Contact Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Contact",
                tint = Color(0xFF3F51B5),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = contact.name,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = contact.number,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun GroupItemA(
    group: GroupUiItem,
    isSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection(!isSelected) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (!group.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = group.imageUrl,
                contentDescription = "Group Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Group",
                tint = Color(0xFF009688),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = group.name,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!group.description.isNullOrBlank()) {
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

