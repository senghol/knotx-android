package com.senghol.knotx.ui.contactPicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn


data class PickerSections(
    val numberContacts: List<PickedContact> = emptyList(),
    val usernameContacts: List<PickedContact> = emptyList(),
    val groups: List<PickedContact> = emptyList(),
    val deviceContacts: List<PickedContact> = emptyList(),
    val groupMembers: List<PickedContact> = emptyList()
) {
    val isEmpty: Boolean
        get() = numberContacts.isEmpty() &&
                usernameContacts.isEmpty() &&
                groups.isEmpty() &&
                deviceContacts.isEmpty() &&
                groupMembers.isEmpty()

    fun all(): List<PickedContact> =
        (numberContacts + usernameContacts + groups + deviceContacts + groupMembers)
            .distinctBy { it.userNumber }
}

data class PickerUiState(
    val query: String = "",
    val sections: PickerSections = PickerSections(),
    val selected: Set<String> = emptySet(),
    val selectedItems: List<PickedContact> = emptyList()
)

class ContactPickerViewModel(
    repository: ContactPickerRepository,
    sources: PickerSources,
    initialSelected: Set<String>,
    initialItems: List<PickedContact> = emptyList()
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selected = MutableStateFlow(
        initialSelected + initialItems.map { it.userNumber }
    )

    private val allSections: StateFlow<PickerSections> = combine(
        if (sources.numberContacts) repository.numberContacts() else flowOf(emptyList()),
        if (sources.usernameContacts) repository.usernameContacts() else flowOf(emptyList()),
        if (sources.groups) repository.groups() else flowOf(emptyList()),
        if (sources.deviceContacts) repository.deviceContacts() else flowOf(emptyList()),
        if (sources.groupMembers) repository.groupMembers(sources.groupId) else flowOf(emptyList())
    ) { numbers, usernames, groups, device, members ->
        val n = numbers.withExtras(initialItems.filter { !it.isGroup && !it.isUsernameId })
        val u = usernames.withExtras(initialItems.filter { it.isUsernameId })
        val g = groups.withExtras(initialItems.filter { it.isGroup })

        // Prefer home and username contacts over device contacts.
        val taken = (n + u).map { it.userNumber }.toSet()
        val d = device.filter { it.userNumber !in taken }

        // Avoid showing a person twice across home, username, device and group-member sections.
        val memberIds = (n + u + d).map { it.userNumber }.toSet()
        val m = members.filter { it.userNumber !in memberIds }

        PickerSections(
            numberContacts = n,
            usernameContacts = u,
            groups = g,
            deviceContacts = d,
            groupMembers = m
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PickerSections()
    )

    val uiState: StateFlow<PickerUiState> = combine(
        allSections, query, selected
    ) { all, q, sel ->
        val needle = q.trim()

        fun List<PickedContact>.matching(): List<PickedContact> =
            if (needle.isEmpty()) this
            else filter {
                it.userName.contains(needle, ignoreCase = true) ||
                        it.userNumber.contains(needle, ignoreCase = true)
            }

        PickerUiState(
            query = q,
            sections = PickerSections(
                numberContacts = all.numberContacts.matching(),
                usernameContacts = all.usernameContacts.matching(),
                groups = all.groups.matching(),
                deviceContacts = all.deviceContacts.matching(),
                groupMembers = all.groupMembers.matching()
            ),
            selected = sel,
            selectedItems = all.all().filter { it.userNumber in sel }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PickerUiState(
            selected = initialSelected + initialItems.map { it.userNumber }
        )
    )

    fun setQuery(q: String) {
        query.value = q
    }

    fun toggle(id: String) {
        selected.value =
            if (id in selected.value) selected.value - id
            else selected.value + id
    }

    fun result(): List<PickedContact> =
        allSections.value.all().filter { it.userNumber in selected.value }

    class Factory(
        private val repository: ContactPickerRepository,
        private val sources: PickerSources,
        private val initialSelected: Set<String>,
        private val initialItems: List<PickedContact> = emptyList()
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ContactPickerViewModel(
                repository, sources, initialSelected, initialItems
            ) as T
    }

    private fun List<PickedContact>.withExtras(
        extras: List<PickedContact>
    ): List<PickedContact> {
        val ids = extras.map { it.userNumber }.toSet()
        return extras + filter { it.userNumber !in ids }
    }
}
