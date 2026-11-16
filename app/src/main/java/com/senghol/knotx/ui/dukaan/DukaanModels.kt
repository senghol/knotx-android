package com.senghol.knotx.ui.dukaan

/** The saved shop profile (one row in the `dukan` table). */
data class DukaanProfile(
    val name: String = "",
    val gst: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val upiId: String = "",
    val invoicePrefix: String = "INV-",
    val invoicePadding: Int = 3,
    val nextInvoiceNumber: Long = 1,
    val defaultTaxPercent: Double = 0.0,
    val defaultNotes: String = "",
    val updatedAt: Long = 0,
) {
    /** True once the owner has entered at least a shop name. */
    val isSet: Boolean get() = name.isNotBlank()

    fun formatInvoice(number: Long = nextInvoiceNumber): String =
        invoicePrefix + number.toString().padStart(invoicePadding, '0')

    /** What the next bill will be numbered, e.g. "INV-014". */
    val nextInvoice: String get() = formatInvoice()
}

/**
 * Text-only copy of the profile used by the edit screen. Everything is a String
 * so half-typed input ("12.", "") never gets snapped back while typing.
 */
data class DukaanForm(
    val name: String = "",
    val gst: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val upiId: String = "",
    val invoicePrefix: String = "",
    val invoicePadding: String = "",
    val nextInvoiceNumber: String = "",
    val defaultTaxPercent: String = "",
    val defaultNotes: String = "",
) {
    companion object {
        fun from(p: DukaanProfile) = DukaanForm(
            name = p.name,
            gst = p.gst,
            phone = p.phone,
            email = p.email,
            address = p.address,
            upiId = p.upiId,
            invoicePrefix = p.invoicePrefix,
            invoicePadding = p.invoicePadding.toString(),
            nextInvoiceNumber = p.nextInvoiceNumber.toString(),
            defaultTaxPercent = if (p.defaultTaxPercent == 0.0) "" else p.defaultTaxPercent.toString(),
            defaultNotes = p.defaultNotes,
        )
    }

    fun validate(): DukaanFormErrors {
        val pad = invoicePadding.toIntOrNull()
        val next = nextInvoiceNumber.toLongOrNull()
        val tax = if (defaultTaxPercent.isBlank()) 0.0 else defaultTaxPercent.toDoubleOrNull()

        return DukaanFormErrors(
            name = if (name.isBlank()) "Shop name is required" else null,
            gst = gstError(gst),
            phone = phoneError(phone),
            email = emailError(email),
            upiId = if (upiId.isNotBlank() && !upiId.contains('@')) "UPI ID looks like name@bank" else null,
            invoicePrefix = if (invoicePrefix.length > 12) "Max 12 characters" else null,
            invoicePadding = if (pad == null || pad !in 1..8) "Use 1 to 8" else null,
            nextInvoiceNumber = if (next == null || next < 1) "Must be 1 or more" else null,
            defaultTaxPercent = if (tax == null || tax !in 0.0..100.0) "Use 0 to 100" else null,
        )
    }

    /** Call only after [validate] has no errors. */
    fun toProfile(base: DukaanProfile): DukaanProfile = base.copy(
        name = name.trim(),
        gst = gst.trim().uppercase(),
        phone = phone.trim(),
        email = email.trim(),
        address = address.trim(),
        upiId = upiId.trim(),
        invoicePrefix = invoicePrefix,
        invoicePadding = invoicePadding.toInt(),
        nextInvoiceNumber = nextInvoiceNumber.toLong(),
        defaultTaxPercent = if (defaultTaxPercent.isBlank()) 0.0 else defaultTaxPercent.toDouble(),
        defaultNotes = defaultNotes.trim(),
        updatedAt = System.currentTimeMillis(),
    )
}

data class DukaanFormErrors(
    val name: String? = null,
    val gst: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val upiId: String? = null,
    val invoicePrefix: String? = null,
    val invoicePadding: String? = null,
    val nextInvoiceNumber: String? = null,
    val defaultTaxPercent: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(
            name, gst, phone, email, upiId,
            invoicePrefix, invoicePadding, nextInvoiceNumber, defaultTaxPercent,
        ).any { it != null }
}

/** Saved "Bill to" details for one contact. */
data class DukaanCustomer(
    val contactId: String,
    val name: String = "",
    val gst: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
)

/** A saved item shown as a hint while typing a bill line. */
data class CatalogItem(
    val id: String,
    val name: String,
    val hsn: String,
    val price: Double,
)

sealed interface DukaanEvent {
    data object Saved : DukaanEvent
    data class SaveFailed(val message: String) : DukaanEvent
}

private val GST_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

// Shared by the details form and the customer edit dialog. Blank = fine (all optional).
fun gstError(v: String): String? =
    if (v.isNotBlank() && !GST_REGEX.matches(v.trim().uppercase())) "Enter a valid 15-character GSTIN" else null

fun phoneError(v: String): String? =
    if (v.isNotBlank() && v.count { it.isDigit() } !in 7..15) "Enter a valid phone number" else null

fun emailError(v: String): String? =
    if (v.isNotBlank() && !EMAIL_REGEX.matches(v.trim())) "Enter a valid email" else null