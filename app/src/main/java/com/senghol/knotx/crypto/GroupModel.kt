package com.senghol.knotx.crypto

import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey

data class GroupIdentityKeySet(
    val x25519KeyPair: KeyPair,
    val mlKemKeyPair: KeyPair,
    val ed25519KeyPair: KeyPair
)

data class HybridEncapsulatedPayload(
    val recipientLeafId: String,
    val ephemeralX25519Public: ByteArray,
    val mlKemCiphertext: ByteArray,
    val encryptedPathSeed: ByteArray,
    val nonce: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HybridEncapsulatedPayload

        return recipientLeafId == other.recipientLeafId &&
                ephemeralX25519Public.contentEquals(other.ephemeralX25519Public) &&
                mlKemCiphertext.contentEquals(other.mlKemCiphertext) &&
                encryptedPathSeed.contentEquals(other.encryptedPathSeed) &&
                nonce.contentEquals(other.nonce)
    }

    override fun hashCode(): Int {
        var result = recipientLeafId.hashCode()
        result = 31 * result + ephemeralX25519Public.contentHashCode()
        result = 31 * result + mlKemCiphertext.contentHashCode()
        result = 31 * result + encryptedPathSeed.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        return result
    }
}

data class EpochPathHash(
    val recipientLeafId: String,
    val pathHash: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpochPathHash) return false

        return recipientLeafId == other.recipientLeafId &&
                pathHash.contentEquals(other.pathHash)
    }

    override fun hashCode(): Int {
        var result = recipientLeafId.hashCode()
        result = 31 * result + pathHash.contentHashCode()
        return result
    }
}

data class AesGcmPayload(
    val ciphertext: ByteArray,
    val nonce: ByteArray
)

data class EpochCommit(
    val protocolVersion: Int,
    val groupId: String,
    val parentEpoch: Long,
    val newEpoch: Long,
    val rosterVersion: Long,
    val commitId: String,
    val parentCommitHash: ByteArray?,
    val committerMemberId: String,
    val committerDeviceId: String,
    // All recipients can receive this list.
    // This is what is included in the signed commit hash.
    val pathHashes: List<EpochPathHash>,
    val encryptedPaths: List<HybridEncapsulatedPayload>,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpochCommit

        return protocolVersion == other.protocolVersion &&
                groupId == other.groupId &&
                parentEpoch == other.parentEpoch &&
                newEpoch == other.newEpoch &&
                rosterVersion == other.rosterVersion &&
                commitId == other.commitId &&
                when {
                    parentCommitHash === other.parentCommitHash -> true
                    parentCommitHash == null || other.parentCommitHash == null -> false
                    else -> parentCommitHash.contentEquals(other.parentCommitHash)
                } &&
                committerMemberId == other.committerMemberId &&
                committerDeviceId == other.committerDeviceId &&
                encryptedPaths == other.encryptedPaths &&
                signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = protocolVersion
        result = 31 * result + groupId.hashCode()
        result = 31 * result + parentEpoch.hashCode()
        result = 31 * result + newEpoch.hashCode()
        result = 31 * result + rosterVersion.hashCode()
        result = 31 * result + commitId.hashCode()
        result = 31 * result + (parentCommitHash?.contentHashCode() ?: 0)
        result = 31 * result + committerMemberId.hashCode()
        result = 31 * result + committerDeviceId.hashCode()
        result = 31 * result + encryptedPaths.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

data class GroupMessage(
    val protocolVersion: Int,
    val groupId: String,
    val epoch: Long,
    val senderId: String,
    val senderDeviceId: String,
    val messageId: String,
    val messageNumber: Long,
    val nonce: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupMessage

        return protocolVersion == other.protocolVersion &&
                groupId == other.groupId &&
                epoch == other.epoch &&
                senderId == other.senderId &&
                senderDeviceId == other.senderDeviceId &&
                messageId == other.messageId &&
                messageNumber == other.messageNumber &&
                nonce.contentEquals(other.nonce) &&
                ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = protocolVersion
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch.hashCode()
        result = 31 * result + senderId.hashCode()
        result = 31 * result + senderDeviceId.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + messageNumber.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

/**
 * Encrypted payload for secrets stored locally at rest.
 * `nonce` is the AES-GCM nonce generated for this encryption operation.
 * `keyVersion` allows future Android KeyStore wrapping-key rotation.
 */
data class EncryptedKeyPayload(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val keyVersion: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedKeyPayload

        return keyVersion == other.keyVersion &&
                ciphertext.contentEquals(other.ciphertext) &&
                nonce.contentEquals(other.nonce)
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + keyVersion
        return result
    }
}
data class LocalGroupPrivateKeys(
    val x25519PrivateKey: PrivateKey,
    val mlKemPrivateKey: PrivateKey,
    val ed25519PrivateKey: PrivateKey
)

data class LocalGroupKeyPairs(
    val x25519KeyPair: KeyPair,
    val mlKemKeyPair: KeyPair,
    val ed25519KeyPair: KeyPair
)

data class LocalGroupKeys(
    val x25519PrivateKey: PrivateKey,
    val x25519PublicKey: PublicKey,
    val mlKemPrivateKey: PrivateKey,
    val mlKemPublicKey: PublicKey,
    val ed25519PrivateKey: PrivateKey,
    val ed25519PublicKey: PublicKey
)
data class GroupMemberKeyPackage(
    val groupId: String,
    val userNumber: String,
    val memberInstanceId: String,
    val deviceId: String,
    val keyGeneration: Long,
    val x25519PublicKey: ByteArray,
    val mlKemPublicKey: ByteArray,
    val ed25519PublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupMemberKeyPackage

        return groupId == other.groupId &&
                userNumber == other.userNumber &&
                memberInstanceId == other.memberInstanceId &&
                deviceId == other.deviceId &&
                keyGeneration == other.keyGeneration &&
                x25519PublicKey.contentEquals(other.x25519PublicKey) &&
                mlKemPublicKey.contentEquals(other.mlKemPublicKey) &&
                ed25519PublicKey.contentEquals(other.ed25519PublicKey)
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + userNumber.hashCode()
        result = 31 * result + memberInstanceId.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + keyGeneration.hashCode()
        result = 31 * result + x25519PublicKey.contentHashCode()
        result = 31 * result + mlKemPublicKey.contentHashCode()
        result = 31 * result + ed25519PublicKey.contentHashCode()
        return result
    }
}