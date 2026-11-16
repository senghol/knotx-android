package com.senghol.knotx.utils.data

import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.JsonObject
import com.senghol.knotx.ui.components.BubbleType
import com.senghol.knotx.ui.components.LedgerStatus
import com.senghol.knotx.ui.components.LedgerType
import com.senghol.knotx.ui.components.ListData
import com.senghol.knotx.ui.components.ListItem
import com.senghol.knotx.ui.components.ListMessage
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType
// Define the common type

sealed interface ParsedSandeshItem {
    val sandeshId: String
    val time: Long
    val formattedDate: String
    val sandeshStithi: SandeshStithi
    val isSelf: Boolean
}

const val MSG_TEXT = 0
const val MSG_SINGLE_EMOJI = 1
const val MSG_LINK = 2
data class ReactionUi(
    val userId: String,
    val emoji: String
)
data class ParsedLedgerSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    override val time: Long,
    val formattedTime: String,
    override val formattedDate: String,
    override val sandeshStithi: SandeshStithi,
    val ledgerType: LedgerType,
    val ledgerStatus: LedgerStatus,
    val amount: Double,
    val isPositive: Boolean,
    val contactName: String,
    val note: String,
    val resolvedBalance: Double?,
    val billTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val invoiceNumber: String = "",
    val dataJson: JsonObject  // full parsed object for preview screen
) : ParsedSandeshItem  // whatever your base sealed class / interface is

data class ParsedDateHeaderSandesh(
    override val sandeshId: String,            // "date_header_<epochDay>"
    override val time: Long,                   // timestamp of the day's oldest message
    val epochDay: Long,                        // the day this header represents
    override val formattedDate: String,        // short: "Today", "Yesterday", "Monday", "12 Jun 2025"
    val detailedDate: String,                  // expanded: "Sunday, 4 October 2026 · Today"
    override val sandeshStithi: SandeshStithi, // required by ParsedSandeshItem
    override val isSelf: Boolean = false
) : ParsedSandeshItem

data class ParsedTextSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val msgType: Int,
    val bubbleType: BubbleType,
    override val time: Long,                  // 👈 Raw timestamp (keep it for sorting/comparison)
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    override val sandeshStithi: SandeshStithi,
    val paath: String,
    val replyOriginalMessage: JsonObject? = null,
    val linkTitle: String? = null,
    val linkUri: String? = null,
) : ParsedSandeshItem
sealed interface AiBlock {
    data class Text(val text: String) : AiBlock
    data class Heading(val level: Int, val text: String) : AiBlock
    data class Code(val lang: String, val code: String) : AiBlock
    data class Table(
        val header: List<String>,
        val align: List<String>,          // "left" | "center" | "right"
        val rows: List<List<String>>
    ) : AiBlock
    data class ListBlock(val ordered: Boolean, val items: List<AiListItem>) : AiBlock
    data class Quote(val text: String) : AiBlock
    data object Divider : AiBlock
}

data class AiListItem(val level: Int, val text: String)

data class ParsedAiSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val bubbleType: BubbleType,
    override val time: Long,
    val formattedTime: String,
    override val formattedDate: String,
    override val sandeshStithi: SandeshStithi,
    val paath: String,                    // full or partial raw Markdown (fallback)
    val blocks: List<AiBlock>,            // empty only if paath is blank
    val isTyping: Boolean,                // true until the final blocks arrive
    val status: String                    // OK / NO_CREDITS / BUSY / ERROR ...
) : ParsedSandeshItem

data class ParsedListSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    override val time: Long,
    override val formattedDate: String,
    val formattedTime: String,
    override val sandeshStithi: SandeshStithi,

    val listMessage: ListMessage,

    val displayList: ListData,

    val title: String,

    val completed: Int,
    val total: Int,
    val progress: Float,

    val previewItems: List<ListItem>,
    val remainingItems: Int,

    val isUpdate: Boolean,
) : ParsedSandeshItem
data class ParsedUniversalSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    override val time: Long,                  // 👈 Raw timestamp (keep it for sorting/comparison)
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    override val sandeshStithi: SandeshStithi,
    val paath: String,
    val icon: ImageVector
) : ParsedSandeshItem


