package com.senghol.knotx.utils

import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import com.google.gson.Gson
import javax.crypto.spec.SecretKeySpec

object HybridEncryptor {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128

    private val gson = Gson()

    data class EncryptedPayload(
        val encryptedMessage: String,
        val encryptedAesKey: String
    )

    fun encrypt(messageJson: String, receiverPublicKeyBase64: String): EncryptedPayload {
        // Step 1: Generate AES Key
        val aesKey = KeyGenerator.getInstance("AES").apply { init(AES_KEY_SIZE) }.generateKey()

        // Step 2: Encrypt Message using AES
        val iv = ByteArray(GCM_IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val encryptedMessageBytes = cipher.doFinal(messageJson.toByteArray(Charsets.UTF_8))
        val finalEncryptedMessage = iv + encryptedMessageBytes
        val encryptedMessageBase64 = Base64.encodeToString(finalEncryptedMessage, Base64.NO_WRAP)

        // Step 3: Encrypt AES Key using receiver’s RSA public key
        val publicKeyBytes = Base64.decode(receiverPublicKeyBase64, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.encoded)
        val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKeyBytes, Base64.NO_WRAP)

        return EncryptedPayload(encryptedMessageBase64, encryptedAesKeyBase64)
    }

    fun decrypt(payload: EncryptedPayload, keystoreAlias: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = keyStore.getKey(keystoreAlias, null) as PrivateKey

        // Decrypt AES Key using Keystore
        val encryptedAesKeyBytes = Base64.decode(payload.encryptedAesKey, Base64.NO_WRAP)
        val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // Decrypt Message using AES Key
        val fullEncrypted = Base64.decode(payload.encryptedMessage, Base64.NO_WRAP)
        val iv = fullEncrypted.sliceArray(0 until GCM_IV_SIZE)
        val cipherText = fullEncrypted.sliceArray(GCM_IV_SIZE until fullEncrypted.size)
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        val decryptedBytes = aesCipher.doFinal(cipherText)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun <T> encryptJson(data: T, receiverPublicKeyBase64: String): EncryptedPayload {
        val json = gson.toJson(data)
        return encrypt(json, receiverPublicKeyBase64)
    }

    fun <T> decryptJson(payload: EncryptedPayload, keystoreAlias: String, clazz: Class<T>): T {
        val json = decrypt(payload, keystoreAlias)
        return gson.fromJson(json, clazz)
    }
}
