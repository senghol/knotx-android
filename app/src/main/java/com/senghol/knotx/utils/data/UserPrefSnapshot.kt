package com.senghol.knotx.utils.data

data class UserPrefSnapshot(
    val userName: String? = null,
    val userPass: String? = null,
    val userNumber: String? = null,
    val isRegistered: Boolean = false,
    val region: String? = null,
    val isLoggedIn: Boolean = false,
    val profileImage: String? = null,
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val language: String? = null,
    val lastSyncTime: String? = null,
    val isFirstRun: Boolean = true,
    val sessionToken: String? = null,
    val isBiometricEnabled: Boolean = false,
    val lastMessageId: String? = null,
    val unreadCount: Int = 0
)
