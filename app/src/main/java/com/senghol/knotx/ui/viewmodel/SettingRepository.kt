package com.senghol.knotx.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.senghol.knotx.utils.settingStore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class SettingsRepository(context: Context) {

    private val dataStore = context.settingStore

    // Read all key-value settings and convert them to JsonObjects
    suspend fun getAllSettings(): Map<String, JsonObject> {
        val prefs = dataStore.data.first()
        return prefs.asMap().mapKeys { it.key.name }.mapValues { entry ->
            val json = JsonObject()
            when (val value = entry.value) {
                is Boolean -> json.addProperty("enabled", value)
                is String -> json.addProperty("value", value)
                is Int, is Float, is Double -> json.addProperty("value", value.toString())
                else -> json.addProperty("value", value.toString())
            }
            json
        }
    }

    // Update a specific key with supported primitive types
    suspend fun updateSetting(key: String, value: Any) {
        dataStore.edit { prefs ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is Int -> prefs[intPreferencesKey(key)] = value
                is Float -> prefs[floatPreferencesKey(key)] = value
                is Double -> prefs[stringPreferencesKey(key)] = value.toString() // Store Double as String
                is String -> prefs[stringPreferencesKey(key)] = value
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.simpleName}")
            }
        }
    }
    suspend fun initializeDefaultsIfNeeded(defaults: Map<String, String>) {
        val prefs = dataStore.data.first()

        if (prefs[booleanPreferencesKey("settings_initialized")] == true) return

        dataStore.edit { settings ->
            defaults.forEach { (key, jsonValue) ->
                settings[stringPreferencesKey(key)] = jsonValue
            }
            settings[booleanPreferencesKey("settings_initialized")] = true
        }
    }

}