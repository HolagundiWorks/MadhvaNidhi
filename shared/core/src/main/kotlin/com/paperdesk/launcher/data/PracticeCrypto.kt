package com.paperdesk.launcher.data

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase encryption for practice exports. The file is: magic (8 bytes) | salt (16) | nonce (12) | AES-256-GCM
 * ciphertext with the magic as associated data. The key is PBKDF2-HMAC-SHA256 of the passphrase and the salt.
 * A wrong passphrase or a changed byte fails authentication and is reported as one error.
 */
object PracticeCrypto {
    private val MAGIC = "MNPRACT1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val ITERATIONS = 210_000
    const val MIN_PASSPHRASE = 6

    class CryptoException(message: String) : Exception(message)

    fun isSealed(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size + SALT_BYTES + NONCE_BYTES && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun seal(plain: ByteArray, passphrase: CharArray, random: SecureRandom = SecureRandom()): ByteArray {
        if (passphrase.size < MIN_PASSPHRASE) throw CryptoException("The passphrase must be at least $MIN_PASSPHRASE characters.")
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val body = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(MAGIC)
            }.doFinal(plain)
        } finally {
            key.fill(0)
        }
        return MAGIC + salt + nonce + body
    }

    fun open(sealed: ByteArray, passphrase: CharArray): ByteArray {
        if (!isSealed(sealed)) throw CryptoException("This is not an encrypted practice export.")
        val salt = sealed.copyOfRange(MAGIC.size, MAGIC.size + SALT_BYTES)
        val nonce = sealed.copyOfRange(MAGIC.size + SALT_BYTES, MAGIC.size + SALT_BYTES + NONCE_BYTES)
        val body = sealed.copyOfRange(MAGIC.size + SALT_BYTES + NONCE_BYTES, sealed.size)
        val key = deriveKey(passphrase, salt)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(MAGIC)
            }.doFinal(body)
        } catch (e: GeneralSecurityException) {
            throw CryptoException("The passphrase is wrong, or the file has been changed.")
        } finally {
            key.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
