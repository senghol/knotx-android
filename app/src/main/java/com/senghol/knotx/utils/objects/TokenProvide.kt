package com.senghol.knotx.utils.objects

import android.content.Context
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.flow.first

object TokenProvider {



    private const val TOKEN_VALIDITY_MS = 350 * 60 * 1000L

    suspend fun getValidFileToken(context: Context): String? {

        val prefs = context.userDataStore.data.first()

        val token = prefs[PrefKeys.FILE_TOKEN]
        val tokenTime = prefs[PrefKeys.FILE_TOKEN_TIME] ?: 0L

        val currentTime = System.currentTimeMillis()
        val isExpired = (currentTime - tokenTime) > TOKEN_VALIDITY_MS

        

        return if (!token.isNullOrEmpty() && !isExpired) {
            
            token
        } else {
            
            null
        }
    }
}