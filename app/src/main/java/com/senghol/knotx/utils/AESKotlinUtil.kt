package com.senghol.knotx.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESKotlinUtil {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    fun encryptForSMS(phoneNumber: String, sessionKey: String, base64Key: String): String {
        val secretKey = SecretKeySpec(Base64.decode(base64Key, Base64.DEFAULT), "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        
        val encryptedBytes = cipher.doFinal(phoneNumber.toByteArray())

        val encryptedText = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
        return "$sessionKey::$ivString::$encryptedText"  // Include session key in SMS
    }
}
