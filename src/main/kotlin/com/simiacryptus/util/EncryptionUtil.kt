package com.simiacryptus.util

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object EncryptionUtil {
    private const val ALGORITHM = "AES"

    fun encrypt(value: String?, key: String): String? {
        if (value == null) return null
        if (key.isEmpty()) return null
        val keySpec = SecretKeySpec(key.padEnd(16).toByteArray().copyOf(16), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encryptedValue = cipher.doFinal(value.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedValue)
    }

    fun decrypt(value: String?, key: String): String? {
        if (value == null) return null
        if (key.isEmpty()) return null
        val keySpec = SecretKeySpec(key.padEnd(16).toByteArray().copyOf(16), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decodedValue = Base64.getDecoder().decode(value)
        val decryptedValue = cipher.doFinal(decodedValue)
        return String(decryptedValue)
    }
}