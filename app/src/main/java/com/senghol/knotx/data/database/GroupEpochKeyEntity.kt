package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Stores locally encrypted historical and active group epoch secrets.
 * Recent epochs are retained to decrypt delayed or out-of-order messages.
 * Older epochs are pruned to limit historical key exposure.
 */
@Entity(
    tableName = "group_epoch_keys",
    primaryKeys = ["groupId", "epoch"],
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
data class GroupEpochKeyEntity(
    val groupId: String,
    val epoch: Long,
    val encryptedEpochSecret: ByteArray,
    val nonce: ByteArray,
    val keyVersion: Int,
    val commitHash: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupEpochKeyEntity

        return groupId == other.groupId &&
                epoch == other.epoch &&
                encryptedEpochSecret.contentEquals(other.encryptedEpochSecret) &&
                nonce.contentEquals(other.nonce) &&
                keyVersion == other.keyVersion &&
                commitHash.contentEquals(other.commitHash) &&
                createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + epoch.hashCode()
        result = 31 * result + encryptedEpochSecret.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + keyVersion
        result = 31 * result + commitHash.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}