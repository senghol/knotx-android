package com.senghol.knotx.ui.dukaan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * "My business" - three tabs:
 *   My details : the details printed on your bills, invoice numbering, bill defaults
 *   Customers  : saved "Bill to" details (edit / delete)
 *   Items      : saved items used as hints on bills (add / edit / delete)
 *
 * Creates its own [DukaanViewModel], so the caller only passes [onBack].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DukaanScreen(
    darkMode: Boolean = true,
    onBack: () -> Unit,
) {
    val vm: DukaanViewModel = viewModel()
    var isDark by remember { mutableStateOf(darkMode) }
    val t = if (isDark) DarkBillTheme else LightBillTheme

    var tab by rememberSaveable { mutableStateOf(0) }

    val form by vm.form.collectAsState()
    val errors by vm.errors.collectAsState()
    val hasChanges by vm.hasChanges.collectAsState()
    val isSaving by vm.isSaving.collectAsState()
    val customers by vm.customers.collectAsState()
    val savedItems by vm.savedItems.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                DukaanEvent.Saved -> snackbar.showSnackbar("Your details saved")
                is DukaanEvent.SaveFailed -> snackbar.showSnackbar(event.message)
            }
        }
    }

    // Only the "My details" tab has unsaved-change tracking; the other tabs save per dialog.
    fun requestBack() {
        if (hasChanges) showDiscard = true else onBack()
    }
    BackHandler { requestBack() }

    Scaffold(
        containerColor = t.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("My business", fontWeight = FontWeight.SemiBold, color = t.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = { requestBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = t.onSurface)
                        }
                    },
                    actions = {
                        if (tab == 0) {
                            Button(
                                onClick = { vm.save() },
                                enabled = hasChanges && !isSaving,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White,
                                    disabledContainerColor = t.divider,
                                    disabledContentColor = t.onSurfaceDim,
                                ),
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = { isDark = !isDark }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = t.onSurfaceMid,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = t.surface),
                )
                TabsBar(
                    labels = listOf("My details", "Customers (${customers.size})", "Items (${savedItems.size})"),
                    selected = tab,
                    onSelect = { tab = it },
                    t = t,
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> DetailsTab(vm = vm, form = form, errors = errors, t = t)
                1 -> CustomersTab(vm = vm, customers = customers, t = t)
                else -> ItemsTab(vm = vm, catalog = savedItems, t = t)
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            containerColor = t.surface,
            title = { Text("Discard changes?", color = t.onSurface) },
            text = { Text("Your edits to My details haven't been saved.", color = t.onSurfaceMid) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    vm.discardChanges()
                    onBack()
                }) { Text("Discard", color = BillDanger) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("Keep editing", color = Accent) }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Tabs bar
// ---------------------------------------------------------------------------

