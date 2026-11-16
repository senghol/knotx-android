package com.senghol.knotx.ui.contactPicker




/** What the picker returns for every ticked row. */
data class PickedContact(
    /** User number, "@username" id, or group ID when isGroup is true. */
    val userNumber: String,
    val userName: String,
    val profileUri: String?,
    val isGroup: Boolean
) {
    /** Name for compact places (chips, strips). */
    val displayName: String
        get() = if (isGroup) userName else userName.ifBlank { userNumber }

    /** True for people saved as "@something" instead of a phone number. */
    val isUsernameId: Boolean
        get() = !isGroup && userNumber.startsWith("@")
}

/** Which lists the picker shows. Disabled sources are never queried. */
data class PickerSources(
    /** Upbhogta whose userNumber does NOT start with "@". */
    val numberContacts: Boolean = true,
    /** Upbhogta whose userNumber starts with "@". */
    val usernameContacts: Boolean = false,
    val groups: Boolean = false,
    /** Installed device contacts in the ADD state. */
    val deviceContacts: Boolean = false,
    /** Show members of all groups or the specified group. */
    val groupMembers: Boolean = false,
    /** Null means members from all groups; otherwise filter by this group ID. */
    val groupId: String? = null
)
