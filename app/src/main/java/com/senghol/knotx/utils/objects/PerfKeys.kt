package com.senghol.knotx.utils.objects

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PrefKeys {
    // User-related
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_PASS = stringPreferencesKey("user_pass")
    val USER_NUMBER = stringPreferencesKey("user_number")
    val FCM_TOKEN = stringPreferencesKey("fcm_token")
    val IS_REGISTERED = booleanPreferencesKey("is_registered")
    val REGION = stringPreferencesKey("user_region")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val PROFILE_IMAGE = stringPreferencesKey("profile_image")
    val PROFILE_TIME = longPreferencesKey("profile_time")
    val USER_ABOUT = stringPreferencesKey("user_about")
    val DELIVERY_TOKEN = stringPreferencesKey("delivery_token")
    val FILE_TOKEN = stringPreferencesKey("file_token")
    val AI_WALLET_BALANCE = stringPreferencesKey("ai_wallet_balance")
    val FILE_TOKEN_TIME = longPreferencesKey("file_token_time")
    val DELIVERY_TOKEN_TIME = longPreferencesKey("delivery_token_time")

    val NOTIFICATION_COUNT = intPreferencesKey("notification_count")

    val LAST_MESSAGE_IDS = stringSetPreferencesKey("last_message_ids")
    val DOOTAWAS_THEME = stringPreferencesKey("dootawas_theme_json")
    val DOOTAWAS_GROUP_THEME = stringPreferencesKey("dootawas_group_theme_json")

    val APP_ICON = stringPreferencesKey("app_icon")
    val USER_NAME_ID = stringPreferencesKey("user_name_id") // stored WITHOUT the "@"
    val USER_NAME_ID_UPDATE_TIME = longPreferencesKey("user_name_id_time")
    val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")

    fun signalSessionId(identifier: String) = stringPreferencesKey("signal_session_$identifier")
    val REVEALED_PHONE_NUMBERS = stringSetPreferencesKey("revealed_phone_numbers")
    val REVIEW_FIRST_OPEN = longPreferencesKey("review_first_open")
    val REVIEW_OPEN_COUNT = intPreferencesKey("review_open_count")
    val REVIEW_LAST_ASKED = longPreferencesKey("review_last_asked")
}


object OldPrefKeys {//delete this 1 year later in Sep 2027
    val USER_PHONE = stringPreferencesKey("user_phone")
}
