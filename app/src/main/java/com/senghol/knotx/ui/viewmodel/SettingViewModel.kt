package com.senghol.knotx.ui.viewmodel


import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.objects.PrefKeys
import com.senghol.knotx.utils.settingStore
import com.senghol.knotx.utils.userDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingStore = application.applicationContext.settingStore
    private val userDataStore = application.applicationContext.userDataStore


    // ───────────────────────────────────────────────────────
    // User Data (from userDataStore)
    // ───────────────────────────────────────────────────────
    suspend fun getIsRegistered(): Boolean {
        return userDataStore.data
            .map { prefs -> prefs[PrefKeys.IS_REGISTERED] == true }
            .first() // suspends until first value is emitted
    }


    val userNameFlow: Flow<String?> = userDataStore.data
        .map { prefs -> prefs[PrefKeys.USER_NAME] }

    val phoneNumberFlow: Flow<String?> = userDataStore.data
        .map { prefs -> prefs[PrefKeys.USER_NUMBER] }

    private val _profileImagePathFlow = MutableStateFlow<String?>(null)
    val profileImagePathFlow: StateFlow<String?> = _profileImagePathFlow

    init {
        viewModelScope.launch {
            userDataStore.data.map { prefs ->
                prefs[PrefKeys.PROFILE_IMAGE]
            }.collect {
                _profileImagePathFlow.value = it
            }
        }
    }

    suspend fun updateUserName(newName: String) {
        userDataStore.edit { prefs ->
            prefs[PrefKeys.USER_NAME] = newName
        }
    }

    suspend fun getUserName(): String? {
        return userDataStore.data.map { it[PrefKeys.USER_NAME] }.firstOrNull()
    }

    // Set the user's profile image path
    fun setProfileImage(path: String) {
        viewModelScope.launch {
            userDataStore.edit { prefs ->
                prefs[PrefKeys.PROFILE_IMAGE] = path
            }}
    }

    // Remove profile image
    fun removeProfileImage() {
        viewModelScope.launch {
            userDataStore.edit { prefs ->
                prefs.remove(PrefKeys.PROFILE_IMAGE)
            }
            _profileImagePathFlow.value = null
        }
    }


    suspend fun getProfileImagePath(): String? {
        return userDataStore.data.map { it[PrefKeys.PROFILE_IMAGE] }.firstOrNull()
    }

    suspend fun setUserRegistered(isRegistered: Boolean) {
        
        userDataStore.edit { prefs ->
            prefs[PrefKeys.IS_REGISTERED] = isRegistered
            
        }
    }



    // App Settings (from settingStore)
    private val _isPasscodeEnabledFlow = MutableStateFlow(false)
    val isPasscodeEnabledFlow: StateFlow<Boolean> = _isPasscodeEnabledFlow

    val settingsFlow: StateFlow<Map<Preferences.Key<*>, Any?>> =
        settingStore.data
            .map { prefs -> prefs.asMap() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    init {
        viewModelScope.launch {
            settingStore.data.map { prefs ->
                prefs[SettingPrefKeys.ACCOUNT_SECURITY_PASSCODE] != null
            }.collect {
                _isPasscodeEnabledFlow.value = it
            }
        }
    }

    fun getPasscode(): Flow<String?> {
        return settingStore.data.map { preferences ->
            preferences[SettingPrefKeys.ACCOUNT_SECURITY_PASSCODE]
        }
    }

    fun setPasscode(passcode: String) {
        viewModelScope.launch {
            settingStore.edit { prefs ->
                prefs[SettingPrefKeys.ACCOUNT_SECURITY_PASSCODE] = passcode
            }
            _isPasscodeEnabledFlow.value = true
        }
    }

    fun removePasscode() {
        viewModelScope.launch {
            settingStore.edit { prefs ->
                prefs.remove(SettingPrefKeys.ACCOUNT_SECURITY_PASSCODE)
            }
            _isPasscodeEnabledFlow.value = false
        }
    }

    fun updateBooleanSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            settingStore.edit { prefs -> prefs[key] = value }
        }
    }

    fun updateIntSetting(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch {
            settingStore.edit { prefs -> prefs[key] = value }
        }
    }

    fun updateStringSetting(key: Preferences.Key<String>, value: String) {
        viewModelScope.launch {
            settingStore.edit { prefs -> prefs[key] = value }
        }
    }

    fun clearSetting(key: Preferences.Key<*>) {
        viewModelScope.launch {
            settingStore.edit { prefs -> prefs.remove(key) }
        }
    }

}