@Composable
private fun TabsBar(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    t: BillTheme,
) {
    Row(Modifier.fillMaxWidth().background(t.surface)) {
        labels.forEachIndexed { i, label ->
            val isSel = i == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    color = if (isSel) Accent else t.onSurfaceMid,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isSel) Accent else Color.Transparent)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 1: My details
// ---------------------------------------------------------------------------

@Composable
private fun DetailsTab(
    vm: DukaanViewModel,
    form: DukaanForm?,
    errors: DukaanFormErrors,
    t: BillTheme,
) {
    if (form == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }
    val f = form

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard("YOUR BUSINESS (SHOWN ON BILLS)", t) {
            DukaanField(f.name, { v -> vm.onFormChange { it.copy(name = v) } }, "Business name *", t, error = errors.name)
            DukaanField(f.gst, { v -> vm.onFormChange { it.copy(gst = v.uppercase()) } }, "GST number (optional)", t, error = errors.gst)
            DukaanField(
                f.phone,
                { v -> vm.onFormChange { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' }) } },
                "Phone (optional)", t, error = errors.phone, keyboardType = KeyboardType.Phone,
            )
            DukaanField(f.email, { v -> vm.onFormChange { it.copy(email = v) } }, "Email (optional)", t, error = errors.email, keyboardType = KeyboardType.Email)
            DukaanField(f.address, { v -> vm.onFormChange { it.copy(address = v) } }, "Address (optional)", t, singleLine = false, minLines = 2)
            DukaanField(f.upiId, { v -> vm.onFormChange { it.copy(upiId = v.trim()) } }, "UPI ID (optional)", t, error = errors.upiId)
        }

        SectionCard("INVOICE NUMBERING", t) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DukaanField(
                    f.invoicePrefix, { v -> vm.onFormChange { it.copy(invoicePrefix = v) } },
                    "Prefix", t, error = errors.invoicePrefix, modifier = Modifier.weight(1f),
                )
                DukaanField(
                    f.invoicePadding,
                    { v -> vm.onFormChange { it.copy(invoicePadding = v.filter { c -> c.isDigit() }.take(1)) } },
                    "Digits", t, error = errors.invoicePadding, keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            DukaanField(
                f.nextInvoiceNumber,
                { v -> vm.onFormChange { it.copy(nextInvoiceNumber = v.filter { c -> c.isDigit() }.take(9)) } },
                "Next number", t, error = errors.nextInvoiceNumber, keyboardType = KeyboardType.Number,
            )
            InvoicePreview(f, t)
            Text(
                "The next number goes up by one each time a bill is sent. Change it only if you " +
                        "need to, so invoice numbers stay in order.",
                color = t.onSurfaceDim,
                fontSize = 12.sp,
            )
        }

        SectionCard("BILL DEFAULTS", t) {
            DukaanField(
                f.defaultTaxPercent,
                { v -> vm.onFormChange { it.copy(defaultTaxPercent = v.filter { c -> c.isDigit() || c == '.' }) } },
                "Default tax / GST", t, error = errors.defaultTaxPercent,
                keyboardType = KeyboardType.Decimal, suffix = "%",
            )
            DukaanField(
                f.defaultNotes, { v -> vm.onFormChange { it.copy(defaultNotes = v) } },
                "Default note on bills", t, singleLine = false, minLines = 3,
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

/** Live "next bill will be numbered ..." box, updates as the fields change. */
@Composable
private fun InvoicePreview(form: DukaanForm, t: BillTheme) {
    val pad = form.invoicePadding.toIntOrNull()
    val next = form.nextInvoiceNumber.toLongOrNull()
    val text = if (pad != null && pad in 1..8 && next != null && next >= 1) {
        form.invoicePrefix + next.toString().padStart(pad, '0')
    } else "-"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Next bill number", color = t.onSurfaceMid, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text, color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// ---------------------------------------------------------------------------
// Tab 2: Customers
// ---------------------------------------------------------------------------

@Composable
private fun CustomersTab(
    vm: DukaanViewModel,
    customers: List<DukaanCustomer>,
    t: BillTheme,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<DukaanCustomer?>(null) }
    var deleting by remember { mutableStateOf<DukaanCustomer?>(null) }

    val shown = remember(customers, query) {
        val q = query.trim()
        if (q.isEmpty()) customers
        else customers.filter {
            it.name.contains(q, true) || it.contactId.contains(q, true) ||
                    it.phone.contains(q, true) || it.gst.contains(q, true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        SearchBox(query, { query = it }, "Search customers", t, Modifier.padding(16.dp))

        if (shown.isEmpty()) {
            EmptyState(
                if (customers.isEmpty())
                    "No saved customers yet.\nOn a bill, tap the bookmark next to BILL TO to save one."
                else "No match",
                t,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.contactId }) { c ->
                    RowCard(
                        title = c.name.ifBlank { c.contactId },
                        subtitle = listOf(c.phone, c.gst).filter { it.isNotBlank() }.joinToString("  -  ")
                            .ifBlank { "Contact: ${c.contactId}" },
                        t = t,
                        onClick = { editing = c },
                        onDelete = { deleting = c },
                    )
                }
            }
        }
    }

    editing?.let { c ->
        CustomerDialog(
            customer = c,
            t = t,
            onSave = { updated -> scope.launch { vm.saveCustomer(updated) }; editing = null },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { c ->
        ConfirmDeleteDialog(
            title = "Delete customer?",
            message = "\"${c.name.ifBlank { c.contactId }}\" will be removed from saved customers. " +
                    "Old bills are not affected.",
            t = t,
            onConfirm = { scope.launch { vm.deleteCustomer(c.contactId) }; deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CustomerDialog(
    customer: DukaanCustomer,
    t: BillTheme,
    onSave: (DukaanCustomer) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(customer.name) }
    var gst by remember { mutableStateOf(customer.gst) }
    var phone by remember { mutableStateOf(customer.phone) }
    var email by remember { mutableStateOf(customer.email) }
    var address by remember { mutableStateOf(customer.address) }

    val gstErr = gstError(gst)
    val phoneErr = phoneError(phone)
    val emailErr = emailError(email)
    val valid = gstErr == null && phoneErr == null && emailErr == null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.surface,
        title = { Text("Edit customer", color = t.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Contact: ${customer.contactId}", color = t.onSurfaceDim, fontSize = 12.sp)
                DukaanField(name, { name = it }, "Name", t)
                DukaanField(gst, { gst = it.uppercase() }, "GST number", t, error = gstErr)
                DukaanField(
                    phone, { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    "Phone", t, error = phoneErr, keyboardType = KeyboardType.Phone,
                )
                DukaanField(email, { email = it }, "Email", t, error = emailErr, keyboardType = KeyboardType.Email)
                DukaanField(address, { address = it }, "Address", t, singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        customer.copy(
                            name = name.trim(), gst = gst.trim(), phone = phone.trim(),
                            email = email.trim(), address = address.trim(),
                        )
                    )
                },
            ) { Text("Save", color = if (valid) Accent else t.onSurfaceDim) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = t.onSurfaceMid) } },
    )
}

// ---------------------------------------------------------------------------
// Tab 3: Items
// ---------------------------------------------------------------------------

@Composable
private fun ItemsTab(
    vm: DukaanViewModel,
    catalog: List<CatalogItem>,
    t: BillTheme,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    // editingItem == null && showItemDialog -> adding a new item.
    var showItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
    var deleting by remember { mutableStateOf<CatalogItem?>(null) }

    val shown = remember(catalog, query) {
        val q = query.trim()
        if (q.isEmpty()) catalog
        else catalog.filter { it.name.contains(q, true) || it.hsn.contains(q, true) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchBox(query, { query = it }, "Search items", t, Modifier.weight(1f))
            FilledIconButton(
                onClick = { editingItem = null; showItemDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Accent),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item", tint = Color.White)
            }
        }

        if (shown.isEmpty()) {
            EmptyState(
                if (catalog.isEmpty())
                    "No saved items yet.\nTap + to add one, or tap the bookmark on a bill item."
                else "No match",
                t,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.id }) { item ->
                    RowCard(
                        title = item.name,
                        subtitle = if (item.hsn.isNotBlank()) "HSN/SAC ${item.hsn}" else "No HSN/SAC",
                        trailing = if (item.price > 0) "₹" + formatMoney(item.price) else "No price",
                        t = t,
                        onClick = { editingItem = item; showItemDialog = true },
                        onDelete = { deleting = item },
                    )
                }
            }
        }
    }

    if (showItemDialog) {
        ItemDialog(
            item = editingItem,
            t = t,
            onSave = { name, hsn, price -> vm.saveCatalogItem(editingItem?.id, name, hsn, price) },
            onDismiss = { showItemDialog = false; editingItem = null },
        )
    }
    deleting?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete item?",
            message = "\"${item.name}\" will be removed from saved items. Old bills are not affected.",
            t = t,
            onConfirm = { scope.launch { vm.deleteItem(item.id) }; deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ItemDialog(
    item: CatalogItem?,
    t: BillTheme,
    onSave: suspend (name: String, hsn: String, price: Double) -> String?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(item?.name ?: "") }
    var hsn by remember { mutableStateOf(item?.hsn ?: "") }
    var priceText by remember {
        mutableStateOf(item?.price?.takeIf { it > 0 }?.let { formatMoney(it) } ?: "")
    }
    var serverError by remember { mutableStateOf<String?>(null) }

    val priceValue = if (priceText.isBlank()) 0.0 else priceText.toDoubleOrNull()
    val priceErr = if (priceValue == null || priceValue < 0) "Enter a valid price" else null
    val valid = name.isNotBlank() && priceErr == null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.surface,
        title = { Text(if (item == null) "Add item" else "Edit item", color = t.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DukaanField(name, { name = it; serverError = null }, "Item name *", t, error = serverError)
                DukaanField(
                    hsn, { hsn = it }, "HSN/SAC (optional)", t,
                    keyboardType = KeyboardType.Number,
                )
                DukaanField(
                    priceText, { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                    "Default price", t, error = priceErr,
                    keyboardType = KeyboardType.Decimal, prefix = "₹",
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    scope.launch {
                        val err = onSave(name, hsn, priceValue ?: 0.0)
                        if (err == null) onDismiss() else serverError = err
                    }
                },
            ) { Text("Save", color = if (valid) Accent else t.onSurfaceDim) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = t.onSurfaceMid) } },
    )
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

private fun formatMoney(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v)

@Composable
private fun SectionCard(
    title: String,
    t: BillTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            color = t.onSurfaceDim,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
        )
        content()
    }
}

@Composable
private fun DukaanField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    t: BillTheme,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    prefix: String? = null,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it, color = t.onSurfaceMid) } },
        suffix = suffix?.let { { Text(it, color = t.onSurfaceMid) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(color = t.onSurface, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = t.fieldContainer,
            unfocusedContainerColor = t.fieldContainer,
            focusedBorderColor = Accent,
            unfocusedBorderColor = t.fieldBorder,
            focusedLabelColor = Accent,
            unfocusedLabelColor = t.onSurfaceDim,
            cursorColor = Accent,
            errorBorderColor = BillDanger,
            errorLabelColor = BillDanger,
            errorSupportingTextColor = BillDanger,
            errorContainerColor = t.fieldContainer,
        ),
    )
}

@Composable
private fun SearchBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    t: BillTheme,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = t.onSurfaceDim, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = t.onSurfaceDim) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(color = t.onSurface, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = t.fieldContainer,
            unfocusedContainerColor = t.fieldContainer,
            focusedBorderColor = Accent,
            unfocusedBorderColor = t.fieldBorder,
            cursorColor = Accent,
        ),
    )
}

@Composable
private fun RowCard(
    title: String,
    subtitle: String,
    t: BillTheme,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                title, color = t.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle, color = t.onSurfaceMid, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(trailing, color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = t.onSurfaceDim)
        }
    }
}

@Composable
private fun EmptyState(message: String, t: BillTheme) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            message,
            color = t.onSurfaceDim,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    t: BillTheme,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.surface,
        title = { Text(title, color = t.onSurface) },
        text = { Text(message, color = t.onSurfaceMid) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = BillDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = t.onSurfaceMid) } },
    )
}