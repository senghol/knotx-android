package com.senghol.knotx.utils.enums

enum class SandeshType(val value: Int) {
    MESSAGE(0),
    IMAGE(1),
    VIDEO(2), //todo check why we did not use 3 here or just forgot it
    AUDIO(4),
    DOCUMENT(5),
    CONTACT(6),
    LOCATION(7),
    DELETE(8),
    REPLY_MESSAGE(9),
    REPLY_IMAGE(10),
    REPLY_VIDEO(11),
    REPLY_AUDIO(12),
    REPLY_DOCUMENT(13),
    REPLY_CONTACT(14),
    REPLY_LOCATION(15),
    CREATE_GROUP(17),
    MEMBERS_UPDATED(18),
    REACTION(19),
    REQUEST_DISAPPEARING(20),
    RESPONSE_DISAPPEARING(21),
    GROUP_UPDATED(22),
    UNIVERSAL_MESSAGE(23),
    LIVE_LOCATION_UPDATE(24),
    SCREEN_COLORS(25),
    LEDGER(26),
    LIST(27),
    REPLY_LIST(28),
    GROUP_KEY_SETUP(29),
    GROUP_KEY_REQUEST(30),
    GROUP_KEY_RESPONSE(31),
    EPOCH_COMMIT(32),
    AI_MESSAGE(33);
    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: MESSAGE
    }

    fun getLabel(): String = when (this) {
        MESSAGE, REPLY_MESSAGE -> "Messages"
        IMAGE, REPLY_IMAGE -> "Images"
        VIDEO, REPLY_VIDEO -> "Videos"
        AUDIO, REPLY_AUDIO -> "Audio"
        DOCUMENT, REPLY_DOCUMENT -> "Documents"
        CONTACT, REPLY_CONTACT -> "Contacts"
        LOCATION, REPLY_LOCATION -> "Locations"
        else -> "Media"
    }
}


