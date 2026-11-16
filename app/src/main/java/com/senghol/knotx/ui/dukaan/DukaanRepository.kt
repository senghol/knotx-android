package com.senghol.knotx.ui.dukaan

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID



import app.cash.sqldelight.coroutines.mapToList
import com.senghol.knotx.data.sqldelight.SengholDB


/**
 * Single access point for the `dukan` table. Nothing outside this class should
 * touch `dukanQueries` directly.
 */
class DukaanRepository(
    private val db: SengholDB,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val queries get() = db.dukaanQueries

    /**
     * Live shop profile. Makes sure the single row exists before the first
     * read, so collectors never see "no row" and the DB is never empty.
     */
    val profile: Flow<DukaanProfile> = flow {
        withContext(io) { queries.ensureRow() }
        emitAll(queries.getDukan(::mapProfile).asFlow().mapToOne(io))
    }

    /** Saves business details, defaults and invoice format together. */
    suspend fun saveProfile(p: DukaanProfile) = withContext(io) {
        db.transaction {
            queries.updateProfile(
                name = p.name,
                gst = p.gst,
                phone = p.phone,
                email = p.email,
                address = p.address,
                upiId = p.upiId,
                defaultTaxPercent = p.defaultTaxPercent,
                defaultNotes = p.defaultNotes,
                updatedAt = p.updatedAt,
            )
            queries.updateInvoiceFormat(
                prefix = p.invoicePrefix,
                padding = p.invoicePadding.toLong(),
                nextNumber = p.nextInvoiceNumber,
                updatedAt = p.updatedAt,
            )
        }
    }

    /**
     * Returns the next invoice number (e.g. "INV-014") AND advances the counter,
     * read + increment inside ONE transaction so two quick sends can't get the
     * same number. Call only when a bill is actually being sent.
     */
    suspend fun reserveNextInvoiceNumber(): String = withContext(io) {
        db.transactionWithResult {
            val current = queries.getDukan(::mapProfile).executeAsOne()
            queries.incrementInvoiceNumber()
            current.nextInvoice
        }
    }

    // ---- Customers ("Bill to") ---------------------------------------------

    /** Saved details for this contact, or null if none saved yet. */
    fun customer(contactId: String): Flow<DukaanCustomer?> =
        db.customerQueries.getCustomer(contactId, ::mapCustomer).asFlow().mapToOneOrNull(io)

    suspend fun saveCustomer(c: DukaanCustomer): Unit = withContext(io) {
        db.customerQueries.upsertCustomer(
            contactId = c.contactId,
            name = c.name.trim(),
            gst = c.gst.trim().uppercase(),
            phone = c.phone.trim(),
            email = c.email.trim(),
            address = c.address.trim(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    // ---- Item catalog -------------------------------------------------------

    suspend fun searchItems(query: String): List<CatalogItem> = withContext(io) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else db.catalogItemQueries.searchItems(q, ::mapItem).executeAsList()
    }

    /** Inserts a new item, or updates hsn/price if the name already exists. */
    suspend fun saveItem(name: String, hsn: String, price: Double): Unit = withContext(io) {
        val clean = name.trim()
        if (clean.isEmpty()) return@withContext
        val key = clean.lowercase()
        val now = System.currentTimeMillis()
        db.transaction {
            val q = db.catalogItemQueries
            if (q.getByKey(key, ::mapItem).executeAsOneOrNull() == null) {
                q.insertItem(UUID.randomUUID().toString(), clean, key, hsn.trim(), price, now)
            } else {
                q.updateItem(clean, hsn.trim(), price, now, key)
            }
        }
    }

    /** Bumps use-count for saved items that appeared on a sent bill. Unknown names are ignored. */
    suspend fun recordItemsUsed(names: List<String>): Unit = withContext(io) {
        db.transaction {
            names.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { db.catalogItemQueries.bumpUse(it) }
        }
    }

    /** All saved customers, A to Z. */
    fun allCustomers(): Flow<List<DukaanCustomer>> =
        db.customerQueries.getAllCustomers(::mapCustomer).asFlow().mapToList(io)

    suspend fun deleteCustomer(contactId: String): Unit = withContext(io) {
        db.customerQueries.deleteCustomer(contactId)
    }

    /** All saved items, A to Z. */
    fun allItems(): Flow<List<CatalogItem>> =
        db.catalogItemQueries.getAllItems(::mapItem).asFlow().mapToList(io)

    suspend fun deleteItem(id: String): Unit = withContext(io) {
        db.catalogItemQueries.deleteItem(id)
    }

    /**
     * id == null -> add a new item, otherwise edit that item.
     * Throws (message is user-readable) if the name is empty or already used by another item.
     */
    suspend fun saveItemById(id: String?, name: String, hsn: String, price: Double): Unit = withContext(io) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Item name is required" }
        val key = clean.lowercase()
        val now = System.currentTimeMillis()
        db.transaction {
            val q = db.catalogItemQueries
            val existing = q.getByKey(key, ::mapItem).executeAsOneOrNull()
            if (id == null) {
                check(existing == null) { "\"$clean\" is already saved" }
                q.insertItem(UUID.randomUUID().toString(), clean, key, hsn.trim(), price, now)
            } else {
                check(existing == null || existing.id == id) { "Another item is already named \"$clean\"" }
                q.updateItemById(clean, key, hsn.trim(), price, now, id)
            }
        }
    }

    private fun mapCustomer(
        contactId: String,
        name: String,
        gst: String,
        phone: String,
        email: String,
        address: String,
        updatedAt: Long,
    ) = DukaanCustomer(contactId, name, gst, phone, email, address)

    @Suppress("UNUSED_PARAMETER")
    private fun mapItem(
        id: String,
        name: String,
        nameKey: String,
        hsn: String,
        price: Double,
        useCount: Long,
        updatedAt: Long,
    ) = CatalogItem(id, name, hsn, price)

    // Parameters follow the column order of the `dukan` table exactly.
    @Suppress("UNUSED_PARAMETER")
    private fun mapProfile(
        id: Long,
        name: String,
        gst: String,
        phone: String,
        email: String,
        address: String,
        upiId: String,
        invoicePrefix: String,
        invoicePadding: Long,
        nextInvoiceNumber: Long,
        defaultTaxPercent: Double,
        defaultNotes: String,
        updatedAt: Long,
    ) = DukaanProfile(
        name = name,
        gst = gst,
        phone = phone,
        email = email,
        address = address,
        upiId = upiId,
        invoicePrefix = invoicePrefix,
        invoicePadding = invoicePadding.toInt(),
        nextInvoiceNumber = nextInvoiceNumber,
        defaultTaxPercent = defaultTaxPercent,
        defaultNotes = defaultNotes,
        updatedAt = updatedAt,
    )
}