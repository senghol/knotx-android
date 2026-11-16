package com.senghol.knotx.utils.objects

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PrefsHelper {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_IS_REGISTERED = "is_registered"
    // Add these for language
    private const val KEY_USE_SYSTEM_LANGUAGE = "use_system_language" // Or use SettingPrefKeys.USE_SYSTEM_LANGUAGE if defined
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_DIRECT_ID = "direct_id"
    fun setIsRegistered(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_IS_REGISTERED, value) }
    }

    fun getIsRegistered(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
    }
    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }

    // New: Language methods
    fun setUseSystemLanguage(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { putBoolean(KEY_USE_SYSTEM_LANGUAGE, value) }
    }

    fun getUseSystemLanguage(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_SYSTEM_LANGUAGE, true) // Default to true (use system)
    }

    fun setAppLanguage(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { putString(KEY_APP_LANGUAGE, value) }
    }

    fun getAppLanguage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_LANGUAGE, null)
    }

    // 🔥 Live state
    private val _directIdFlow = MutableStateFlow<String?>(null)
    val directIdFlow: StateFlow<String?> = _directIdFlow

    fun setDirectId(context: Context, id: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            if (id == null) remove(KEY_DIRECT_ID)
            else putString(KEY_DIRECT_ID, id)
        }

        // 🔥 Immediately emit live update
        _directIdFlow.value = id
    }

    fun getDirectId(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DIRECT_ID, null)
    }

    // 🔥 Call this once at app start
    fun init(context: Context) {
        val saved = getDirectId(context)
        if (_directIdFlow.value != saved) {
            _directIdFlow.value = saved
        }
    }

}
