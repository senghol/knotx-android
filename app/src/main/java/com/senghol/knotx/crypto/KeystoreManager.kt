package com.senghol.knotx.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyStoreManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "dootawas_group_crypto_master_key"

        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_NONCE_LENGTH_BYTES = 12

        private const val CURRENT_KEY_VERSION = 1
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val parameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }

        return (keyStore.getEntry(
            MASTER_KEY_ALIAS,
            null
        ) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Generic AES-256-GCM wrapping for local cryptographic material.
     *
     * [aad] cryptographically binds the encrypted data to its intended purpose.
     */
    fun wrap(
        plaintext: ByteArray,
        aad: ByteArray
    ): EncryptedKeyPayload {
        require(plaintext.isNotEmpty()) {
            "Cannot wrap empty plaintext."
        }

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        cipher.updateAAD(aad)

        val ciphertext = cipher.doFinal(plaintext)
        val nonce = cipher.iv

        require(nonce.size == GCM_NONCE_LENGTH_BYTES) {
            "Unexpected AES-GCM nonce length: ${nonce.size}"
        }

        return EncryptedKeyPayload(
            ciphertext = ciphertext,
            nonce = nonce,
            keyVersion = CURRENT_KEY_VERSION
        )
    }

    /**
     * Generic AES-256-GCM unwrapping for local cryptographic material.
     */
    fun unwrap(
        ciphertext: ByteArray,
        nonce: ByteArray,
        keyVersion: Int,
        aad: ByteArray
    ): ByteArray {
        require(keyVersion == CURRENT_KEY_VERSION) {
            "Unsupported local key version: $keyVersion"
        }

        require(nonce.size == GCM_NONCE_LENGTH_BYTES) {
            "Invalid AES-GCM nonce length: ${nonce.size}"
        }

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateMasterKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
        )

        cipher.updateAAD(aad)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Wraps a private key for local storage.
     *
     * The key purpose is included in AAD so an encrypted X25519 private key
     * cannot be substituted for an ML-KEM private key.
     */
    fun wrapPrivateKey(
        rawKeyBytes: ByteArray,
        keyPurpose: String
    ): EncryptedKeyPayload {
        require(keyPurpose.isNotBlank()) {
            "Key purpose cannot be blank."
        }

        return wrap(
            plaintext = rawKeyBytes,
            aad = buildPrivateKeyAad(keyPurpose)
        )
    }

    /**
     * Unwraps a locally stored private key.
     */
    fun unwrapPrivateKey(
        encryptedBytes: ByteArray,
        nonce: ByteArray,
        keyVersion: Int,
        keyPurpose: String
    ): ByteArray {
        require(keyPurpose.isNotBlank()) {
            "Key purpose cannot be blank."
        }

        return unwrap(
            ciphertext = encryptedBytes,
            nonce = nonce,
            keyVersion = keyVersion,
            aad = buildPrivateKeyAad(keyPurpose)
        )
    }

    /**
     * Wraps an epoch secret before storing it in Room.
     *
     * The secret is bound to:
     * - Group ID
     * - Epoch number
     * - Accepted commit hash
     */
    fun wrapEpochSecret(
        groupId: String,
        epoch: Long,
        commitHash: ByteArray,
        epochSecret: ByteArray
    ): EncryptedKeyPayload {
        require(groupId.isNotBlank()) {
            "Group ID cannot be blank."
        }

        require(epoch >= 0L) {
            "Epoch cannot be negative."
        }

        require(commitHash.isNotEmpty()) {
            "Commit hash cannot be empty."
        }

        require(epochSecret.size == 32) {
            "Epoch secret must be exactly 32 bytes."
        }

        return wrap(
            plaintext = epochSecret,
            aad = buildEpochAad(
                groupId = groupId,
                epoch = epoch,
                commitHash = commitHash
            )
        )
    }

    /**
     * Unwraps an epoch secret from local Room storage.
     */
    fun unwrapEpochSecret(
        groupId: String,
        epoch: Long,
        commitHash: ByteArray,
        encryptedEpochSecret: ByteArray,
        nonce: ByteArray,
        keyVersion: Int
    ): ByteArray {
        require(groupId.isNotBlank()) {
            "Group ID cannot be blank."
        }

        require(epoch >= 0L) {
            "Epoch cannot be negative."
        }

        require(commitHash.isNotEmpty()) {
            "Commit hash cannot be empty."
        }

        val epochSecret = unwrap(
            ciphertext = encryptedEpochSecret,
            nonce = nonce,
            keyVersion = keyVersion,
            aad = buildEpochAad(
                groupId = groupId,
                epoch = epoch,
                commitHash = commitHash
            )
        )

        require(epochSecret.size == 32) {
            "Invalid decrypted epoch secret length: ${epochSecret.size}"
        }

        return epochSecret
    }

    /**
     * Creates AAD for locally encrypted private keys.
     */
    private fun buildPrivateKeyAad(
        keyPurpose: String
    ): ByteArray {
        return "DOOTAWAS_PRIVATE_KEY_V1:$keyPurpose"
            .toByteArray(Charsets.UTF_8)
    }

    /**
     * Creates deterministic binary AAD for a locally stored epoch secret.
     *
     * Binary encoding avoids delimiter/serialization ambiguity.
     */
    private fun buildEpochAad(
        groupId: String,
        epoch: Long,
        commitHash: ByteArray
    ): ByteArray {
        val domain = "DOOTAWAS_GROUP_EPOCH_V1".toByteArray(Charsets.UTF_8)
        val groupIdBytes = groupId.toByteArray(Charsets.UTF_8)

        return ByteBuffer.allocate(
            domain.size +
                    4 +
                    groupIdBytes.size +
                    8 +
                    4 +
                    commitHash.size
        ).apply {
            put(domain)
            putInt(groupIdBytes.size)
            put(groupIdBytes)
            putLong(epoch)
            putInt(commitHash.size)
            put(commitHash)
        }.array()
    }
}