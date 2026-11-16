package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.senghol.knotx.utils.enums.ContactButtonState

@Entity(tableName = "contact")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, //todo update name to userName
    val isInstalled: Boolean = false,
    val number: String, //todo update name to userNumber
    val imageUrl: String? = null,
    val imageTime: Long = 0L,
    var backgroundColor: Int = 0,
    val isSelected: Boolean = false,
    var buttonState: ContactButtonState = if (isInstalled) ContactButtonState.ADD else ContactButtonState.INVITE
)
