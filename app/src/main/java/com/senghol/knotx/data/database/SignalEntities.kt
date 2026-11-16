package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identity_keys")
data class IdentityEntity(
    @PrimaryKey val id: Int = 1,
    val identityKeyPair: ByteArray,   // ← serialized IdentityKeyPair
    val registrationId: Int           // ← your local registration ID
) {
    override fun equals(other: Any?): Boolean = other is IdentityEntity &&
            id == other.id &&
            registrationId == other.registrationId &&
            identityKeyPair.contentEquals(other.identityKeyPair)

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + registrationId
        result = 31 * result + identityKeyPair.contentHashCode()
        return result
    }
}

@Entity(tableName = "prekeys")
data class PreKeyEntity(
    @PrimaryKey val preKeyId: Int,
    val record: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PreKeyEntity) return false
        return preKeyId == other.preKeyId &&
                record.contentEquals(other.record)
    }

    override fun hashCode(): Int {
        var result = preKeyId
        result = 31 * result + record.contentHashCode()
        return result
    }
}

@Entity(tableName = "signed_prekeys")
data class SignedPreKeyEntity(
    @PrimaryKey val signedPreKeyId: Int,
    val record: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedPreKeyEntity) return false
        return signedPreKeyId == other.signedPreKeyId &&
                record.contentEquals(other.record)
    }

    override fun hashCode(): Int {
        var result = signedPreKeyId
        result = 31 * result + record.contentHashCode()
        return result
    }
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val address: String, // "user@deviceId"
    val record: ByteArray,
    val sessionId: String            // ← Added for the stable fingerprint/version
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionEntity) return false
        return address == other.address &&
                record.contentEquals(other.record) &&
                sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + record.contentHashCode()
        result = 31 * result + sessionId.hashCode()
        return result
    }
}


@Entity(tableName = "trusted_identities")
data class TrustedIdentityEntity(
    @PrimaryKey val address: String,
    val identityKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrustedIdentityEntity) return false
        return address == other.address &&
                identityKey.contentEquals(other.identityKey)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + identityKey.contentHashCode()
        return result
    }
}

@Entity(tableName = "kyber_prekeys")
data class  KyberPreKeyEntity(
    @PrimaryKey val id: Int,
    val record: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KyberPreKeyEntity

        if (id != other.id) return false
        if (!record.contentEquals(other.record)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + record.contentHashCode()
        return result
    }
}

/**
 * Tracks the last used PreKey and KyberPreKey IDs locally.
 * Ensures consistent incremental key generation across app restarts.
 */
@Entity(tableName = "key_state")
data class KeyStateEntity(
    @PrimaryKey val id: Int = 1,             // Always single row (id=1)
    val lastPreKeyId: Int = 1000,            // Default starting prekey ID
    val lastKyberId: Int = 0,                // Default starting kyber key ID
    val updatedAt: Long = System.currentTimeMillis()
)
