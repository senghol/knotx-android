package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.senghol.knotx.utils.enums.SandeshStithi

/**
 * Represents a chat message entity stored in the local Room database.
 *
 * This entity is designed to store messages efficiently for quick retrieval and sorting.
 *
 * @property id A unique identifier for each message. Acts as the primary key.
 * @property username The username of the sender or receiver of the message.
 * @property samagri The actual message content stored as a JSON string (supports text, images, etc.).
 * @property isSelf Indicates whether the message was sent by the current user (`true`) or received (`false`).
 * @property likhit The timestamp of when the message was created, stored as a `Long` for efficient sorting.
 */

/**
 * Defines constants used for storing and retrieving message data within `samagri` (JSON format).
 *
 * Messages stored in the `SandeshA` entity use `samagri` to store structured message content
 * in JSON format. Each message contains a `sandeshType` to define its type (e.g., text, image, video),
 * along with relevant fields based on its type.
 *
 * Example JSON Structure:
 * ```json
 *
 * {
 *   "link":"false",
 *   "paath": "Hello, how are you?",
 *   "sandeshType": 1
 * }
 * ```
 *
 * {
 *   "originalUri":"content://com.android.providers.media.documents/document/image%3A3537",
 *   "mimeType":"image/jpeg",
 *   "sandeshType":1}
 * {
 *   "originalUri":"content://com.android.providers.media.documents/document/video%3A2790",
 *   "duplicateUri":"/data/user/0/com.senghol.knotx/files/preview/preview_1771946145802.webp",
 *   "mimeType":"video/mp4",
 *   "sandeshType":2
 *   }
 *
 *   {
 *      "originalUri":"file:///storage/emulated/0/Android/data/com.senghol.knotx/files/recordings/recording_1772015698301.m4a",
 *      "mimeType":".m4a",
 *      "duration":7569,
 *      "downloadPercent":0,
 *      "sandeshType":4
 *      }
 *
 *      {"originalUri":"content://com.android.providers.media.documents/document/document%3A1505",
 *      "mimeType":"application/pdf",
 *      "duplicateUri":"file:///storage/emulated/0/Android/data/com.senghol.knotx/files/document_previews/preview_1772016387170.png",
 *      "fileName":"Weeding Invitations 💞.pdf",
 *      "sandeshType":5}
 * ```
 */

@Entity(tableName = "sandesh")
data class Sandesh(
    @PrimaryKey val sandeshId: String,
    val userNumber: String,
    val samagri: String,
    val isSelf: Boolean,
    val time: Long,
    val sandeshType: Int,
    val sandeshStithi: SandeshStithi
)
