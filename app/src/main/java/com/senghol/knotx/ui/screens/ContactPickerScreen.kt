package com.senghol.knotx.ui.screens


// 🧩 Compose imports
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person

// 🧩 Android imports
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send

// 🧩 Accompanist permissions

// 🧩 Jetpack Navigation (only if you are using NavController for onBack)
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.ui.viewmodel.ContactViewModel
import com.senghol.knotx.ui.viewmodel.ContactViewModelFactory
import com.senghol.knotx.utils.data.ContactUiItem
import com.senghol.knotx.utils.enums.DootawasMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    onContactsSelected: (JsonObject) -> Unit,  // Updated to take a JsonObject
    onBack: () -> Unit
) {
    val database = DootawasDatabase.getDatabase(LocalContext.current)
    val contactDao = database.contactDao()
    val context = LocalContext.current
    val application = context.applicationContext as KnotxApp
    val dakiya = remember { application.knotx }

    val viewModel: ContactViewModel = viewModel(factory = ContactViewModelFactory(contactDao,dakiya))

    val contacts by viewModel.filteredContacts.collectAsState()
    var selectedContacts by remember { mutableStateOf<Set<ContactUiItem>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Contacts")
                        if (selectedContacts.isNotEmpty()) {
                            Text(
                                text = "Selected: ${selectedContacts.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedContacts.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                // Convert selectedContacts to JSON format
                                val jsonContacts = selectedContacts.map { contact ->
                                    JsonObject().apply {
                                        addProperty(DootawasMessage.CONTACT_NAME, contact.name)
                                        addProperty(DootawasMessage.NUMBER, contact.number)
                                    }
                                }

                                // Wrap it in a final JSON object with the count
                                val finalJson = JsonObject().apply {
                                    addProperty("count", jsonContacts.size)
                                    add(DootawasMessage.CONTACTS, JsonArray().apply {
                                        jsonContacts.forEach { add(it) }
                                    })
                                }

                                // Call the onContactsSelected with the JSON object
                                onContactsSelected(finalJson)
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Selected")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.search(it)
                },
                label = { Text("Search Contacts") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // Contact List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = selectedContacts.contains(contact),
                        onToggleSelection = { selected ->
                            selectedContacts = if (selected) {
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


@Composable
fun ContactItem(
    contact: ContactUiItem,
    isSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection(!isSelected) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Contact",
            tint = Color(0xFF3F51B5),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = contact.name, fontWeight = FontWeight.Bold)
            Text(text = contact.number, style = MaterialTheme.typography.bodySmall)
        }
    }
}


