package com.senghol.knotx.utils.data

import com.senghol.knotx.utils.enums.SandeshStithi

data class GroupUiItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val lastMessage: String? = null,
    val totalUnread: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean,
    val lastMessageTime: String,
    val sandeshStithi: SandeshStithi,
    val groupBackgroundColor: Int
)
