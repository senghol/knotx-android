package com.senghol.knotx.utils.data


//todo instead of his callupbhogta only a single CallUiState.kt can be used

data class CallUpbhogta(
    val userNumber: String,          // unique ID (phone / user id)
    val userName: String? = null,
    val profileImageUrl: String? = null,
    val status: String? = null,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = false
)
