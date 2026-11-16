package com.senghol.knotx.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.senghol.knotx.utils.objects.PrefKeys
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

/**
 * Single place for all DataStore-backed user preferences — profile fields,
 * FCM token, username-id state, etc. Get it via DootawasStore.getInstance(context)
 * from anywhere; it's a per-process singleton, same pattern as SignalDatabase.
 */
class DootawasStore(context: Context) {

    // No Context field stored on the instance — just the DataStore handle itself.
    private val dataStore = context.userDataStore



    // ==================== READ (flows) ====================

    val userNameKhud: Flow<String?> = dataStore.data.map { it[PrefKeys.USER_NAME] }
    val userNumberKhud: Flow<String?> = dataStore.data.map { it[PrefKeys.USER_NUMBER] }
    val userNameIdKhud: Flow<String?> = dataStore.data.map { it[PrefKeys.USER_NAME_ID] }
//    val aboutKhud: Flow<String?> = dataStore.data.map { it[PrefKeys.ABOUT] }
//    val fcmTokenKhud: Flow<String?> = dataStore.data.map { it[PrefKeys.FCM_TOKEN] }
//    val userNameIdCooldownUntil: Flow<Long?> = dataStore.data.map { it[PrefKeys.USER_NAME_ID_COOLDOWN_UNTIL] }
//
//    // ==================== WRITE (setters) ====================
//    // null clears the key entirely rather than storing an empty string —
//    // keeps isNullOrBlank() checks elsewhere meaningful.
//
//    suspend fun setUserName(value: String?) = write(PrefKeys.USER_NAME, value)
//    suspend fun setPhoneNumber(value: String?) = write(PrefKeys.USER_NUMBER, value)
//    suspend fun setUserNameId(value: String?) = write(PrefKeys.USER_NAME_ID, value)
//    suspend fun setAbout(value: String?) = write(PrefKeys.ABOUT, value)
//    suspend fun setFcmToken(value: String?) = write(PrefKeys.FCM_TOKEN, value)
//
//    suspend fun setUserNameIdCooldownUntil(timestampMillis: Long?) {
//        dataStore.edit {
//            if (timestampMillis == null) it.remove(PrefKeys.USER_NAME_ID_COOLDOWN_UNTIL)
//            else it[PrefKeys.USER_NAME_ID_COOLDOWN_UNTIL] = timestampMillis
//        }
//    }

    private suspend fun write(key: Preferences.Key<String>, value: String?) {
        dataStore.edit {
            if (value.isNullOrBlank()) it.remove(key) else it[key] = value
        }
    }

    /** Clears every key this store owns — e.g. on logout. */
    /** Clears every key this store owns — e.g. on logout. */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }


    private val _askReview = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val askReview = _askReview.asSharedFlow()

    suspend fun checkReview() {
        recordAppOpen()
        delay(30_000.milliseconds)
        if (shouldAskForReview()) _askReview.emit(Unit)
    }

    suspend fun recordAppOpen() {
        dataStore.edit { p ->
            if (p[PrefKeys.REVIEW_FIRST_OPEN] == null) {
                p[PrefKeys.REVIEW_FIRST_OPEN] = System.currentTimeMillis()
            }
            p[PrefKeys.REVIEW_OPEN_COUNT] = (p[PrefKeys.REVIEW_OPEN_COUNT] ?: 0) + 1
        }
    }

    suspend fun shouldAskForReview(): Boolean {
        val p = dataStore.data.first()
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val firstOpen = p[PrefKeys.REVIEW_FIRST_OPEN] ?: return false
        return now - firstOpen >= 3 * day &&
                (p[PrefKeys.REVIEW_OPEN_COUNT] ?: 0) >= 5 &&
                now - (p[PrefKeys.REVIEW_LAST_ASKED] ?: 0L) >= 60 * day
    }

    suspend fun markReviewAsked() {
        dataStore.edit { it[PrefKeys.REVIEW_LAST_ASKED] = System.currentTimeMillis() }
    }
}