data class ParsedImageSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,
    override val formattedDate: String,

    //todo use directly uri instead of string in display uri and blur uri and update to all places
    val imageUris: List<String>,        // ✅ NEW (main data)
    val displayUri: String?,       // ✅ Only one field now
    val downloadPercent: Int,
    val paath: String = "",
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,
    val replyOriginalMessage: JsonObject? = null,
    val blurUri: String?,
) : ParsedSandeshItem


data class ParsedVideoSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,
    override val formattedDate: String,
    val paath: String = "",
    val videoUris: List<String> = emptyList(),
    val previewUris: List<String> = emptyList(),
    val blurUri: String?,
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,
    val replyOriginalMessage: JsonObject? = null,
) : ParsedSandeshItem

data class ParsedAudioSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val savedFileUri: String?,
    val duplicateUri: String?,
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,
    val replyOriginalMessage: JsonObject? = null,
    val duration: Long
) : ParsedSandeshItem

data class ParsedDocumentSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val savedFileUri: String?,
    val duplicateUri: String?,
    val mimeType: String,
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,
    val replyOriginalMessage: JsonObject? = null,
) : ParsedSandeshItem

data class ParsedContactSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val contacts: List<ContactInfo>,  // List of contacts
    override val sandeshStithi: SandeshStithi,
    val replyOriginalMessage: JsonObject? = null,
) : ParsedSandeshItem

data class ContactInfo(
    val name: String,
    val phone: String
)
data class ParsedLocationSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val savedFileUri: String?,       // Screenshot if available
    val duplicateUri: String?,       // Preview image if available
    val latitude: Double,
    val longitude: Double,
    override val sandeshStithi: SandeshStithi,
    val replyOriginalMessage: JsonObject? = null,
    val isLive: Boolean = false,
    val liveUntil: Long? = 0,
    val isStopped: Boolean
) : ParsedSandeshItem

data class ParsedDisappearingSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    override val time: Long,
    override val formattedDate: String,
    val formattedTime: String,
    val title: String,
    override val sandeshStithi: SandeshStithi, // adjust type if different in your other classes
    val duration: Int,                 // raw seconds, kept in case UI needs it later
    val formattedDuration: String,     // e.g. "10 seconds", "1 day"
    val message: String,               // full sentence for UI: "X requested disappearing messages (10 seconds)"
    val requestTimeStamp: Long,
    val accepted: Boolean?
) : ParsedSandeshItem

sealed interface GroupParsedSandeshItem {
    val sandeshId: String
    val displayNameOrNumber: String
    val time: Long
    val formattedDate: String
    val sandeshStithi: SandeshStithi
    val isSelf: Boolean
}


data class GroupParsedDateHeaderSandesh(
    override val sandeshId: String,            // "date_header_<epochDay>"
    override val time: Long,                   // timestamp of the day's oldest message
    val epochDay: Long,
    override val formattedDate: String,        // "Today", "Yesterday", "Monday"
    val detailedDate: String,                  // "Sunday, 4 October 2026 · Today"
    override val displayNameOrNumber: String = "",   // required by the interface, unused
    override val sandeshStithi: SandeshStithi,       // use the same neutral value as before
    override val isSelf: Boolean = false
) : GroupParsedSandeshItem
data class GroupParsedUniversalSandesh(
    override val sandeshId: String,
    override val isSelf: Boolean,
    override val displayNameOrNumber: String,
    override val time: Long,                  // 👈 Raw timestamp (keep it for sorting/comparison)
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    override val sandeshStithi: SandeshStithi,
    val paath: String,
) : GroupParsedSandeshItem

data class GroupParsedTextSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val msgType: Int,
    override val time: Long,                  // 👈 Raw timestamp (keep it for sorting/comparison)
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val paath: String,
    val replyOriginalMessage: JsonObject? = null,
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderProfile: Uri?,
    val linkTitle: String? = null,
    val linkUri: String? = null,
) : GroupParsedSandeshItem

