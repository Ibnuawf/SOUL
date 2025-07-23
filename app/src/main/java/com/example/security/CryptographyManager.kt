package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptographyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val AES_KEY_ALIAS = "soul_vault_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(AES_KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        
        // layout: [4-byte IV len][IV bytes][ciphertext]
        val ivSize = iv.size
        val output = ByteArray(4 + ivSize + ciphertext.size)
        output[0] = (ivSize shr 24).toByte()
        output[1] = (ivSize shr 16).toByte()
        output[2] = (ivSize shr 8).toByte()
        output[3] = ivSize.toByte()
        System.arraycopy(iv, 0, output, 4, ivSize)
        System.arraycopy(ciphertext, 0, output, 4 + ivSize, ciphertext.size)
        
        return output
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.size < 4) {
            throw IllegalArgumentException("Data is too short or invalid")
        }
        
        // parse IV size
        val ivSize = ((encryptedData[0].toInt() and 0xFF) shl 24) or
                     ((encryptedData[1].toInt() and 0xFF) shl 16) or
                     ((encryptedData[2].toInt() and 0xFF) shl 8) or
                     (encryptedData[3].toInt() and 0xFF)
                     
        if (encryptedData.size < 4 + ivSize) {
            throw IllegalArgumentException("Data does not contain a full IV")
        }
        
        val iv = ByteArray(ivSize)
        System.arraycopy(encryptedData, 4, iv, 0, ivSize)
        
        val ciphertextLength = encryptedData.size - 4 - ivSize
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(encryptedData, 4 + ivSize, ciphertext, 0, ciphertextLength)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(ciphertext)
    }

    fun encryptFileStream(sourceFile: java.io.File, destFile: java.io.File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val ivSize = iv.size

        java.io.FileOutputStream(destFile).use { outputStream ->
            // write IV size + IV
            outputStream.write(ByteArray(4).apply {
                this[0] = (ivSize shr 24).toByte()
                this[1] = (ivSize shr 16).toByte()
                this[2] = (ivSize shr 8).toByte()
                this[3] = ivSize.toByte()
            })
            outputStream.write(iv)

            java.io.FileInputStream(sourceFile).use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    val encryptedChunk = cipher.update(buffer, 0, bytesRead)
                    if (encryptedChunk != null && encryptedChunk.isNotEmpty()) {
                        outputStream.write(encryptedChunk)
                    }
                    bytesRead = inputStream.read(buffer)
                }
                val finalChunk = cipher.doFinal()
                if (finalChunk != null && finalChunk.isNotEmpty()) {
                    outputStream.write(finalChunk)
                }
            }
        }
    }

    fun decryptFileStream(sourceFile: java.io.File, destFile: java.io.File) {
        java.io.FileInputStream(sourceFile).use { inputStream ->
            val ivSizeBuffer = ByteArray(4)
            if (inputStream.read(ivSizeBuffer) != 4) {
                throw IllegalArgumentException("Data is too short or invalid")
            }
            val ivSize = ((ivSizeBuffer[0].toInt() and 0xFF) shl 24) or
                         ((ivSizeBuffer[1].toInt() and 0xFF) shl 16) or
                         ((ivSizeBuffer[2].toInt() and 0xFF) shl 8) or
                         (ivSizeBuffer[3].toInt() and 0xFF)

            val iv = ByteArray(ivSize)
            if (inputStream.read(iv) != ivSize) {
                throw IllegalArgumentException("Data does not contain a full IV")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            java.io.FileOutputStream(destFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    val decryptedChunk = cipher.update(buffer, 0, bytesRead)
                    if (decryptedChunk != null && decryptedChunk.isNotEmpty()) {
                        outputStream.write(decryptedChunk)
                    }
                    bytesRead = inputStream.read(buffer)
                }
                val finalChunk = cipher.doFinal()
                if (finalChunk != null && finalChunk.isNotEmpty()) {
                    outputStream.write(finalChunk)
                }
            }
        }
    }
}
