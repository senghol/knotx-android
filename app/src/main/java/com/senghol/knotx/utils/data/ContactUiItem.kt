package com.senghol.knotx.utils.data

import com.senghol.knotx.data.database.Contact
import com.senghol.knotx.utils.enums.ContactButtonState

data class ContactUiItem(
    val number: String,
    val name: String,
    val imageUrl: String?,
    val isInstalled: Boolean,
    val buttonState: ContactButtonState,
    var backgroundColor: Int = 0,
)

fun Contact.toUiItem(): ContactUiItem {
    return ContactUiItem(
        number = number,
        name = name,
        imageUrl = imageUrl,
        isInstalled = isInstalled,
        buttonState = buttonState,
        backgroundColor = backgroundColor
    )
}
