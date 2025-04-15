package com.simiacryptus.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EncryptionUtilTest {

    @Test
    fun testEncryptAndDecrypt() {
        val key = "mysecretkey12345"
        var originalText = "Hello, World!"
        for( i in 0..10) {
            val encryptedText = EncryptionUtil.encrypt(originalText, key)
            assertNotNull(encryptedText, "Encryption should not return null")
            val decryptedText = EncryptionUtil.decrypt(encryptedText, key)
            assertNotNull(decryptedText, "Decryption should not return null")
            assertEquals(originalText, decryptedText, "Decrypted text should match the original text")
            originalText += CharArray(100) { 'a' }
        }
    }

    @Test
    fun testEncryptWithNullValue() {
        val key = "mysecretkey12345"
        val encryptedText = EncryptionUtil.encrypt(null, key)
        assertNull(encryptedText, "Encryption of null value should return null")
    }

    @Test
    fun testDecryptWithNullValue() {
        val key = "mysecretkey12345"
        val decryptedText = EncryptionUtil.decrypt(null, key)
        assertNull(decryptedText, "Decryption of null value should return null")
    }

    @Test
    fun testEncryptWithEmptyKey() {
        val originalText = "Hello, World!"
        val encryptedText = EncryptionUtil.encrypt(originalText, "")
        assertNull(encryptedText, "Encryption with empty key should return null")
    }

    @Test
    fun testDecryptWithEmptyKey() {
        val encryptedText = "someEncryptedText"
        val decryptedText = EncryptionUtil.decrypt(encryptedText, "")
        assertNull(decryptedText, "Decryption with empty key should return null")
    }
}