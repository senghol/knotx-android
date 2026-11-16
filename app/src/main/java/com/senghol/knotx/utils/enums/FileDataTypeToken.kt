package com.senghol.knotx.utils.enums

enum class FileDataTypeToken(val value: Int) {

    // File uploads
    FILE_UPLOAD_REQUEST(0),
    FILE_UPLOAD_CONFIRM(1),
    FILE_DOWNLOAD_REQUEST(2),

    // User profile
    PROFILE_UPLOAD_REQUEST(10),
    PROFILE_DOWNLOAD_REQUEST(11),
    PROFILE_DELETE(12),

    // Group profile
    GROUP_PROFILE_UPLOAD(20),
    GROUP_PROFILE_DOWNLOAD_REQUEST(21),
    GROUP_PROFILE_DELETE(22),

    //Status
    STORY_UPLOAD_REQUEST(30),
    STORY_DOWNLOAD_REQUEST(31),
    STORY_UPLOAD_COMPLETE(32),
    GET_STORIES_DIRECT(33);

    companion object {
        fun fromValue(value: Int): FileDataTypeToken =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown FileDataType: $value")
    }
}