package com.senghol.knotx.utils.enums

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object SettingPrefKeys {




    // Notification Settings
    val NOTIFICATION_ENABLE_ALL = booleanPreferencesKey("notification_enable_all")
    val MESSAGE_SOUND = booleanPreferencesKey("notification_sound")
    val NOTIFICATION_LED = stringPreferencesKey("notification_led")

    // Message Notifications
    val NOTIFICATION_NEW_MESSAGES = booleanPreferencesKey("notification_new_messages")
    val NOTIFICATION_GROUP_CHATS = booleanPreferencesKey("notification_group_chats")
    val NOTIFICATION_MENTIONS = booleanPreferencesKey("notification_mentions")
    val MESSAGE_VIBRATION = booleanPreferencesKey("notification_vibration")

    // Call Notifications
    val NOTIFICATION_INCOMING_CALLS = booleanPreferencesKey("notification_incoming_calls")
    val NOTIFICATION_MISSED_CALL_ALERTS = booleanPreferencesKey("notification_missed_call_alerts")

    // In-App Notifications
    val NOTIFICATION_POPUP_PREVIEWS = booleanPreferencesKey("notification_popup_previews")
    val NOTIFICATION_BADGE_COUNT = booleanPreferencesKey("notification_badge_count")

    // Do Not Disturb
    val NOTIFICATION_DND_ENABLE = booleanPreferencesKey("notification_dnd_enable")
    val NOTIFICATION_DND_CUSTOM_HOURS = stringPreferencesKey("notification_dnd_custom_hours")
    val NOTIFICATION_DND_ALLOW_EXCEPTIONS =
        booleanPreferencesKey("notification_dnd_allow_exceptions")

    // Chat Preferences
    val CHAT_CHAT_THEME = booleanPreferencesKey("chat_chat_theme")
    val CHAT_CHAT_WALLPAPER = stringPreferencesKey("chat_chat_wallpaper")
    val CHAT_CHAT_FONT_SIZE = intPreferencesKey("chat_chat_font_size")
    val SWIPE_TUTORIAL_SHOWN = booleanPreferencesKey("swipe_tutorial_shown")
    val CHAT_CHAT_ENTER_KEY_BEHAVIOR = booleanPreferencesKey("chat_chat_enter_key_behavior")

    // Messages Settings
    val CHAT_MESSAGE_READ_RECEIPTS = booleanPreferencesKey("chat_message_read_receipts")
    val CHAT_MESSAGE_TYPING_INDICATORS = booleanPreferencesKey("chat_message_typing_indicators")
    val CHAT_MESSAGE_BACKUP = booleanPreferencesKey("chat_message_backup")
    val CHAT_MESSAGE_AUTO_DELETE = booleanPreferencesKey("chat_message_auto_delete")

    // Media Settings
    val CHAT_MEDIA_AUTO_DOWNLOAD_PHOTOS = booleanPreferencesKey("chat_media_auto_download_photos")
    val CHAT_MEDIA_AUTO_DOWNLOAD_VIDEOS = booleanPreferencesKey("chat_media_auto_download_videos")
    val CHAT_MEDIA_AUTO_DOWNLOAD_VOICE = booleanPreferencesKey("chat_media_auto_download_voice")
    val CHAT_MEDIA_AUTO_DOWNLOAD_FILES = booleanPreferencesKey("chat_media_auto_download_files")
    val CHAT_MEDIA_QUALITY = stringPreferencesKey("chat_media_quality")
    val CHAT_MEDIA_SAVE_TO_GALLERY = booleanPreferencesKey("chat_media_save_to_gallery")

    // Group Chats Settings
    val CHAT_GROUP_CHAT_DEFAULT_PERMISSIONS =
        stringPreferencesKey("chat_group_chat_default_permissions")
    val CHAT_GROUP_CHAT_MUTE_ALL = booleanPreferencesKey("chat_group_chat_mute_all")

    // Blocked Contacts
    val CHAT_BLOCKED_CONTACTS_VIEW = booleanPreferencesKey("chat_blocked_contacts_view")

    // Security
    val ACCOUNT_SECURITY_2FA = booleanPreferencesKey("account_security_two_factor_authentication")
    val ACCOUNT_SECURITY_ACTIVE_SESSIONS = booleanPreferencesKey("account_security_active_sessions")
    val ACCOUNT_SECURITY_PASSCODE = stringPreferencesKey("account_security_passcode_lock")
    val ACCOUNT_SECURITY_PRIVACY = booleanPreferencesKey("account_security_privacy")

    val SOS_CONTACT_NUMBERS = stringSetPreferencesKey("sos_contact_numbers")
    // Call Settings
    val CALL_SPEAKER_DEFAULT = booleanPreferencesKey("call_speaker_default")
    val CALL_RINGTONE = stringPreferencesKey("call_ringtone")
    val CALL_VIDEO_QUALITY = stringPreferencesKey("call_video_quality")


    // Privacy Settings

    // 👇 Privacy List (contacts / users / groups)
    val PRIVACY_LIST = stringSetPreferencesKey("privacy_list")
    val SERVER_BLOCKED_USERS = stringSetPreferencesKey("server_blocked_list")
    val PRIVACY_LAST_SEEN       = intPreferencesKey("privacy_last_seen")
    val PRIVACY_ONLINE_STATUS   = intPreferencesKey("privacy_online_status")
    val PRIVACY_PROFILE_PHOTO   = intPreferencesKey("privacy_profile_photo")
    val PRIVACY_BIO             = intPreferencesKey("privacy_bio")
    val PRIVACY_MESSAGES        = intPreferencesKey("privacy_messages")
    val PRIVACY_GROUP_INVITES   = intPreferencesKey("privacy_group_invites")
    val CALL_ALLOW_FROM = intPreferencesKey("call_allow_from")
    val PRIVACY_ANALYTICS = booleanPreferencesKey("privacy_analytics")
    val PRIVACY_THIRD_PARTY_SHARING = booleanPreferencesKey("privacy_third_party_sharing")
    val PRIVACY_LOGIN_ALERT = booleanPreferencesKey("privacy_login_alert")
    val PRIVACY_SUSPICIOUS_ACTIVITY = booleanPreferencesKey("privacy_suspicious_activity")

    // Data Usage
// 📩 Message Data
    val DATA_USAGE_MOBILE_MESSAGE_SENT = intPreferencesKey("data_usage_mobile_message_sent")
    val DATA_USAGE_MOBILE_MESSAGE_RECEIVED = intPreferencesKey("data_usage_mobile_message_received")
    val DATA_USAGE_WIFI_MESSAGE_SENT = intPreferencesKey("data_usage_wifi_message_sent")
    val DATA_USAGE_WIFI_MESSAGE_RECEIVED = intPreferencesKey("data_usage_wifi_message_received")

    // 🖼️ Media Data
    val DATA_USAGE_MOBILE_MEDIA_SENT = intPreferencesKey("data_usage_mobile_media_sent")
    val DATA_USAGE_MOBILE_MEDIA_RECEIVED = intPreferencesKey("data_usage_mobile_media_received")
    val DATA_USAGE_WIFI_MEDIA_SENT = intPreferencesKey("data_usage_wifi_media_sent")
    val DATA_USAGE_WIFI_MEDIA_RECEIVED = intPreferencesKey("data_usage_wifi_media_received")

    val DATA_USAGE_CALL_SENT = intPreferencesKey("data_usage_call_data_sent")
    val DATA_USAGE_CALL_RECEIVED = intPreferencesKey("data_usage_call_data_received")

    val MOBILE_DATA_LIMIT = stringPreferencesKey("mobile_data_limit")
    val WIFI_USAGE = stringPreferencesKey("wifi_usage")
    val CLEAR_CACHE = stringPreferencesKey("clear_cache")
    val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
    val BACKUP_LOCATION = stringPreferencesKey("backup_location")
    val BACKUP_RETENTION_COUNT = intPreferencesKey("backup_retention_count")
    val MEDIA_RETENTION_MONTHS = intPreferencesKey("media_retention_months")

    // Language & Region
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val APP_REGION = stringPreferencesKey("app_region")
    val USE_SYSTEM_LANGUAGE = booleanPreferencesKey("use_system_language")
    val NUMBER_FORMAT = stringPreferencesKey("number_format")

    // UI Settings
    val APP_THEME = stringPreferencesKey("app_theme")
    val BUBBLE_STYLE = stringPreferencesKey("bubble_style")
    val COLOR_SCHEME = stringPreferencesKey("color_scheme")
    val APP_ICON = stringPreferencesKey("app_icon")
    val HOME_SCREEN_LAYOUT = stringPreferencesKey("home_screen_layout")
    val OPTIONAL_UPDATE_SHOWN_VERSION = stringPreferencesKey("optional_update_shown_version")


    //BACKUP
    val ENABLE_BACKUP = booleanPreferencesKey("enable_backup")
    val INCLUDE_MEDIA = booleanPreferencesKey("include_media")
    val BACKUP_FREQUENCY_DAYS = intPreferencesKey("backup_frequency_days")
    val BACKUP_ACCOUNT = stringPreferencesKey("backup_account")
    val DRIVE_CONNECTED = booleanPreferencesKey("drive_connected")
    val NEEDS_REAUTHORIZATION = booleanPreferencesKey("needs_re_auth")
    val BACKUP_WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
    val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")

    val BACKUP_DIRECTORY_URI = stringPreferencesKey("backup_directory")
//    val BACKUP_DIRECTORY_URI= stringPreferencesKey("backup_directory")
    val ONE_TIME_RUN_VERSION = intPreferencesKey("one_time_run_version")
    val BACKUP_NEXT_SHOW_MS = longPreferencesKey("backup_reminder_next_show")  // -1 = never, 0 = first time
    val BATTERY_NEXT_SHOW_MS = longPreferencesKey("battery_next_show_ms")


    val DONATION_NEXT_SHOW_MS = longPreferencesKey("donation_next_show")
    val DONATION_DISMISSED = booleanPreferencesKey("donation_dismissed")
    val MIUI_PERMISSION_SHOWN = booleanPreferencesKey("miui_permission_shown")

    val PENDING_RESTORE_CLEANUP = booleanPreferencesKey("pending_restore_cleanup")
}