data class GroupParsedListSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,               // ✅ Added override
    override val isSelf: Boolean,                 // ✅ Added override
    override val time: Long,                      // ✅ Added override
    override val formattedDate: String,           // ✅ Added override
    val formattedTime: String,
    override val sandeshStithi: SandeshStithi,     // ✅ Added override
    val listMessage: ListMessage,
    val displayList: ListData,
    val title: String,
    val completed: Int,
    val total: Int,
    val progress: Float,
    val previewItems: List<ListItem>,
    val remainingItems: Int,
    val isUpdate: Boolean,
    // --- Group Specific Fields ---
    override val displayNameOrNumber: String,     // ✅ Added override
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderProfile: Uri?
) : GroupParsedSandeshItem

// 🚀 Hooked up to inheritance chain
data class GroupParsedImageSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,
    override val formattedDate: String,
    val imageUris: List<String>,        // ✅ NEW (main data)
    val displayUri: String?,       // ✅ Only one field now
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val replyOriginalMessage: JsonObject? = null,
    val blurUri: String?,
    val paath: String = "",
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderUserProfile: Uri?,
) : GroupParsedSandeshItem

data class GroupParsedVideoSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val videoUris: List<String> = emptyList(),  // ✅ full list
    val previewUris: List<String> = emptyList(),  // ✅ full list
    val blurUri: String?,
    val paath: String,
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val replyOriginalMessage: JsonObject? = null,
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderUserProfile: Uri?,
) : GroupParsedSandeshItem

data class GroupParsedAudioSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val savedFileUri: String?,
    val duplicateUri: String?,
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val replyOriginalMessage: JsonObject? = null,
    val duration: Long,  // Optional: audio duration in ms
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderUserProfile: Uri?,
) : GroupParsedSandeshItem
data class GroupParsedDocumentSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val savedFileUri: String?,
    val duplicateUri: String?,
    val mimeType: String,
    val downloadPercent: Int,
    val downloadSize: String,
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val replyOriginalMessage: JsonObject? = null,
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderUserProfile: Uri?,
) : GroupParsedSandeshItem
data class GroupParsedContactSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val contacts: List<ContactInfo>,  // List of contacts
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val replyOriginalMessage: JsonObject? = null,
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderUserProfile: Uri?,
) : GroupParsedSandeshItem


data class GroupParsedLocationSandesh(
    val isFirstInGroup: Boolean,
    override val sandeshId: String,
    override val isSelf: Boolean,                 // ✅ override here
    val sandeshType: SandeshType,
    override val time: Long,
    val formattedTime: String,                // 🕒 e.g. "2:34 PM"
    override val formattedDate: String,                // 📆 e.g. "10 June 2025"
    val savedFileUri: String?,       // Screenshot if available
    val duplicateUri: String?,       // Preview image if available
    val latitude: Double,
    val longitude: Double,
    override val sandeshStithi: SandeshStithi,    // ✅ and here
    val replyOriginalMessage: JsonObject? = null,
    override val displayNameOrNumber: String,
    val senderUserDarkColor: Int,
    val senderUserLightColor: Int,
    val senderUserProfile: Uri?,
    val isLive: Boolean = false,
    val liveUntil: Long? = 0,
    val isStopped: Boolean
) : GroupParsedSandeshItem

data class GroupUserProfile(
    val userNumber: String,
    val userName: String,
    val profileUri: Uri?,
    val userDarkColor: Int,
    val userLightColor: Int,

    // 👇 Add only useful fields
    val isSwami: Boolean,
    val canMessage: Boolean,
    val canUpdateProfile: Boolean,
    val canModifyMembers: Boolean,
    val canStartCall: Boolean,
    val joinedAt: Long,
)

data class LiveLocationItem(
    val sandeshId: String,
    val latitude: Double,
    val longitude: Double,
    val isLive: Boolean,
    val displayName: String,
    val profileUri: Uri? = null
)