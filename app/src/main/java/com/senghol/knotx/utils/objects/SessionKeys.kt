package com.senghol.knotx.utils.objects

object SessionKeys {
    @Volatile var serverSessionId: String? = null
    @Volatile var serverAESKey: String? = null
    @Volatile var password: String? = null
}