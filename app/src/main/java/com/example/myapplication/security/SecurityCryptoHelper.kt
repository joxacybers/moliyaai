package com.example.myapplication.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

/**
 * Provides hardware-backed Android Keystore AES-256-GCM encryption for API keys/secrets
 * and salted PBKDF2-HMAC-SHA256 hashing for the user's PIN code.
 */
object SecurityCryptoHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "moliya_ai_master_aes_key"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 12_000
    private const val PBKDF2_KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val PIN_HASH_PREFIX = "pbkdf2$"
    private const val ENC_PREFIX = "enc1$"

    /**
     * Returns true if [storedPin] is already a salted PBKDF2 hash.
     */
    fun isHashedPin(storedPin: String): Boolean {
        return storedPin.startsWith(PIN_HASH_PREFIX)
    }

    /**
     * Hashes a 4-digit PIN using PBKDF2WithHmacSHA256 with a cryptographically random 16-byte salt.
     */
    fun hashPin(rawPin: String): String {
        if (rawPin.isBlank()) return ""
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = derivePbkdf2(rawPin, salt, PBKDF2_ITERATIONS)
        val saltB64 = Base64.getEncoder().withoutPadding().encodeToString(salt)
        val hashB64 = Base64.getEncoder().withoutPadding().encodeToString(derived)
        return "$PIN_HASH_PREFIX$PBKDF2_ITERATIONS$$saltB64$$hashB64"
    }

    /**
     * Verifies [enteredPin] against [storedPin] (supports both salted PBKDF2 hashes and
     * legacy 4-digit plaintext PINs for seamless backward compatibility).
     */
    fun verifyPin(enteredPin: String, storedPin: String): Boolean {
        if (enteredPin.isBlank() || storedPin.isBlank()) return false
        if (!isHashedPin(storedPin)) {
            return MessageDigest.isEqual(
                enteredPin.toByteArray(Charsets.UTF_8),
                storedPin.toByteArray(Charsets.UTF_8),
            )
        }
        val parts = storedPin.removePrefix(PIN_HASH_PREFIX).split('$')
        if (parts.size != 3) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val expectedHash = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val actualHash = derivePbkdf2(enteredPin, salt, iterations)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    private fun derivePbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Encrypts a sensitive secret (such as a custom OpenAI API key) using Android Keystore AES-256-GCM.
     */
    fun encryptSecret(plainText: String): String {
        val trimmed = plainText.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith(ENC_PREFIX)) return trimmed
        return try {
            val secretKey = getOrCreateKeystoreKey() ?: return trimmed
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.getEncoder().withoutPadding().encodeToString(iv)
            val cipherB64 = Base64.getEncoder().withoutPadding().encodeToString(cipherBytes)
            "$ENC_PREFIX$ivB64$$cipherB64"
        } catch (_: Exception) {
            trimmed
        }
    }

    /**
     * Decrypts a secret previously encrypted by [encryptSecret].
     * Transparently returns legacy unencrypted values if not prefixed with [ENC_PREFIX].
     */
    fun decryptSecret(storedValue: String): String {
        if (storedValue.isBlank()) return ""
        if (!storedValue.startsWith(ENC_PREFIX)) return storedValue
        val parts = storedValue.removePrefix(ENC_PREFIX).split('$')
        if (parts.size != 2) return ""
        return try {
            val secretKey = getOrCreateKeystoreKey() ?: return ""
            val iv = Base64.getDecoder().decode(parts[0])
            val cipherBytes = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                return existing.secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            null
        }
    }
}
