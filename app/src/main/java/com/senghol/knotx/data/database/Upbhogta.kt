package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.senghol.knotx.utils.enums.SandeshStithi
import java.util.UUID

@Entity(
    tableName = "upbhogta",
    indices = [Index(value = ["userNumber"], unique = true)] // Ensure number is unique
)

data class Upbhogta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String? = null,
    val onlineName: String? = null,
    val userNumber: String,
    val photoUrl: String? = null,
    val about: String? = null,
    val lastMessage: String? = null,
    val lastOnline: Long = System.currentTimeMillis(),
    val totalUnread: Int = 0,
    val status: String? = null,
    val encryptionKey: String? = null, //todo delete this
    val isBlocked: Boolean = false,
    val isPinned: Boolean = false,
    val backgroundColor: Int = 0, // ✅ Default value
    val isFavourite: Boolean = false,
    val isMuted: Boolean = false,
    val wallpaperImage: String? = null,
    val isArchived: Boolean = false,
    val isGroup: Boolean = false, //todo delete this
    val lastMessageTime: Long = System.currentTimeMillis(),
    val imageTime: Long = 0L,
    val sandeshStithi: SandeshStithi,
    val lastMessageId: String,
    val isAccountActive: Boolean = true,
    val draftMessage: String? = null,
    val customColors: String? = null,
    val bakaya: Double = 0.0,  // ← Int to Double
    val revealSender: Boolean = false,
    val autoDisappearMsg: Int = 0,
    val hideKey: String? = null
)
