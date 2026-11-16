package com.senghol.knotx.utils.data

import com.senghol.knotx.data.database.CallSession

data class CallSessionDisplay(
    val callSession: CallSession,
    val userName: String?,
    val photoUrl: String?
)
