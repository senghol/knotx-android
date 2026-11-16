package com.senghol.knotx.utils
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object HybridCrypto {

    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128

    data class EncryptedMessage(
        val encryptedMessage: String,   // Base64(AES-GCM(IV + ciphertext))
        val encryptedAesKey: String     // Base64(RSA-encrypted AES key)
    )

    fun generateRsaKeyPairIfNotExists(alias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) return

        val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(2048)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .build()

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun getPublicKeyBase64(alias: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val publicKey = keyStore.getCertificate(alias).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun encryptString(message: String, receiverPublicKeyBase64: String): EncryptedMessage {
        val aesKey = KeyGenerator.getInstance("AES").apply { init(AES_KEY_SIZE) }.generateKey()
        val iv = ByteArray(GCM_IV_SIZE).also { SecureRandom().nextBytes(it) }

        // Encrypt message with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val ciphertext = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        val ivAndCipher = iv + ciphertext
        val encryptedMessageBase64 = Base64.encodeToString(ivAndCipher, Base64.NO_WRAP)

        // Encrypt AES key with RSA public key
        val publicKeyBytes = Base64.decode(receiverPublicKeyBase64, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)
        val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)

        return EncryptedMessage(encryptedMessageBase64, encryptedAesKeyBase64)
    }

    fun decryptString(payload: EncryptedMessage, alias: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = keyStore.getKey(alias, null) as PrivateKey

        // Decrypt AES key
        val encryptedAesKeyBytes = Base64.decode(payload.encryptedAesKey, Base64.NO_WRAP)
        val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // Decrypt message
        val ivAndCipher = Base64.decode(payload.encryptedMessage, Base64.NO_WRAP)
        val iv = ivAndCipher.sliceArray(0 until GCM_IV_SIZE)
        val cipherText = ivAndCipher.sliceArray(GCM_IV_SIZE until ivAndCipher.size)
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val decryptedBytes = aesCipher.doFinal(cipherText)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}
