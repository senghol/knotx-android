package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.senghol.knotx.utils.data.EncryptedKeyBlob
import com.senghol.knotx.utils.enums.SandeshStithi

@Entity(
    tableName = "group_entity",
    indices = [Index(value = ["groupName"], unique = true)]
)
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val groupDescription: String?,
    val groupImageUrl: String?,
    val lastMessage: String? = null,
    val totalUnread: Int = 0,
    val createdBy: String,
    val createdAt: Long,
    val imageTime: Long = 0,
    val isMuted: Boolean,
    val isPinned: Boolean? = false,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val wallpaperImage: String? = null,
    val sandeshStithi: SandeshStithi,
    val lastMessageId: String,
    val isGroupSuspended: Boolean = false, //todo delete this
    val backgroundColor: Int,
    val draftMessage: String? = null,
    val isGroupActive: Boolean? = true,
    val revealSender: Boolean = false,
    val autoDisappearMsg: Int = 0,
// ===== Group E2EE State =====
    val currentEpoch: Long = 0L,
    val currentCommitHash: ByteArray? = null,
    val rosterVersion: Long = 0L,
    val groupCryptoProtocolVersion: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupEntity

        if (totalUnread != other.totalUnread) return false
        if (createdAt != other.createdAt) return false
        if (imageTime != other.imageTime) return false
        if (isMuted != other.isMuted) return false
        if (isPinned != other.isPinned) return false
        if (lastMessageTime != other.lastMessageTime) return false
        if (isGroupSuspended != other.isGroupSuspended) return false
        if (backgroundColor != other.backgroundColor) return false
        if (isGroupActive != other.isGroupActive) return false
        if (revealSender != other.revealSender) return false
        if (autoDisappearMsg != other.autoDisappearMsg) return false
        if (currentEpoch != other.currentEpoch) return false
        if (rosterVersion != other.rosterVersion) return false
        if (groupCryptoProtocolVersion != other.groupCryptoProtocolVersion) return false
        if (groupId != other.groupId) return false
        if (groupName != other.groupName) return false
        if (groupDescription != other.groupDescription) return false
        if (groupImageUrl != other.groupImageUrl) return false
        if (lastMessage != other.lastMessage) return false
        if (createdBy != other.createdBy) return false
        if (wallpaperImage != other.wallpaperImage) return false
        if (sandeshStithi != other.sandeshStithi) return false
        if (lastMessageId != other.lastMessageId) return false
        if (draftMessage != other.draftMessage) return false

        when {
            currentCommitHash === other.currentCommitHash -> Unit
            currentCommitHash == null || other.currentCommitHash == null -> return false
            !currentCommitHash.contentEquals(other.currentCommitHash) -> return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = totalUnread
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + imageTime.hashCode()
        result = 31 * result + isMuted.hashCode()
        result = 31 * result + (isPinned?.hashCode() ?: 0)
        result = 31 * result + lastMessageTime.hashCode()
        result = 31 * result + isGroupSuspended.hashCode()
        result = 31 * result + backgroundColor
        result = 31 * result + (isGroupActive?.hashCode() ?: 0)
        result = 31 * result + revealSender.hashCode()
        result = 31 * result + autoDisappearMsg
        result = 31 * result + currentEpoch.hashCode()
        result = 31 * result + rosterVersion.hashCode()
        result = 31 * result + groupCryptoProtocolVersion
        result = 31 * result + groupId.hashCode()
        result = 31 * result + groupName.hashCode()
        result = 31 * result + (groupDescription?.hashCode() ?: 0)
        result = 31 * result + (groupImageUrl?.hashCode() ?: 0)
        result = 31 * result + (lastMessage?.hashCode() ?: 0)
        result = 31 * result + createdBy.hashCode()
        result = 31 * result + (wallpaperImage?.hashCode() ?: 0)
        result = 31 * result + sandeshStithi.hashCode()
        result = 31 * result + lastMessageId.hashCode()
        result = 31 * result + (draftMessage?.hashCode() ?: 0)
        result = 31 * result + (currentCommitHash?.contentHashCode() ?: 0)
        return result
    }
}
