package com.senghol.knotx.utils.data

import com.senghol.knotx.utils.enums.SandeshStithi

data class UpbhogtaUiItem(
    val userNumber: String,
    val name: String,
    val imageUrl: String? = null,
    val lastMessage: String? = null,
    val totalUnread: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    val isFavourite: Boolean = false,
    val isTyping: Boolean = false,
    val background: Int,
    val lastMessageTime: String,
    val sandeshStithi: SandeshStithi
)
