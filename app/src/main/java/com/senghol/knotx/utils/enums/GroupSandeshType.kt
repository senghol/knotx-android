package com.senghol.knotx.utils.enums
enum class GroupSandeshType(val value: Int) {
    // Standard Messages & Media
    MESSAGE(0),
    IMAGE(1),
    VIDEO(2),
    AUDIO(4),
    DOCUMENT(5),
    CONTACT(6),
    LOCATION(7),
    DELETE(8),

    // Replies
    REPLY_MESSAGE(9),
    REPLY_IMAGE(10),
    REPLY_VIDEO(11),
    REPLY_AUDIO(12),
    REPLY_DOCUMENT(13),
    REPLY_CONTACT(14),
    REPLY_LOCATION(15),

    // Group & Protocol Operations (SandeshType)
    MEMBERS_UPDATED(18),
    REACTION(19),
    REQUEST_KEY_ROTATION(20),
    RECEIVED_GROUP_KEY(21),
    GROUP_UPDATED(22),
    UNIVERSAL_MESSAGE(23),
    LIVE_LOCATION_UPDATE(24),
    SCREEN_COLORS(25),
    LEDGER(26),
    LIST(27),
    REPLY_LIST(28),
    GROUP_KEY_PACKAGE(29),
    EPOCH_COMMIT(30),

    // Administrative Group Actions (GroupAction mapping starting offset at 100 to prevent value collision)
    ACTION_CREATE_GROUP(101),
    ACTION_DELETE_PROFILE(102),
    ACTION_DELETE_GROUP(103),
    ACTION_ADD_MEMBERS(104),
    ACTION_INVITE_MEMBER(105),
    ACTION_REMOVE_MEMBER(106),
    ACTION_LEAVE_GROUP(107),
    ACTION_RESTORE(108),
    ACTION_GET_GROUPS(109),
    ACTION_GET_GROUP(110),
    ACTION_ERROR(111),
    ACTION_GET_MEMBERS(112),
    ACTION_MEMBER_UPDATED(113),
    ACTION_GROUP_UPDATED(114),
    ACTION_MEMBER_PERMISSION_UPDATED(115),
    ACTION_GET_ALL_MEMBERS_ADDED(116),
    ACTION_GET_ALL_MEMBERS_REINSTALLED(117);


    companion object {
        private val map = entries.associateBy(GroupSandeshType::value)

        fun fromInt(value: Int): GroupSandeshType = map[value] ?: MESSAGE
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

    fun isGroupAction(): Boolean = value >= 100
}