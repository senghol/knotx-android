package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Stores group member UI metadata, permissions and cryptographic identity data.
 *
 * `leafIndex` is stable for the lifetime of this membership incarnation and
 * must never be renumbered or reassigned to another member.
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "userNumber"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class GroupMemberEntity(
    val groupId: String,
    val userNumber: String,
    val contactName: String,
    val onlineName: String? = null,
    val userDarkColor: Int,
    val userLightColor: Int,
    val joinedAt: Long,
    val isSwami: Boolean,
    val canMessage: Boolean,
    val canUpdateProfile: Boolean,
    val canModifyMembers: Boolean,
    val canStartCall: Boolean,
    val profileTime: Long = 0L,
    val profileUrl: String,
    val publicKey: String,

    // ===== Group E2EE Identity =====

    // Stable identifier for this specific membership incarnation.
    // If a user leaves and later rejoins, they receive a new value.
    val memberInstanceId: String = "",

    // Identifier of the device owning these hybrid public keys.
    val deviceId: String = "",

    // Stable TreeKEM leaf position for this membership incarnation.
    // Must never be renumbered or reassigned.
    val leafIndex: Int = -1,

    // Incremented whenever this device's hybrid public-key package changes.
    val keyGeneration: Long = 0L,

    // X.509 encoded X25519 public key.
    val x25519PublicKey: ByteArray = ByteArray(0),

    // X.509 encoded ML-KEM-768 public key.
    val mlKemPublicKey: ByteArray = ByteArray(0),
    val ed25519PublicKey: ByteArray = ByteArray(0),

    // Whether this member/device is eligible to receive future epoch keys.
    val isCryptoActive: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupMemberEntity

        return groupId == other.groupId &&
                userNumber == other.userNumber &&
                contactName == other.contactName &&
                onlineName == other.onlineName &&
                userDarkColor == other.userDarkColor &&
                userLightColor == other.userLightColor &&
                joinedAt == other.joinedAt &&
                isSwami == other.isSwami &&
                canMessage == other.canMessage &&
                canUpdateProfile == other.canUpdateProfile &&
                canModifyMembers == other.canModifyMembers &&
                canStartCall == other.canStartCall &&
                profileTime == other.profileTime &&
                profileUrl == other.profileUrl &&
                publicKey == other.publicKey &&
                memberInstanceId == other.memberInstanceId &&
                deviceId == other.deviceId &&
                leafIndex == other.leafIndex &&
                keyGeneration == other.keyGeneration &&
                x25519PublicKey.contentEquals(other.x25519PublicKey) &&
                mlKemPublicKey.contentEquals(other.mlKemPublicKey) &&
                ed25519PublicKey.contentEquals(other.ed25519PublicKey) &&
                isCryptoActive == other.isCryptoActive
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + userNumber.hashCode()
        result = 31 * result + contactName.hashCode()
        result = 31 * result + (onlineName?.hashCode() ?: 0)
        result = 31 * result + userDarkColor
        result = 31 * result + userLightColor
        result = 31 * result + joinedAt.hashCode()
        result = 31 * result + isSwami.hashCode()
        result = 31 * result + canMessage.hashCode()
        result = 31 * result + canUpdateProfile.hashCode()
        result = 31 * result + canModifyMembers.hashCode()
        result = 31 * result + canStartCall.hashCode()
        result = 31 * result + profileTime.hashCode()
        result = 31 * result + profileUrl.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + memberInstanceId.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + leafIndex
        result = 31 * result + keyGeneration.hashCode()
        result = 31 * result + x25519PublicKey.contentHashCode()
        result = 31 * result + mlKemPublicKey.contentHashCode()
        result = 31 * result + ed25519PublicKey.contentHashCode()
        result = 31 * result + isCryptoActive.hashCode()
        return result
    }
}