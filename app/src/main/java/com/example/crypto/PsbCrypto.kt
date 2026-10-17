package com.example.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PsbCrypto {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val GCM_IV_LENGTH = 12 // bytes
    private const val PASSWORD_ITERATIONS = 40000
    private const val PIN_ITERATIONS = 20000

    private val secureRandom = SecureRandom()

    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    fun toBase64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(str: String): ByteArray =
        Base64.decode(str, Base64.NO_WRAP)

    fun sha256(bytes: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes)
    }

    fun deriveKekBits(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PASSWORD_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    fun derivePinVerifier(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PIN_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val derived = factory.generateSecret(spec).encoded
        return toBase64(sha256(derived))
    }

    fun encryptAesGcm(key: SecretKey, plaintext: ByteArray): EncryptedPayload {
        val iv = generateRandomBytes(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }

    fun decryptAesGcm(key: SecretKey, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    fun createSecretKey(bytes: ByteArray): SecretKey =
        SecretKeySpec(bytes, "AES")

    data class EncryptedPayload(
        val iv: ByteArray,
        val ciphertext: ByteArray
    )
}
