package com.senghol.knotx.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.ContactDao
import com.senghol.knotx.utils.data.ContactUiItem
import com.senghol.knotx.utils.data.toUiItem
import com.senghol.knotx.utils.enums.ContactButtonState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class ContactViewModel(
    private val contactDao: ContactDao,
    private val knotx: Knotx
) : ViewModel() {

    // ─────────────── Loading ───────────────

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ─────────────── Contacts ───────────────

    private val _searchQuery = MutableStateFlow("")

    // Full, unfiltered list (single source of truth)
    private val allContactsUi: StateFlow<List<ContactUiItem>> =
        contactDao.getAllContactsFlow()
            .map { list -> list.map { it.toUiItem() } }
            .onEach { _isLoading.value = false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Filtered list for the screen
    val filteredContacts: StateFlow<List<ContactUiItem>> =
        combine(allContactsUi, _searchQuery) { all, query ->
            if (query.isEmpty()) {
                all
            } else {
                all.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.number.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Contacts who use the app (ADD or ADDED), counted from the FULL list
    val appUserCount: StateFlow<Int> =
        allContactsUi
            .map { list ->
                list.count {
                    it.buttonState == ContactButtonState.ADD ||
                            it.buttonState == ContactButtonState.ADDED
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ─────────────── Actions ───────────────

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun updateContactState(contact: ContactUiItem, invite: Boolean) {
        viewModelScope.launch {
            if (invite) {
                knotx.sendInviteSms(contact.number)
            } else {
                knotx.addContactToUpbhogta(contact)
            }
            val newState = when (contact.buttonState) {
                ContactButtonState.INVITE -> ContactButtonState.INVITED
                ContactButtonState.ADD -> ContactButtonState.ADDED
                else -> return@launch
            }
            contactDao.updateContactStateByNumber(contact.number, newState)
        }
    }

    // ─────────────── Refresh ───────────────

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refreshContacts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            knotx.readContacts(true)
            delay(4000)
            _isRefreshing.value = false
        }
    }

    fun readContacts() {
        viewModelScope.launch {
            knotx.readContacts(false)
        }
    }

    fun addItemToUpdate(number: String) {
        viewModelScope.launch {
            knotx.addItemToUpdate(number)
        }
    }
}
