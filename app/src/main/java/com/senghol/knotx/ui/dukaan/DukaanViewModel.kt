package com.senghol.knotx.ui.dukaan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// TODO: adjust this import to wherever KnotxApp lives.
import com.senghol.knotx.KnotxApp


/**
 * ONE view model for everything shop-related:
 *  - the Dukaan (shop details) screen   -> [form], [errors], [hasChanges], [save], [events]
 *  - the bill screen                    -> [profile], [reserveInvoiceNumber]
 *
 * Get it in both places with `viewModel()` (same Activity owner = same instance).
 */
class DukaanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DukaanRepository((app as KnotxApp).dootawasDB)

    // ---- Saved data (null until the first DB read finishes) ----------------

    val profile: StateFlow<DukaanProfile?> = repo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Edit-screen state --------------------------------------------------

    private val _form = MutableStateFlow<DukaanForm?>(null)
    val form: StateFlow<DukaanForm?> = _form.asStateFlow()

    private val _errors = MutableStateFlow(DukaanFormErrors())
    val errors: StateFlow<DukaanFormErrors> = _errors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** True when the form differs from what's saved (drives Save enabled + discard prompt). */
    val hasChanges: StateFlow<Boolean> = combine(profile, _form) { saved, f ->
        saved != null && f != null && f != DukaanForm.from(saved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _events = Channel<DukaanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // What the form looked like the last time it was synced from the DB.
    private var lastSynced: DukaanForm? = null

    init {
        // Keep the form in step with the DB, but ONLY while the user has no unsaved
        // edits - so a save made elsewhere (e.g. from the bill screen) shows up here,
        // yet whatever is being typed is never overwritten.
        viewModelScope.launch {
            repo.profile.collect { p ->
                val fresh = DukaanForm.from(p)
                val current = _form.value
                if (current == null || current == lastSynced) _form.value = fresh
                lastSynced = fresh
            }
        }
    }

    fun onFormChange(transform: (DukaanForm) -> DukaanForm) {
        _form.update { current -> current?.let(transform) }
    }

    fun save() {
        val f = _form.value ?: return
        val saved = profile.value ?: return
        if (_isSaving.value) return

        val result = f.validate()
        _errors.value = result
        if (result.hasErrors) return

        viewModelScope.launch {
            _isSaving.value = true
            val updated = f.toProfile(saved)
            runCatching { repo.saveProfile(updated) }
                .onSuccess {
                    _form.value = DukaanForm.from(updated)
                    _events.send(DukaanEvent.Saved)
                }
                .onFailure { _events.send(DukaanEvent.SaveFailed(it.message ?: "Could not save")) }
            _isSaving.value = false
        }
    }

    /** Throw away edits and go back to what's saved. */
    fun discardChanges() {
        profile.value?.let { _form.value = DukaanForm.from(it) }
        _errors.value = DukaanFormErrors()
    }

    // ---- Used by the bill screen -------------------------------------------

    // ---- Used by the management tabs (customers / items) --------------------

    val customers: StateFlow<List<DukaanCustomer>> = repo.allCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedItems: StateFlow<List<CatalogItem>> = repo.allItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun deleteCustomer(contactId: String) = repo.deleteCustomer(contactId)

    suspend fun deleteItem(id: String) = repo.deleteItem(id)

    /** id == null adds a new item, otherwise edits it. Returns an error message, or null on success. */
    suspend fun saveCatalogItem(id: String?, name: String, hsn: String, price: Double): String? =
        runCatching { repo.saveItemById(id, name, hsn, price) }
            .fold(onSuccess = { null }, onFailure = { it.message ?: "Could not save" })

    /** Saved "Bill to" details for a contact (null = nothing saved yet). */
    fun customer(contactId: String) = repo.customer(contactId)

    suspend fun saveCustomer(c: DukaanCustomer) = repo.saveCustomer(c)

    /** Saved items matching what's being typed (max 5, most used first). */
    suspend fun suggestItems(query: String): List<CatalogItem> = repo.searchItems(query)

    suspend fun saveItem(name: String, hsn: String, price: Double) = repo.saveItem(name, hsn, price)

    suspend fun recordItemsUsed(names: List<String>) = repo.recordItemsUsed(names)

    /** "Save to shop profile" from the bill screen's Bill-from section. */
    suspend fun saveSellerFromBill(
        name: String, gst: String, phone: String, email: String, address: String,
    ) {
        val current = profile.value ?: return
        repo.saveProfile(
            current.copy(
                name = name.trim(),
                gst = gst.trim().uppercase(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Returns the invoice number to put on the bill being sent and advances the
     * counter atomically. Call when the bill is actually sent, not when the
     * screen opens.
     */
    suspend fun reserveInvoiceNumber(): String = repo.reserveNextInvoiceNumber()
}