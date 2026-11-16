package com.senghol.knotx.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore


// ✅ These must be top-level properties (outside any class)
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "dakiya_prefs_user")
val Context.settingStore: DataStore<Preferences> by preferencesDataStore(name = "dakiya_prefs_setting")
