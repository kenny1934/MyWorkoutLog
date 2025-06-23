package com.example.myworkoutlog

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption utilities for cloud backup data protection
 * Uses AES-256-GCM for strong encryption with Android Keystore integration
 */
object EncryptionUtils {
    
    private const val KEYSTORE_ALIAS = "myworkoutlog_backup_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    
    /**
     * Encrypts backup data using AES-256-GCM
     * Returns base64-encoded encrypted data with IV prepended
     */
    fun encryptBackupData(context: Context, plaintext: String): EncryptionResult {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data
            val combined = iv + encryptedData
            val base64Encrypted = Base64.encodeToString(combined, Base64.NO_WRAP)
            
            // Generate data hash for integrity verification
            val dataHash = generateSHA256Hash(plaintext)
            
            EncryptionResult.Success(base64Encrypted, dataHash)
        } catch (e: Exception) {
            EncryptionResult.Error("Encryption failed: ${e.message}")
        }
    }
    
    /**
     * Decrypts backup data using AES-256-GCM
     * Expects base64-encoded data with IV prepended
     */
    fun decryptBackupData(context: Context, encryptedData: String): DecryptionResult {
        return try {
            val secretKey = getOrCreateSecretKey()
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Extract IV and encrypted data
            val iv = combined.sliceArray(0..GCM_IV_LENGTH - 1)
            val encrypted = combined.sliceArray(GCM_IV_LENGTH until combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encrypted)
            val plaintext = String(decryptedBytes, Charsets.UTF_8)
            
            DecryptionResult.Success(plaintext)
        } catch (e: Exception) {
            DecryptionResult.Error("Decryption failed: ${e.message}")
        }
    }
    
    /**
     * Verifies data integrity using SHA-256 hash comparison
     */
    fun verifyDataIntegrity(data: String, expectedHash: String): Boolean {
        return try {
            val actualHash = generateSHA256Hash(data)
            actualHash == expectedHash
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generates SHA-256 hash for data integrity verification
     */
    fun generateSHA256Hash(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
    
    /**
     * Generates a unique device ID for backup identification
     */
    fun generateDeviceId(context: Context): String {
        // Use a combination of device characteristics for unique ID
        val deviceInfo = "${android.os.Build.MODEL}_${android.os.Build.MANUFACTURER}_${System.currentTimeMillis()}"
        return generateSHA256Hash(deviceInfo).take(16) // Use first 16 chars of hash
    }
    
    /**
     * Gets or creates the secret key in Android Keystore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        return if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            // Key exists, retrieve it
            keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        } else {
            // Key doesn't exist, create it
            createSecretKey()
        }
    }
    
    /**
     * Creates a new secret key in Android Keystore
     */
    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Clears the encryption key (for security purposes, e.g., user logout)
     */
    fun clearEncryptionKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
        } catch (e: Exception) {
            // Log error but don't throw - key clearing is best effort
        }
    }
}

/**
 * Result wrapper for encryption operations
 */
sealed class EncryptionResult {
    data class Success(val encryptedData: String, val dataHash: String) : EncryptionResult()
    data class Error(val message: String) : EncryptionResult()
}

/**
 * Result wrapper for decryption operations
 */
sealed class DecryptionResult {
    data class Success(val decryptedData: String) : DecryptionResult()
    data class Error(val message: String) : DecryptionResult()
}

/**
 * Utility extensions for encryption operations
 */
fun String.encrypt(context: Context): EncryptionResult {
    return EncryptionUtils.encryptBackupData(context, this)
}

fun String.decrypt(context: Context): DecryptionResult {
    return EncryptionUtils.decryptBackupData(context, this)
}

fun String.sha256Hash(): String {
    return EncryptionUtils.generateSHA256Hash(this)
}