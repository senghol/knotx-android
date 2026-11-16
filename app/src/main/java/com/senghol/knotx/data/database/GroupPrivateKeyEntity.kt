package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_private_keys",
    primaryKeys = ["groupId", "memberInstanceId"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["memberInstanceId"])
    ]
)
data class GroupPrivateKeyEntity(
    val groupId: String,
    val memberInstanceId: String,

    // X25519 private key, encrypted with Android KeyStore master key.
    val encryptedX25519PrivateKey: ByteArray,
    val x25519Nonce: ByteArray,
    val x25519KeyVersion: Int,

    // ML-KEM private key, encrypted with Android KeyStore master key.
    val encryptedMlKemPrivateKey: ByteArray,
    val mlKemNonce: ByteArray,
    val mlKemKeyVersion: Int,

    // Ed25519 private key, encrypted with Android KeyStore master key.
    val encryptedEd25519PrivateKey: ByteArray,
    val ed25519Nonce: ByteArray,
    val ed25519KeyVersion: Int,

    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupPrivateKeyEntity

        return groupId == other.groupId &&
                memberInstanceId == other.memberInstanceId &&
                encryptedX25519PrivateKey.contentEquals(other.encryptedX25519PrivateKey) &&
                x25519Nonce.contentEquals(other.x25519Nonce) &&
                x25519KeyVersion == other.x25519KeyVersion &&
                encryptedMlKemPrivateKey.contentEquals(other.encryptedMlKemPrivateKey) &&
                mlKemNonce.contentEquals(other.mlKemNonce) &&
                mlKemKeyVersion == other.mlKemKeyVersion &&
                encryptedEd25519PrivateKey.contentEquals(other.encryptedEd25519PrivateKey) &&
                ed25519Nonce.contentEquals(other.ed25519Nonce) &&
                ed25519KeyVersion == other.ed25519KeyVersion &&
                createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + memberInstanceId.hashCode()
        result = 31 * result + encryptedX25519PrivateKey.contentHashCode()
        result = 31 * result + x25519Nonce.contentHashCode()
        result = 31 * result + x25519KeyVersion
        result = 31 * result + encryptedMlKemPrivateKey.contentHashCode()
        result = 31 * result + mlKemNonce.contentHashCode()
        result = 31 * result + mlKemKeyVersion
        result = 31 * result + encryptedEd25519PrivateKey.contentHashCode()
        result = 31 * result + ed25519Nonce.contentHashCode()
        result = 31 * result + ed25519KeyVersion
        result = 31 * result + createdAt.hashCode()
        return result
    }
}