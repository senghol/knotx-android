package com.senghol.knotx.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


object EncryptionHelper {

    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private val SALT = "dootawas_backup_salt_v1".toByteArray()

    private fun deriveKey(accountEmail: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(
            accountEmail.toCharArray(),
            SALT,
            ITERATIONS,
            KEY_LENGTH
        )
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(inputFile: File, outputFile: File, accountEmail: String) {
        val key = deriveKey(accountEmail)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        outputFile.outputStream().buffered().use { out ->
            out.write(iv) // write 16 bytes IV
            CipherOutputStream(out, cipher).use { cos ->
                inputFile.inputStream().buffered().use { it.copyTo(cos, bufferSize = 8192) }
            }
        }
    }

    fun decrypt(inputFile: File, outputFile: File, accountEmail: String) {
        val key = deriveKey(accountEmail)

        inputFile.inputStream().buffered().use { input ->
            val iv = ByteArray(IV_SIZE)
            input.read(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            outputFile.outputStream().buffered().use { out ->
                CipherInputStream(input, cipher).use { it.copyTo(out, bufferSize = 8192) }
            }
        }
    }
}