package com.senghol.knotx.ui.components
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.senghol.knotx.ui.dukaan.Accent
import com.senghol.knotx.ui.dukaan.BillTheme
import com.senghol.knotx.ui.dukaan.CatalogItem
import com.senghol.knotx.ui.dukaan.DarkBillTheme
import com.senghol.knotx.ui.dukaan.DukaanCustomer
import com.senghol.knotx.ui.dukaan.DukaanViewModel
import com.senghol.knotx.ui.dukaan.LightBillTheme
import com.senghol.knotx.utils.enums.DootawasMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.statusBarsPadding

// ─── Data Models ───────────────────────────────────────────────────────────────
// ─── Data Models ───────────────────────────────────────────────────────────────
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import com.senghol.knotx.ui.dialogs.bringIntoViewOnFocus

/**
 * When ANY child field inside this container gets focus, scroll so the
 * WHOLE container is visible above the keyboard (not just the focused field).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringWholeIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.hasFocus) {
                scope.launch {
                    delay(250) // let the keyboard animation finish and the viewport shrink
                    requester.bringIntoView()
                }
            }
        }
}

enum class LedgerType(val value: Int) {
    BILL(0),
    CREDIT(1),
    DEBIT(2),
    ADJUSTMENT(3),
    LEDGER_ACK_BILL_CREDIT(4), // For confirming Bills and Credits
    LEDGER_ACK_DEBIT(5);       // For confirming Debits

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: BILL
    }
}

enum class LedgerStatus(val value: Int) {
    PENDING(0),
    ACCEPTED(1),
    REJECTED(2);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: Int = 1,
    val hsnCode: String = "",
    val price: Double = 0.0
) {
    val total: Double get() = quantity * price
}

data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val contactName: String,
    val items: List<BillItem>,
    val taxPercent: Double,
    val notes: String,
    val previousBalance: Double,
    val amountPaid: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double get() = items.sumOf { it.total }
    val taxAmount: Double get() = subtotal * (taxPercent / 100.0)
    val billTotal: Double get() = subtotal + taxAmount
    val newBalance: Double get() = previousBalance + billTotal - amountPaid
}

fun Bill.toJson(): String = Gson().toJson(this)
fun String.toBill(): Bill = Gson().fromJson(this, Bill::class.java)

// ─── Theme tokens ──────────────────────────────────────────────────────────────


// ─── Screen ────────────────────────────────────────────────────────────────────


private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private fun ListData.toBillItems(onlyChecked: Boolean = false): List<BillItem> =
    items
        .filter { it.text.isNotBlank() && (!onlyChecked || it.checked) }
        .map { li ->
            val qty = li.qty?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
            val unitPrice = li.price ?: 0.0
            if (qty % 1.0 == 0.0) {
                BillItem(name = li.text.trim(), quantity = qty.toInt(), price = unitPrice)
            } else {
                // BillItem.quantity is an Int, so a fractional qty (e.g. 1.5 kg) is folded
                // into the price: quantity 1, price = unit * qty. The total stays correct.
                BillItem(name = "${li.text.trim()} ($qty)", quantity = 1, price = unitPrice * qty)
            }
        }

// ---- CreateBillScreen --------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBillScreen(
    contactId: String,
    contactName: String,
    previousBalance: Double,
    darkMode: Boolean = true,
    initialList: ListData? = null,          // null = normal blank bill
    onOpenDukaan: (() -> Unit)? = null,     // shop icon is hidden when null
    onBillCreated: (JsonObject) -> Unit,
    onBack: () -> Unit
) {
    // Created HERE - the caller (chat screen) doesn't pass or own it. The Dukaan screen
    // also calls viewModel(), so both get the same instance from the same owner.
    val dukaanVm: DukaanViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDark by remember { mutableStateOf(darkMode) }
    val t = if (isDark) DarkBillTheme else LightBillTheme

    // Mandatory date - always non-null, defaults to today.
    var billDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var invoiceNumber by remember { mutableStateOf("") } // optional

    var items by remember {
        mutableStateOf(
            initialList?.toBillItems().orEmpty().ifEmpty { listOf(BillItem()) }
        )
    }
    var taxPercent by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }

    var showBusinessFields by remember { mutableStateOf(false) }
    var sellerName by remember { mutableStateOf("") }
    var sellerGst by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerEmail by remember { mutableStateOf("") }
    var sellerAddress by remember { mutableStateOf("") }

    var buyerName by remember { mutableStateOf("") }
    var buyerGst by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var buyerEmail by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }

    // ---- Saved data from the DB ---------------------------------------------

    val shop by dukaanVm.profile.collectAsState()
    val savedCustomer by remember(contactId) { dukaanVm.customer(contactId) }
        .collectAsState(initial = null)

    var shopPrefilled by remember { mutableStateOf(false) }
    var customerPrefilled by remember { mutableStateOf(false) }
    // The invoice number we suggested; if the user leaves it untouched we consume it on send.
    var suggestedInvoice by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    // Fill from the saved shop ONCE. Never overwrites anything the user already typed.
    LaunchedEffect(shop) {
        val s = shop ?: return@LaunchedEffect
        if (shopPrefilled) return@LaunchedEffect
        shopPrefilled = true
        if (sellerName.isBlank()) sellerName = s.name
        if (sellerGst.isBlank()) sellerGst = s.gst
        if (sellerPhone.isBlank()) sellerPhone = s.phone
        if (sellerEmail.isBlank()) sellerEmail = s.email
        if (sellerAddress.isBlank()) sellerAddress = s.address
        if (taxPercent.isBlank() && s.defaultTaxPercent > 0.0) {
            taxPercent = s.defaultTaxPercent.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }
        }
        if (notes.isBlank()) notes = s.defaultNotes
        if (invoiceNumber.isBlank()) {
            invoiceNumber = s.nextInvoice
            suggestedInvoice = s.nextInvoice
        }
        if (s.isSet) showBusinessFields = true
    }

    // Same for the saved customer of this contact.
    LaunchedEffect(savedCustomer) {
        val c = savedCustomer ?: return@LaunchedEffect
        if (customerPrefilled) return@LaunchedEffect
        customerPrefilled = true
        if (buyerName.isBlank()) buyerName = c.name
        if (buyerGst.isBlank()) buyerGst = c.gst
        if (buyerPhone.isBlank()) buyerPhone = c.phone
        if (buyerEmail.isBlank()) buyerEmail = c.email
        if (buyerAddress.isBlank()) buyerAddress = c.address
        showBusinessFields = true
    }

    // "Is what's on screen already saved?" drives the little bookmark icons.
    val sellerFilled = sellerName.isNotBlank()
    val sellerSaved = shop?.let {
        it.name == sellerName.trim() && it.gst == sellerGst.trim() &&
                it.phone == sellerPhone.trim() && it.email == sellerEmail.trim() &&
                it.address == sellerAddress.trim()
    } ?: false
    val sellerCanSave = sellerFilled && !sellerSaved

    val buyerFilled = listOf(buyerName, buyerGst, buyerPhone, buyerEmail, buyerAddress)
        .any { it.isNotBlank() }
    val buyerSaved = savedCustomer?.let {
        it.name == buyerName.trim() && it.gst == buyerGst.trim() &&
                it.phone == buyerPhone.trim() && it.email == buyerEmail.trim() &&
                it.address == buyerAddress.trim()
    } ?: false
    val buyerCanSave = buyerFilled && !buyerSaved

    // ---- Totals -------------------------------------------------------------

    val subtotal = items.sumOf { it.total }
    val currentTaxPercent = taxPercent.toDoubleOrNull() ?: 0.0
    val taxAmount = subtotal * (currentTaxPercent / 100.0)
    val billTotal = subtotal + taxAmount
    val paid = amountPaid.toDoubleOrNull() ?: 0.0
    val remaining = billTotal - paid
    val newBalance = previousBalance + billTotal - paid

    val scrollState = rememberScrollState()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = billDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { billDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    BackHandler {
        onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("New Bill", fontWeight = FontWeight.SemiBold, color = t.onSurface)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = t.onSurface)
                    }
                },
                actions = {
                    if (onOpenDukaan != null) {
                        IconButton(onClick = onOpenDukaan) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = "Shop details",
                                tint = t.onSurfaceMid
                            )
                        }
                    }
                    IconButton(onClick = { isDark = !isDark }) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = t.onSurfaceMid
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.surface)
            )
        },
        containerColor = t.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // -- Contact ----------------------------------------------------
            ContactCard(contactName = contactName, t = t)

            // -- Invoice # (optional) + Date (mandatory) ---------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("INVOICE NO. (OPTIONAL)", t)
                    BillTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        placeholder = "e.g. INV-2026-014",
                        t = t
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("DATE *", t)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(t.surface, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showDatePicker = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            dateFormatter.format(Date(billDateMillis)),
                            color = t.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // -- Balance summary ---------------------------------------------
            BalanceSummaryCard(
                previousBalance = previousBalance,
                billTotal = billTotal,
                newBalance = newBalance,
                t = t
            )

            // -- Business / GST toggle ---------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(t.surface, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showBusinessFields = !showBusinessFields }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BusinessCenter,
                    contentDescription = null,
                    tint = if (showBusinessFields) Accent else t.onSurfaceDim,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Business / GST Details",
                        color = if (showBusinessFields) Accent else t.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Add contact, address & GST details",
                        color = t.onSurfaceDim,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = showBusinessFields,
                    onCheckedChange = { showBusinessFields = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = t.onSurfaceDim,
                        uncheckedTrackColor = t.divider
                    )
                )
            }

            // -- Business fields (animated) ----------------------------------
            AnimatedVisibility(
                visible = showBusinessFields,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(t.surface, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionLabel("BILL FROM (YOUR BUSINESS)", t) }
                        SaveDetailsIconButton(
                            saved = sellerFilled && sellerSaved,
                            canSave = sellerCanSave,
                            label = "Save to shop profile",
                            t = t,
                        ) {
                            scope.launch {
                                dukaanVm.saveSellerFromBill(
                                    sellerName, sellerGst, sellerPhone, sellerEmail, sellerAddress
                                )
                                Toast.makeText(context, "Shop details saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    BillTextField(
                        value = sellerName,
                        onValueChange = { sellerName = it },
                        placeholder = "Business / shop name",
                        t = t
                    )
                    BillTextField(
                        value = sellerGst,
                        onValueChange = { sellerGst = it.uppercase() },
                        placeholder = "GST number (optional)",
                        t = t
                    )
                    BillTextField(
                        value = sellerPhone,
                        onValueChange = { sellerPhone = it.filter { c -> c.isDigit() || c == '+' } },
                        placeholder = "Phone number (optional)",
                        keyboardType = KeyboardType.Phone,
                        t = t
                    )
                    BillTextField(
                        value = sellerEmail,
                        onValueChange = { sellerEmail = it },
                        placeholder = "Email (optional)",
                        keyboardType = KeyboardType.Email,
                        t = t
                    )
                    BillTextField(
                        value = sellerAddress,
                        onValueChange = { sellerAddress = it },
                        placeholder = "Address (optional)",
                        singleLine = false,
                        minLines = 2,
                        t = t
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionLabel("BILL TO (CUSTOMER)", t) }
                        SaveDetailsIconButton(
                            saved = buyerFilled && buyerSaved,
                            canSave = buyerCanSave,
                            label = "Save customer",
                            t = t,
                        ) {
                            scope.launch {
                                dukaanVm.saveCustomer(
                                    DukaanCustomer(
                                        contactId,
                                        buyerName,
                                        buyerGst,
                                        buyerPhone,
                                        buyerEmail,
                                        buyerAddress
                                    )
                                )
                                Toast.makeText(context, "Customer saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    BillTextField(
                        value = buyerName,
                        onValueChange = { buyerName = it },
                        placeholder = "Name override (optional, defaults to contact)",
                        t = t
                    )
                    BillTextField(
                        value = buyerGst,
                        onValueChange = { buyerGst = it.uppercase() },
                        placeholder = "Customer GST number (optional)",
                        t = t
                    )
                    BillTextField(
                        value = buyerPhone,
                        onValueChange = { buyerPhone = it.filter { c -> c.isDigit() || c == '+' } },
                        placeholder = "Phone number (optional)",
                        keyboardType = KeyboardType.Phone,
                        t = t
                    )
                    BillTextField(
                        value = buyerEmail,
                        onValueChange = { buyerEmail = it },
                        placeholder = "Email (optional)",
                        keyboardType = KeyboardType.Email,
                        t = t
                    )
                    BillTextField(
                        value = buyerAddress,
                        onValueChange = { buyerAddress = it },
                        placeholder = "Address (optional)",
                        singleLine = false,
                        minLines = 2,
                        t = t
                    )
                }
            }

            // -- Items -------------------------------------------------------
            SectionLabel("ITEMS", t)
            items.forEachIndexed { index, item ->
                BillItemRowWithHsn(
                    item = item,
                    t = t,
                    suggest = dukaanVm::suggestItems,
                    onSaveItem = { dukaanVm.saveItem(it.name, it.hsnCode, it.price) },
                    onUpdate = { updated ->
                        items = items.toMutableList().also { it[index] = updated }
                    },
                    onRemove = if (items.size > 1) ({
                        items = items.toMutableList().also { it.removeAt(index) }
                    }) else null
                )
            }
            TextButton(
                onClick = { items = items + BillItem() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(4.dp))
                Text("Add Item", color = Accent, fontWeight = FontWeight.SemiBold)
            }

            // -- Tax ---------------------------------------------------------
            SectionLabel("TAX / GST", t)
            BillTextField(
                value = taxPercent,
                onValueChange = { taxPercent = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "e.g. 18",
                suffix = "%",
                keyboardType = KeyboardType.Decimal,
                t = t
            )

            // -- Totals ------------------------------------------------------
            TotalsCard(
                subtotal = subtotal,
                taxPercent = currentTaxPercent,
                taxAmount = taxAmount,
                billTotal = billTotal,
                t = t
            )

            // -- Payment -----------------------------------------------------
            SectionLabel("AMOUNT PAID", t)
            PaymentField(
                amountPaid = amountPaid,
                billTotal = billTotal,
                remaining = remaining,
                onValueChange = {
                    amountPaid = it.filter { c -> c.isDigit() || c == '.' }
                },
                t = t
            )

            // -- Notes -------------------------------------------------------
            SectionLabel("NOTES", t)
            BillTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Optional note to contact",
                singleLine = false,
                minLines = 3,
                t = t
            )

            Spacer(Modifier.height(4.dp))

            // -- Send --------------------------------------------------------
            val isValid = items.all { it.name.isNotBlank() && it.price > 0 }
            Button(
                onClick = {
                    scope.launch {
                        isSending = true
                        try {
                            // Consume an invoice number only if the user kept the suggested one.
                            val invoiceToUse =
                                if (invoiceNumber.isNotBlank() && invoiceNumber == suggestedInvoice)
                                    runCatching { dukaanVm.reserveInvoiceNumber() }
                                        .getOrDefault(invoiceNumber.trim())
                                else invoiceNumber.trim()

                            val dataJson = JsonObject().apply {
                                val itemsArray = JsonArray()
                                items.forEach { item ->
                                    itemsArray.add(JsonObject().apply {
                                        addProperty(DootawasMessage.BILL_ITEM_NAME,  item.name)
                                        addProperty(DootawasMessage.BILL_ITEM_HSN,   item.hsnCode)
                                        addProperty(DootawasMessage.BILL_ITEM_PRICE, item.price)
                                        addProperty(DootawasMessage.BILL_ITEM_QTY,   item.quantity)
                                        addProperty(DootawasMessage.BILL_ITEM_TOTAL, item.total)
                                    })
                                }
                                add(DootawasMessage.BILL_ITEMS,               itemsArray)
                                addProperty(DootawasMessage.BILL_TAX_PERCENT, currentTaxPercent)
                                addProperty(DootawasMessage.BILL_TAX_AMOUNT,  taxAmount)
                                addProperty(DootawasMessage.BILL_SUBTOTAL,    subtotal)
                                addProperty(DootawasMessage.BILL_AMOUNT_PAID, paid)
                                addProperty(DootawasMessage.BILL_TOTAL,       billTotal)
                                addProperty(DootawasMessage.BILL_DATE,        billDateMillis)

                                if (invoiceToUse.isNotBlank())
                                    addProperty(DootawasMessage.BILL_INVOICE_NUMBER, invoiceToUse)

                                // Business / GST fields - only if toggle is on and fields are filled
                                if (showBusinessFields) {
                                    if (sellerName.isNotBlank())
                                        addProperty(DootawasMessage.BILL_SELLER_NAME, sellerName)
                                    if (sellerGst.isNotBlank())
                                        addProperty(DootawasMessage.BILL_SELLER_GST, sellerGst)
                                    if (sellerPhone.isNotBlank())
                                        addProperty(DootawasMessage.BILL_SELLER_PHONE, sellerPhone)
                                    if (sellerEmail.isNotBlank())
                                        addProperty(DootawasMessage.BILL_SELLER_EMAIL, sellerEmail)
                                    if (sellerAddress.isNotBlank())
                                        addProperty(DootawasMessage.BILL_SELLER_ADDRESS, sellerAddress)

                                    if (buyerName.isNotBlank())
                                        addProperty(DootawasMessage.BILL_BUYER_NAME, buyerName)
                                    if (buyerGst.isNotBlank())
                                        addProperty(DootawasMessage.BILL_BUYER_GST, buyerGst)
                                    if (buyerPhone.isNotBlank())
                                        addProperty(DootawasMessage.BILL_BUYER_PHONE, buyerPhone)
                                    if (buyerEmail.isNotBlank())
                                        addProperty(DootawasMessage.BILL_BUYER_EMAIL, buyerEmail)
                                    if (buyerAddress.isNotBlank())
                                        addProperty(DootawasMessage.BILL_BUYER_ADDRESS, buyerAddress)
                                }
                            }

                            val ledgerJson = JsonObject().apply {
                                addProperty(DootawasMessage.LEDGER_TYPE,   LedgerType.BILL.value)
                                addProperty(DootawasMessage.LEDGER_STATUS, LedgerStatus.PENDING.value)
                                addProperty(DootawasMessage.LEDGER_AMOUNT,           billTotal)
                                addProperty(DootawasMessage.LEDGER_NEW_BALANCE,      newBalance)
                                addProperty(DootawasMessage.LEDGER_CONTACT_ID,       contactId)
                                addProperty(DootawasMessage.LEDGER_CONTACT_NAME,     contactName)
                                addProperty(DootawasMessage.LEDGER_PREVIOUS_BALANCE, previousBalance)
                                addProperty(DootawasMessage.LEDGER_NOTE,             notes)
                                addProperty(DootawasMessage.LEDGER_CREATED_AT,       System.currentTimeMillis())
                                add(DootawasMessage.LEDGER_DATA,                     dataJson)
                            }

                            // Float frequently used saved items to the top of future hints.
                            dukaanVm.recordItemsUsed(items.map { it.name })
                            onBillCreated(ledgerJson)
                        } finally {
                            isSending = false
                        }
                    }
                },
                enabled = isValid && !isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    disabledContainerColor = t.divider
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Send Bill",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}



@Composable
private fun SaveDetailsIconButton(
    saved: Boolean,
    canSave: Boolean,
    label: String,
    t: BillTheme,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = canSave, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
            contentDescription = label,
            tint = when {
                canSave -> Accent
                saved -> Accent.copy(alpha = 0.6f)
                else -> t.onSurfaceDim
            },
            modifier = Modifier.size(20.dp),
        )
    }
}
// ---- REPLACES the old BillItemRowWithHsn ------------------------------------

@Composable
private fun BillItemRowWithHsn(
    item: BillItem,
    t: BillTheme,
    suggest: suspend (String) -> List<CatalogItem>,
    onSaveItem: suspend (BillItem) -> Unit,
    onUpdate: (BillItem) -> Unit,
    onRemove: (() -> Unit)?
) {
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }

    // Look up saved items as the name is typed (small delay = fewer DB hits).
    LaunchedEffect(item.name, refresh) {
        val q = item.name.trim()
        if (q.isEmpty()) {
            suggestions = emptyList()
        } else {
            delay(150)
            suggestions = suggest(q)
        }
    }

    val name = item.name.trim()
    val exact = suggestions.firstOrNull { it.name.equals(name, ignoreCase = true) }
    val isSavedSame = exact != null && exact.price == item.price && exact.hsn == item.hsnCode
    val canSave = name.isNotEmpty() && item.price > 0 && !isSavedSame
    // Hints = saved items, except one that is already exactly what is typed.
    val hints = suggestions.filterNot {
        it.name.equals(name, ignoreCase = true) && it.price == item.price && it.hsn == item.hsnCode
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringWholeIntoViewOnFocus()
            .background(t.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(2f)) {
                BillTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    placeholder = "Item name",
                    t = t
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BillTextField(
                    value = item.hsnCode,
                    onValueChange = { onUpdate(item.copy(hsnCode = it)) },
                    placeholder = "HSN/SAC",
                    keyboardType = KeyboardType.Number,
                    t = t
                )
            }
            // Save to catalog: bookmark+ = new/changed, filled bookmark = already saved.
            if (name.isNotEmpty() && item.price > 0) {
                SaveDetailsIconButton(
                    saved = isSavedSame,
                    canSave = canSave,
                    label = if (exact == null) "Save item" else "Update saved item",
                    t = t,
                ) {
                    scope.launch {
                        onSaveItem(item)
                        refresh++
                    }
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove item",
                        tint = t.onSurfaceDim
                    )
                }
            }
        }

        // Saved-item hints: tap to fill name, HSN and price.
        if (hints.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hints.forEach { h ->
                    val priceText =
                        if (h.price <= 0.0) ""
                        else "  ₹" + (if (h.price % 1.0 == 0.0) h.price.toLong().toString() else "%.2f".format(h.price))
                    AssistChip(
                        onClick = { onUpdate(item.copy(name = h.name, hsnCode = h.hsn, price = h.price)) },
                        label = { Text(h.name + priceText, fontSize = 12.sp) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BillTextField(
                    value = item.quantity.toString(),
                    onValueChange = {
                        val qty = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(quantity = qty.toInt()))
                    },
                    placeholder = "Qty",
                    keyboardType = KeyboardType.Decimal,
                    t = t
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BillTextField(
                    value = if (item.price == 0.0) "" else item.price.toString(),
                    onValueChange = {
                        val price = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(price = price))
                    },
                    placeholder = "Price",
                    keyboardType = KeyboardType.Decimal,
                    t = t
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text("Total", color = t.onSurfaceDim, fontSize = 10.sp)
                Text(
                    "₹%.2f".format(item.total),
                    color = t.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ─── Payment field ─────────────────────────────────────────────────────────────

@Composable
private fun PaymentField(
    amountPaid: String,
    billTotal: Double,
    remaining: Double,
    onValueChange: (String) -> Unit,
    t: BillTheme
) {
    val paid = amountPaid.toDoubleOrNull() ?: 0.0
    val isOverpaid = paid > billTotal && billTotal > 0
    val isFullyPaid = paid >= billTotal && billTotal > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus()
            .background(t.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = amountPaid,
            onValueChange = onValueChange,
            placeholder = {
                Text("0.00", color = t.onSurfaceDim, fontSize = 15.sp)
            },
            prefix = { Text("₹", color = t.onSurfaceMid, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = t.fieldBorder,
                cursorColor = Accent,
                focusedContainerColor = t.fieldContainer,
                unfocusedContainerColor = t.fieldContainer,
                focusedTextColor = t.onSurface,
                unfocusedTextColor = t.onSurface
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                color = t.onSurface
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isOverpaid -> {
                    StatusChip(
                        label = "Overpaid by ₹%.2f".format(paid - billTotal),
                        color = Color(0xFF2196F3)
                    )
                }
                isFullyPaid -> {
                    StatusChip(label = "Fully paid ✓", color = Color(0xFF4CAF50))
                }
                paid > 0 -> {
                    StatusChip(
                        label = "Remaining ₹%.2f".format(remaining),
                        color = Color(0xFFFF9800)
                    )
                }
                else -> {
                    Text(
                        "Enter amount customer is paying now",
                        color = t.onSurfaceDim,
                        fontSize = 11.sp
                    )
                }
            }

            // Progress pill
            if (billTotal > 0 && paid > 0) {
                val progress = (paid / billTotal).coerceIn(0.0, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(6.dp)
                        .background(t.divider, RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((80 * progress).dp)
                            .background(
                                when {
                                    isOverpaid -> Color(0xFF2196F3)
                                    isFullyPaid -> Color(0xFF4CAF50)
                                    else -> Accent
                                },
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ContactCard(contactName: String, t: BillTheme) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surface, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .background(Accent.copy(alpha = 0.15f), CircleShape)
        ) {
            Text(
                text = contactName.firstOrNull()?.uppercase() ?: "?",
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Billing to", color = t.onSurfaceDim, fontSize = 11.sp)
            Text(
                contactName,
                color = t.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun BalanceSummaryCard(
    previousBalance: Double,
    billTotal: Double,
    newBalance: Double,
    t: BillTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BalanceItem("Previous", previousBalance, t.onSurfaceMid, t)
        Icon(
            Icons.Default.Add,
            tint = t.onSurfaceDim,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        BalanceItem("This Bill", billTotal, Accent, t)
        Icon(
            Icons.AutoMirrored.Default.ArrowForward,
            tint = t.onSurfaceDim,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        BalanceItem("New Balance", newBalance, Color(0xFF4CAF50), t)
    }
}

@Composable
private fun BalanceItem(label: String, amount: Double, color: Color, t: BillTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = t.onSurfaceDim, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "₹%.2f".format(amount),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TotalsCard(
    subtotal: Double,
    taxPercent: Double,
    taxAmount: Double,
    billTotal: Double,
    t: BillTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TotalRow("Subtotal", subtotal, t.onSurfaceMid, t)
        TotalRow("Tax (${taxPercent.toInt()}%)", taxAmount, t.onSurfaceDim, t)
        HorizontalDivider(color = t.divider, thickness = 1.dp)
        TotalRow("Total", billTotal, Accent, t, bold = true)
    }
}

@Composable
private fun TotalRow(label: String, amount: Double, color: Color, t: BillTheme, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = t.onSurfaceMid,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
        Text(
            "₹%.2f".format(amount),
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    t: BillTheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = t.onSurfaceDim, fontSize = 13.sp) },
        suffix = suffix?.let { { Text(it, color = t.onSurfaceMid) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = billFieldColors(t),
        textStyle = LocalTextStyle.current.copy(color = t.onSurface, fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun SectionLabel(text: String, t: BillTheme) {
    Text(
        text = text,
        color = t.onSurfaceDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
    )
}

@Composable
private fun billFieldColors(t: BillTheme) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = t.fieldBorder,
    cursorColor = Accent,
    focusedContainerColor = t.fieldContainer,
    unfocusedContainerColor = t.fieldContainer,
    focusedTextColor = t.onSurface,
    unfocusedTextColor = t.onSurface,
    focusedPlaceholderColor = t.onSurfaceDim,
    unfocusedPlaceholderColor = t.onSurfaceDim
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPreviewScreen(
    ledgerJson: JsonObject,
    darkMode: Boolean = true,
    onBack: () -> Unit,
    onShare: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isDark by remember { mutableStateOf(darkMode) }
    val t = if (isDark) DarkBillTheme else LightBillTheme
    val scrollState = rememberScrollState()

    val contactName  = ledgerJson.get(DootawasMessage.LEDGER_CONTACT_NAME)?.asString ?: ""
    val note         = ledgerJson.get(DootawasMessage.LEDGER_NOTE)?.asString ?: ""
    val createdAt    = ledgerJson.get(DootawasMessage.LEDGER_CREATED_AT)?.asLong ?: System.currentTimeMillis()
    val ledgerType   = LedgerType.fromInt(ledgerJson.get(DootawasMessage.LEDGER_TYPE)?.asInt ?: 0)
    val ledgerStatus = LedgerStatus.fromInt(ledgerJson.get(DootawasMessage.LEDGER_STATUS)?.asInt ?: 0)

    val dataJson     = ledgerJson.getAsJsonObject(DootawasMessage.LEDGER_DATA)
    val subtotal     = dataJson?.get(DootawasMessage.BILL_SUBTOTAL)?.asDouble ?: 0.0
    val taxPercent   = dataJson?.get(DootawasMessage.BILL_TAX_PERCENT)?.asDouble ?: 0.0
    val taxAmount    = dataJson?.get(DootawasMessage.BILL_TAX_AMOUNT)?.asDouble ?: 0.0
    val billTotal    = dataJson?.get(DootawasMessage.BILL_TOTAL)?.asDouble ?: 0.0
    val amountPaid   = dataJson?.get(DootawasMessage.BILL_AMOUNT_PAID)?.asDouble ?: 0.0
    val itemsArray   = dataJson?.getAsJsonArray(DootawasMessage.BILL_ITEMS)

    // ── NEW: invoice # (optional) + bill date (mandatory) ─────────────────
    val invoiceNumber = dataJson?.get(DootawasMessage.BILL_INVOICE_NUMBER)?.asString
    val billDateMillis = dataJson?.get(DootawasMessage.BILL_DATE)?.asLong ?: createdAt

    val sellerName    = dataJson?.get(DootawasMessage.BILL_SELLER_NAME)?.asString
    val sellerGst     = dataJson?.get(DootawasMessage.BILL_SELLER_GST)?.asString
    val sellerPhone   = dataJson?.get(DootawasMessage.BILL_SELLER_PHONE)?.asString
    val sellerEmail   = dataJson?.get(DootawasMessage.BILL_SELLER_EMAIL)?.asString
    val sellerAddress = dataJson?.get(DootawasMessage.BILL_SELLER_ADDRESS)?.asString

    val buyerName     = dataJson?.get(DootawasMessage.BILL_BUYER_NAME)?.asString
    val buyerGst      = dataJson?.get(DootawasMessage.BILL_BUYER_GST)?.asString
    val buyerPhone    = dataJson?.get(DootawasMessage.BILL_BUYER_PHONE)?.asString
    val buyerEmail    = dataJson?.get(DootawasMessage.BILL_BUYER_EMAIL)?.asString
    val buyerAddress  = dataJson?.get(DootawasMessage.BILL_BUYER_ADDRESS)?.asString

    val hasSellerDetails = listOfNotNull(sellerName, sellerGst, sellerPhone, sellerEmail, sellerAddress)
        .any { it.isNotBlank() }
    val hasBuyerDetails = listOfNotNull(buyerName, buyerGst, buyerPhone, buyerEmail, buyerAddress)
        .any { it.isNotBlank() }
    val hasBusinessDetails = hasSellerDetails || hasBuyerDetails

    // ── payment summary — derived, no stored keys needed ──────────────────
    val remaining    = (billTotal - amountPaid).coerceAtLeast(0.0)
    val overpaid     = (amountPaid - billTotal).coerceAtLeast(0.0)
    val isFullyPaid  = amountPaid >= billTotal && billTotal > 0
    val isOverpaid   = amountPaid > billTotal && billTotal > 0

    // NOTE: this now reflects the mandatory bill date, not createdAt.
    // createdAt is still available separately if you want a "created" timestamp shown elsewhere.
    val dateFormatted = remember(billDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(billDateMillis))
    }
    BackHandler {
        onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (!invoiceNumber.isNullOrBlank()) "Invoice $invoiceNumber" else "Bill preview",
                            fontWeight = FontWeight.SemiBold,
                            color = t.onSurface, fontSize = 16.sp
                        )
                        Text(dateFormatted, color = t.onSurfaceDim, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = t.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { isDark = !isDark }) {
                        Icon(
                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme", tint = t.onSurfaceMid
                        )
                    }
                    IconButton(onClick = {
                        printBillAsPdf(context, buildBillHtml(
                            contactName, dateFormatted, itemsArray,
                            subtotal, taxPercent, taxAmount, billTotal,
                            amountPaid, remaining, overpaid, isFullyPaid, isOverpaid,
                            note, sellerName, sellerGst, buyerName, buyerGst,
                            // NEW params — see buildBillHtml note below
                            invoiceNumber, sellerPhone, sellerEmail, sellerAddress,
                            buyerPhone, buyerEmail, buyerAddress
                        ))
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Print / Save PDF",
                            tint = t.onSurfaceMid)
                    }
//                    IconButton(onClick = { onShare(buildShareText(
//                        contactName, dateFormatted, itemsArray,
//                        subtotal, taxPercent, taxAmount, billTotal,
//                        amountPaid, remaining, note,
//                        // NEW param — see buildShareText note below
//                        invoiceNumber
//                    )) }) {
//                        Icon(Icons.Default.Share, contentDescription = "Share", tint = t.onSurfaceMid)
//                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.surface)
            )
        },
        containerColor = t.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Contact card ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(t.surface, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Accent.copy(alpha = 0.15f), CircleShape)
                ) {
                    Text(contactName.firstOrNull()?.uppercase() ?: "?",
                        color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Billed to", color = t.onSurfaceDim, fontSize = 11.sp)
                    Text(contactName, color = t.onSurface,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier
                        .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("₹%.2f".format(billTotal), color = Accent,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── Business / Bill-from-Bill-to details ──────────────────────
            if (hasBusinessDetails) {
                PreviewSection(title = "BUSINESS DETAILS", t = t) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hasSellerDetails) {
                            Text("From", color = t.onSurfaceDim, fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold)
                            if (!sellerName.isNullOrBlank())
                                Text(sellerName, color = t.onSurface, fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium)
                            if (!sellerGst.isNullOrBlank())
                                Text("GST: $sellerGst", color = t.onSurfaceMid, fontSize = 12.sp)
                            if (!sellerPhone.isNullOrBlank())
                                Text(sellerPhone, color = t.onSurfaceMid, fontSize = 12.sp)
                            if (!sellerEmail.isNullOrBlank())
                                Text(sellerEmail, color = t.onSurfaceMid, fontSize = 12.sp)
                            if (!sellerAddress.isNullOrBlank())
                                Text(sellerAddress, color = t.onSurfaceMid, fontSize = 12.sp)
                        }
                        if (hasSellerDetails && hasBuyerDetails)
                            HorizontalDivider(color = t.divider)
                        if (hasBuyerDetails) {
                            Text("To", color = t.onSurfaceDim, fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold)
                            if (!buyerName.isNullOrBlank())
                                Text(buyerName, color = t.onSurface, fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium)
                            if (!buyerGst.isNullOrBlank())
                                Text("GST: $buyerGst", color = t.onSurfaceMid, fontSize = 12.sp)
                            if (!buyerPhone.isNullOrBlank())
                                Text(buyerPhone, color = t.onSurfaceMid, fontSize = 12.sp)
                            if (!buyerEmail.isNullOrBlank())
                                Text(buyerEmail, color = t.onSurfaceMid, fontSize = 12.sp)
                            if (!buyerAddress.isNullOrBlank())
                                Text(buyerAddress, color = t.onSurfaceMid, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (ledgerType == LedgerType.BILL) {
                // ── Items (now with HSN column) ────────────────────────────
                if (itemsArray != null && itemsArray.size() > 0) {
                    PreviewSection(title = "ITEMS", t = t) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(t.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Item", color = t.onSurfaceDim, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f))
                                Text("HSN", color = t.onSurfaceDim, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                                Text("Qty", color = t.onSurfaceDim, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                Text("Price", color = t.onSurfaceDim, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                Text("Total", color = t.onSurfaceDim, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                            }
                            itemsArray.forEachIndexed { index, element ->
                                val item = element.asJsonObject
                                val name  = item.get(DootawasMessage.BILL_ITEM_NAME)?.asString ?: ""
                                val hsn   = item.get(DootawasMessage.BILL_ITEM_HSN)?.asString ?: ""
                                val qty   = item.get(DootawasMessage.BILL_ITEM_QTY)?.asInt ?: 0
                                val price = item.get(DootawasMessage.BILL_ITEM_PRICE)?.asDouble ?: 0.0
                                val total = item.get(DootawasMessage.BILL_ITEM_TOTAL)?.asDouble ?: 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(if (index % 2 == 0) t.surface else t.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = t.onSurface, fontSize = 13.sp,
                                        modifier = Modifier.weight(1.4f), maxLines = 2)
                                    Text(hsn.ifBlank { "—" }, color = t.onSurfaceMid, fontSize = 12.sp,
                                        modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                                    Text("$qty", color = t.onSurfaceMid, fontSize = 13.sp,
                                        modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                    Text("₹%.2f".format(price), color = t.onSurfaceMid,
                                        fontSize = 13.sp, modifier = Modifier.width(70.dp),
                                        textAlign = TextAlign.End)
                                    Text("₹%.2f".format(total), color = t.onSurface,
                                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }

                // ── Summary — derived only, no stored balance keys ────────
                PreviewSection(title = "SUMMARY", t = t) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreviewRow("Subtotal", "₹%.2f".format(subtotal), t.onSurfaceMid, t)
                        if (taxPercent > 0)
                            PreviewRow("Tax (${taxPercent.toInt()}%)",
                                "₹%.2f".format(taxAmount), t.onSurfaceDim, t)
                        HorizontalDivider(color = t.divider)
                        PreviewRow("Bill total", "₹%.2f".format(billTotal), Accent, t, bold = true)
                        Spacer(Modifier.height(4.dp))
                        PreviewRow("Paid", "₹%.2f".format(amountPaid), Color(0xFF4CAF50), t)
                        HorizontalDivider(color = t.divider)
                        when {
                            isOverpaid -> PreviewRow(
                                "Overpaid by", "₹%.2f".format(overpaid),
                                Color(0xFF2196F3), t, bold = true)
                            isFullyPaid -> PreviewRow(
                                "Remaining", "₹0.00",
                                Color(0xFF4CAF50), t, bold = true)
                            else -> PreviewRow(
                                "Remaining", "₹%.2f".format(remaining),
                                Color(0xFFE53935), t, bold = true)
                        }
                    }
                }

                // ── Payment status banner ─────────────────────────────────
                val (label, icon, color) = when {
                    isOverpaid  -> Triple("Overpaid by ₹%.2f".format(overpaid),
                        Icons.Default.CheckCircle, Color(0xFF2196F3))
                    isFullyPaid -> Triple("Fully paid",
                        Icons.Default.CheckCircle, Color(0xFF4CAF50))
                    amountPaid <= 0 -> Triple("Unpaid",
                        Icons.Default.Cancel, Color(0xFFE53935))
                    else -> Triple("Partially paid — ₹%.2f remaining".format(remaining),
                        Icons.Default.Schedule, Color(0xFFFF9800))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            if (note.isNotBlank()) {
                PreviewSection(title = "NOTES", t = t) {
                    Text(note, color = t.onSurfaceMid, fontSize = 14.sp,
                        lineHeight = 20.sp, modifier = Modifier.padding(12.dp))
                }
            }

            val statusColor = when (ledgerStatus) {
                LedgerStatus.ACCEPTED -> Color(0xFF4CAF50)
                LedgerStatus.REJECTED -> Color(0xFFE53935)
                LedgerStatus.PENDING  -> Color(0xFFFF9800)
            }
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Box(modifier = Modifier
                    .background(statusColor.copy(0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(ledgerStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── PDF print via Android PrintManager ───────────────────────────────────────

fun printBillAsPdf(context: Context, html: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val webView = WebView(context)
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val jobName = "Bill_${System.currentTimeMillis()}"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
}


fun buildBillHtml(
    contactName: String,
    date: String,
    itemsArray: JsonArray?,
    subtotal: Double,
    taxPercent: Double,
    taxAmount: Double,
    billTotal: Double,
    amountPaid: Double,
    remaining: Double,
    overpaid: Double,
    isFullyPaid: Boolean,
    isOverpaid: Boolean,
    note: String,
    sellerName: String?,
    sellerGst: String?,
    buyerName: String?,
    buyerGst: String?,
    invoiceNumber: String?,
    sellerPhone: String?,
    sellerEmail: String?,
    sellerAddress: String?,
    buyerPhone: String?,
    buyerEmail: String?,
    buyerAddress: String?
): String {
    val subtotalStr   = "Rs.%.2f".format(subtotal)
    val taxAmountStr  = "Rs.%.2f".format(taxAmount)
    val billTotalStr  = "Rs.%.2f".format(billTotal)
    val amountPaidStr = "Rs.%.2f".format(amountPaid)
    val remainingStr  = "Rs.%.2f".format(remaining)
    val overpaidStr   = "Rs.%.2f".format(overpaid)

    val taxLine = if (taxPercent > 0)
        "<tr><td>Tax (${taxPercent.toInt()}%)</td><td style='text-align:right'>$taxAmountStr</td></tr>"
    else ""

    val remainingRow = if (isOverpaid)
        "<tr class='bold'><td>Overpaid</td><td style='text-align:right'>$overpaidStr</td></tr>"
    else
        "<tr class='bold'><td>Remaining</td><td style='text-align:right'>$remainingStr</td></tr>"

    val statusText = when {
        isOverpaid  -> "Overpaid by $overpaidStr"
        isFullyPaid -> "Fully Paid"
        amountPaid <= 0 -> "Unpaid"
        else -> "Partially Paid \u2014 $remainingStr remaining"
    }
    val statusBg = when {
        isFullyPaid || isOverpaid -> "#e7f6ed"
        amountPaid <= 0 -> "#fdecec"
        else -> "#fef3e2"
    }
    val statusFg = when {
        isFullyPaid || isOverpaid -> "#1a7f4b"
        amountPaid <= 0 -> "#c0392b"
        else -> "#b7660b"
    }

    val itemsHtml = buildString {
        itemsArray?.forEach { element ->
            val item = element.asJsonObject
            val name  = item.get(DootawasMessage.BILL_ITEM_NAME)?.asString ?: ""
            val hsn   = item.get(DootawasMessage.BILL_ITEM_HSN)?.asString ?: ""
            val qty   = item.get(DootawasMessage.BILL_ITEM_QTY)?.asInt ?: 0
            val price = "Rs.%.2f".format(item.get(DootawasMessage.BILL_ITEM_PRICE)?.asDouble ?: 0.0)
            val total = "Rs.%.2f".format(item.get(DootawasMessage.BILL_ITEM_TOTAL)?.asDouble ?: 0.0)
            append("<tr><td>$name</td>")
            append("<td style='text-align:center;color:#888'>${hsn.ifBlank { "\u2014" }}</td>")
            append("<td style='text-align:center'>$qty</td>")
            append("<td style='text-align:right'>$price</td>")
            append("<td style='text-align:right'>$total</td></tr>")
        }
    }

    fun partyBlock(
        label: String,
        name: String?,
        gst: String?,
        phone: String?,
        email: String?,
        address: String?,
        fallbackName: String? = null
    ): String {
        val displayName = name?.takeIf { it.isNotBlank() } ?: fallbackName
        if (displayName.isNullOrBlank() && gst.isNullOrBlank() && phone.isNullOrBlank()
            && email.isNullOrBlank() && address.isNullOrBlank()
        ) return ""
        return buildString {
            append("<div class='party'><h3>$label</h3>")
            if (!displayName.isNullOrBlank()) append("<p class='party-name'>$displayName</p>")
            if (!address.isNullOrBlank()) append("<p class='party-line'>$address</p>")
            if (!phone.isNullOrBlank()) append("<p class='party-line'>$phone</p>")
            if (!email.isNullOrBlank()) append("<p class='party-line'>$email</p>")
            if (!gst.isNullOrBlank()) append("<p class='party-gst'>GSTIN: $gst</p>")
            append("</div>")
        }
    }

    val fromBlock = partyBlock("BILL FROM", sellerName, sellerGst, sellerPhone, sellerEmail, sellerAddress)
    val toBlock = partyBlock("BILL TO", buyerName, buyerGst, buyerPhone, buyerEmail, buyerAddress, fallbackName = contactName)

    val partiesHtml = if (fromBlock.isNotBlank() || toBlock.isNotBlank())
        "<div class='parties'>$fromBlock$toBlock</div>"
    else
        "<div style='margin-bottom:20px'><strong>Billed to:</strong> $contactName</div>"

    val invoiceMetaHtml = buildString {
        append("<div class='invoice-meta'>")
        if (!invoiceNumber.isNullOrBlank())
            append("<div><span class='meta-label'>Invoice No.</span><span class='meta-value'>$invoiceNumber</span></div>")
        append("<div><span class='meta-label'>Date</span><span class='meta-value'>$date</span></div>")
        append("</div>")
    }

    val noteHtml = if (note.isNotBlank())
        "<div class='note'><strong>Note:</strong> $note</div>" else ""

    return """
<html><head><meta charset="UTF-8">
<style>
  * { box-sizing: border-box; }
  body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; color: #1a1a1a; }
  .header { display: flex; justify-content: space-between; align-items: flex-start;
            border-bottom: 2px solid #6366f1; padding-bottom: 16px; margin-bottom: 24px; }
  .header h1 { font-size: 26px; margin: 0; color: #1a1a1a; letter-spacing: 0.5px; }
  .invoice-meta { text-align: right; font-size: 13px; }
  .invoice-meta div { margin-bottom: 4px; }
  .meta-label { color: #888; margin-right: 8px; }
  .meta-value { font-weight: bold; color: #1a1a1a; }
  .parties { display: flex; gap: 40px; margin-bottom: 28px; }
  .party { flex: 1; }
  .party h3 { font-size: 11px; color: #6366f1; letter-spacing: 1px; margin: 0 0 6px; }
  .party-name { margin: 0 0 2px; font-size: 14px; font-weight: bold; color: #1a1a1a; }
  .party-line { margin: 0 0 2px; font-size: 12.5px; color: #555; }
  .party-gst { margin: 4px 0 0; font-size: 12px; color: #888; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
  th { background: #f4f4fb; padding: 10px 12px; text-align: left; font-size: 11.5px;
       color: #444; letter-spacing: 0.3px; }
  td { padding: 9px 12px; font-size: 13px; border-bottom: 1px solid #eee; }
  .totals { margin-left: auto; width: 280px; }
  .totals tr td { border: none; padding: 5px 8px; font-size: 13px; }
  .totals .bold td { font-weight: bold; border-top: 1px solid #ddd; padding-top: 10px;
                      color: #6366f1; font-size: 14px; }
  .status { display: inline-block; padding: 7px 18px; border-radius: 20px;
            font-size: 13px; font-weight: bold; margin-top: 18px;
            background: $statusBg; color: $statusFg; }
  .note { background: #f9f9fb; border-left: 3px solid #6366f1; padding: 10px 14px;
          font-size: 13px; color: #555; margin-top: 18px; border-radius: 4px; }
</style></head><body>

<div class="header">
  <h1>INVOICE</h1>
  $invoiceMetaHtml
</div>

$partiesHtml

<table>
  <tr>
    <th>Item</th>
    <th style='text-align:center'>HSN/SAC</th>
    <th style='text-align:center'>Qty</th>
    <th style='text-align:right'>Price</th>
    <th style='text-align:right'>Total</th>
  </tr>
  $itemsHtml
</table>

<table class="totals">
  <tr><td>Subtotal</td><td style='text-align:right'>$subtotalStr</td></tr>
  $taxLine
  <tr class="bold"><td>Bill Total</td><td style='text-align:right'>$billTotalStr</td></tr>
  <tr><td>Paid</td><td style='text-align:right'>$amountPaidStr</td></tr>
  $remainingRow
</table>

<div class="status">$statusText</div>
$noteHtml

</body></html>
    """.trimIndent()
}


// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun PreviewSection(
    title: String,
    t: BillTheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surface, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Text(
            text = title,
            color = t.onSurfaceDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 6.dp)
        )
        content()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    valueColor: Color,
    t: BillTheme,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = t.onSurfaceMid,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, fontSize = 14.sp)
        Text(value, color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}

// ─── Share text formatter ─────────────────────────────────────────────────────

fun Bill.toShareText(): String {
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(createdAt))
    val sb = StringBuilder()
    sb.appendLine("🧾 *Bill from Knotx*")
    sb.appendLine("Date: $date")
    sb.appendLine("To: $contactName")
    sb.appendLine()
    sb.appendLine("*Items:*")
    items.forEach {
        sb.appendLine("  ${it.name} × ${it.quantity}  →  ₹%.2f".format(it.total))
    }
    sb.appendLine()
    sb.appendLine("Subtotal:   ₹%.2f".format(subtotal))
    if (taxPercent > 0)
        sb.appendLine("Tax (${taxPercent.toInt()}%): ₹%.2f".format(taxAmount))
    sb.appendLine("*Total:     ₹%.2f*".format(billTotal))
    sb.appendLine()
    sb.appendLine("Prev. Balance: ₹%.2f".format(previousBalance))
    sb.appendLine("Paid:          ₹%.2f".format(amountPaid))
    sb.appendLine("*Net Balance:  ₹%.2f*".format(newBalance))
    if (notes.isNotBlank()) {
        sb.appendLine()
        sb.appendLine("Note: $notes")
    }
    return sb.toString()
}

// ─── Data Model ───────────────────────────────────────────────────────────────

enum class SettlementType {
    CREDIT,
    DEBIT
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementSheet(
    contactId: String,
    contactName: String,
    currentBalance: Double,
    darkMode: Boolean = true,
    onDismiss: () -> Unit,
    onSettled: (JsonObject) -> Unit,
    onCreateBill: () -> Unit
) {
    val t = if (darkMode) DarkBillTheme else LightBillTheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(SettlementType.CREDIT) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showHint by remember { mutableStateOf(true) }

    LaunchedEffect(selectedType) {
        showHint = true
        delay(2500)
        showHint = false
    }

    // Fade only; layout height never changes
    val hintAlpha by animateFloatAsState(
        targetValue = if (showHint) 1f else 0f,
        animationSpec = tween(250),
        label = "hintAlpha"
    )

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val previewBalance = when (selectedType) {
        SettlementType.CREDIT -> currentBalance + amount
        SettlementType.DEBIT  -> currentBalance - amount
    }
    val delta = previewBalance - currentBalance

    val hintText = when (selectedType) {
        SettlementType.CREDIT -> "माल दिया या पैसे दिए"
        SettlementType.DEBIT  -> "पैसे मिले या माल मिला"
    }
    val hintColor = when (selectedType) {
        SettlementType.CREDIT -> Color(0xFF4CAF50)
        SettlementType.DEBIT  -> Color(0xFFE53935)
    }
    val isValid = amount > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = t.surface,
        modifier = Modifier
            .statusBarsPadding()      // ← add this
            .fillMaxSize(),                 // ← always full screen
        contentWindowInsets = { WindowInsets(0) },         // ← we handle insets ourselves
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(t.onSurfaceDim.copy(alpha = 0.4f), RoundedCornerShape(50))
            )
        }
    ) {
        // Outer column: fixed size, handles nav bar + keyboard ONCE
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {

            // ── Scrollable content (takes all remaining space) ────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Accent.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Text(
                            contactName.firstOrNull()?.uppercase() ?: "?",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Settle balance", color = t.onSurface,
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(contactName, color = t.onSurfaceDim, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = t.onSurfaceDim)
                    }
                }

                HorizontalDivider(color = t.divider)

                // Current balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Balance", color = t.onSurfaceMid, fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .background(
                                if (currentBalance > 0) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                else Color(0xFFE53935).copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "₹%.2f".format(currentBalance),
                            color = if (currentBalance > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                TextButton(
                    onClick = onCreateBill,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Accent)
                ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create a bill instead", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                HorizontalDivider(color = t.divider)

                // Type selector
                SettlementTypeSelector(
                    selected = selectedType,
                    onSelect = {
                        selectedType = it
                        amountText = ""
                    },
                    t = t
                )

                // Hint: FIXED height slot, only alpha changes → no layout jump
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .alpha(hintAlpha)
                        .background(hintColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = hintColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            hintText,
                            color = hintColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Amount input
                SectionLabel(
                    if (selectedType == SettlementType.CREDIT) "AMOUNT TO ADD" else "AMOUNT TO REDUCE",
                    t
                )
                SettlementAmountField(
                    type = selectedType,
                    amountText = amountText,
                    currentBalance = currentBalance,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    t = t
                )

                // Balance equation
                SectionLabel("BEFORE → AFTER", t)
                BalanceEquationRow(
                    currentBalance = currentBalance,
                    delta = delta,
                    newBalance = previewBalance,
                    t = t
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Add a note (optional)", color = t.onSurfaceDim, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = billFieldColors(t),
                    textStyle = LocalTextStyle.current.copy(color = t.onSurface, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes, contentDescription = null,
                            tint = t.onSurfaceDim, modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Spacer(Modifier.height(8.dp))
            }

            // ── Confirm button: pinned at the bottom, always visible ──────
            Button(
                onClick = {
                    val typeValue = when (selectedType) {
                        SettlementType.CREDIT -> LedgerType.CREDIT.value
                        SettlementType.DEBIT  -> LedgerType.DEBIT.value
                    }
                    val dataJson = JsonObject().apply {
                        addProperty(DootawasMessage.SETTLEMENT_TYPE, typeValue)
                    }
                    val ledgerJson = JsonObject().apply {
                        addProperty(DootawasMessage.LEDGER_TYPE, typeValue)
                        addProperty(DootawasMessage.LEDGER_STATUS, LedgerStatus.PENDING.value)
                        addProperty(DootawasMessage.LEDGER_AMOUNT, amount)
                        addProperty(DootawasMessage.LEDGER_CONTACT_ID, contactId)
                        addProperty(DootawasMessage.LEDGER_CONTACT_NAME, contactName)
                        addProperty(DootawasMessage.LEDGER_NOTE, note)
                        addProperty(DootawasMessage.LEDGER_CREATED_AT, System.currentTimeMillis())
                        add(DootawasMessage.LEDGER_DATA, dataJson)
                    }
                    onSettled(ledgerJson)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = settlementColor(selectedType),
                    disabledContainerColor = t.divider
                )
            ) {
                Icon(
                    imageVector = settlementIcon(selectedType),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    settlementLabel(selectedType),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}
// ─── Balance equation row ──────────────────────────────────────────────────────

@Composable
private fun BalanceEquationRow(
    currentBalance: Double,
    delta: Double,
    newBalance: Double,
    t: BillTheme
) {
    val deltaColor = if (delta > 0) Color(0xFF4CAF50) else Color(0xFFE53935)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Before
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Before", color = t.onSurfaceDim, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text("₹%.2f".format(currentBalance), color = t.onSurfaceMid,
                fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        // Delta
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Change", color = t.onSurfaceDim, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                (if (delta >= 0) "+₹%.2f" else "-₹%.2f").format(kotlin.math.abs(delta)),
                color = deltaColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Icon(Icons.Default.ArrowForward, contentDescription = null,
            tint = t.onSurfaceDim, modifier = Modifier.size(14.dp))

        // After
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("After", color = t.onSurfaceDim, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "₹%.2f".format(newBalance),
                color = if (newBalance <= 0) Color(0xFF4CAF50) else t.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// ─── Type selector ────────────────────────────────────────────────────────────

@Composable
private fun SettlementTypeSelector(
    selected: SettlementType,
    onSelect: (SettlementType) -> Unit,
    t: BillTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SettlementType.entries.forEach { type ->
            val isSelected = selected == type
            val color = settlementColor(type)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) color.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) color.copy(alpha = 0.4f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(type) }
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = settlementIcon(type),
                        contentDescription = null,
                        tint = if (isSelected) color else t.onSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = settlementTabLabel(type),
                        color = if (isSelected) color else t.onSurfaceDim,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ─── Amount field ─────────────────────────────────────────────────────────────

@Composable
private fun SettlementAmountField(
    type: SettlementType,
    amountText: String,
    currentBalance: Double,
    onValueChange: (String) -> Unit,
    t: BillTheme
) {
    val label = when (type) {
        SettlementType.CREDIT     -> "Amount to add to balance"
        SettlementType.DEBIT      -> "Amount to reduce from balance"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = t.onSurfaceDim, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)

        OutlinedTextField(
            value = amountText,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "0.00",
                    color = t.onSurfaceDim, fontSize = 15.sp
                )
            },
            prefix = { Text("₹", color = t.onSurfaceMid, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = settlementColor(type),
                unfocusedBorderColor = t.fieldBorder,
                cursorColor = settlementColor(type),
                focusedContainerColor = t.fieldContainer,
                unfocusedContainerColor = t.fieldContainer,
                focusedTextColor = t.onSurface,
                unfocusedTextColor = t.onSurface
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = t.onSurface
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Quick fill — only for THEY_PAID when balance > 0
        if (type == SettlementType.DEBIT && currentBalance > 0)  {
            TextButton(
                onClick = { onValueChange("%.2f".format(currentBalance)) },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Clear full balance ₹%.2f".format(currentBalance),
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


private fun settlementColor(type: SettlementType) = when (type) {
    SettlementType.CREDIT -> Color(0xFF4CAF50)
    SettlementType.DEBIT  -> Color(0xFFE53935)
}

private fun settlementIcon(type: SettlementType) = when (type) {
    SettlementType.CREDIT -> Icons.Default.Add
    SettlementType.DEBIT  -> Icons.Default.Remove
}

private fun settlementLabel(type: SettlementType) = when (type) {
    SettlementType.CREDIT -> "Add to balance"
    SettlementType.DEBIT  -> "Reduce balance"
}

private fun settlementTabLabel(type: SettlementType) = when (type) {
    SettlementType.CREDIT -> "Add"
    SettlementType.DEBIT  -> "Reduce"
}