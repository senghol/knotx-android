package com.senghol.knotx.utils.enums

enum class InternalStorageType(
    val folder: String,
    val prefix: String,
    val defaultExtension: String
) {

    IMAGE(
        "userdata/Images",
        "IMG",
        "jpg"
    ),

    VIDEO(
        "userdata/Videos",
        "VID",
        "mp4"
    ),

    AUDIO(
        "userdata/Audio",
        "AUD",
        "m4a"
    ),

    DOCUMENT(
        "userdata/Documents",
        "DOC",
        "dat"
    ),

    PREVIEW(
        "userdata/Preview",
        "PREVIEW",
        "jpg"
    ),

    PROFILE(
        "userdata/Profiles",
        "PROFILE",
        "jpg"
    ),

    DRAFT(
        "userdata/Drafts",
        "DRAFT",
        "dat"
    )
}