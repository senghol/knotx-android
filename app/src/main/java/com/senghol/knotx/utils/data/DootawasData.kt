package com.senghol.knotx.utils.data

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Embedded
import com.google.gson.annotations.SerializedName
import com.senghol.knotx.data.database.CallSession
import com.senghol.knotx.data.database.StoryEntity
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.FileDataType
import com.senghol.knotx.utils.enums.FileDataTypeToken
import com.senghol.knotx.utils.enums.JsonKeys

data class UserColors(
    val darkModeColor: Int,
    val lightModeColor: Int
)


data class AESKeyGroup(
    val id: String,
    val aesKey: String,
    val createdAt: Long
)

data class VideoMeta(
    val width: Int,
    val height: Int,
    val durationMs: Long
)

data class EncryptedKeyBlob(
    val encrypted: String,
    val iv: String
)


data class AckKey(
    val messageId: String,
    val receiver: String,
    val dataType: DataType
)

data class GroupMessageMeta(
    val sandeshId: String,
    val userNumber: String
)

data class ActiveLiveLocation(

    val sandeshId: String,

    val targetId: String,

    val isGroup: Boolean
)

data class ContactPhotoProjection(
    val number: String,
    val imageUrl: String
)

data class FileUploadResponse(
    val success: Boolean,
    val fileTokenType: Int,
    val messageId: String,
    val uploadUrl: String,
    val dataType: Int,
)

data class FileDownloadResponse(
    val success: Boolean,
    val messageId: String,
    val downloadUrl: String?,
    val message: String? = null
)

data class ProfileUploadResponse(
    val dataBoolean: Boolean,
    val messageId: String,
    val uploadUrl: String
)

data class ProfileDownloadResponse(
    val dataBoolean: Boolean,
    val messageId: String,
    val downloadUrl: String
)

 sealed class UserNameIdCheckState {
     object Idle : UserNameIdCheckState()
     object Checking : UserNameIdCheckState()
     object Available : UserNameIdCheckState()
     object Taken : UserNameIdCheckState()
     data class Invalid(val reason: String) : UserNameIdCheckState()
     data class Error(val message: String) : UserNameIdCheckState()
 }

sealed class UpdateUserNameIdResult {
    object Success : UpdateUserNameIdResult()
    object Taken : UpdateUserNameIdResult()
    data class CooldownActive(val availableAt: Long) : UpdateUserNameIdResult()
    data class Failed(val reason: String) : UpdateUserNameIdResult()
}

data class HomeTab(
    val title: String,
    val icon: ImageVector,
    val unreadCount: Int
)


// 1. Domain / UI model
data class CheckedContact(
    val phone: String,
    val state: ContactState,
    val displayName: String? = null,     // from server if exists
    val profileUri: String
)

enum class ContactState {
    Checking,
    Exists,           // can invite / add
    NotFound,
    AlreadyAdded,     // optional - if you track already in contacts
    Error     , // network / timeout / server error
    INVALID
}

data class CallWithUser(
    @Embedded val callSession: CallSession,

    val contactName: String?,
    val photoUrl: String?
)
/** Represents a grouped collection of active stories for a specific user. */
data class UserStoryGroup(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val stories: List<StoryEntity>,
    val hasUnseenStories: Boolean,
    val lastUpdated: Long = stories.maxOfOrNull { it.createdAt } ?: 0L
)


data class AiWalletBalance(
    val balance: Long = 0,            // total credits the user can spend
    val freeMonthlyTarget: Long = 0   // monthly free top-up level, for UI hints like "10,000 free every month"
)