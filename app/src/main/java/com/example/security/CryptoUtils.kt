package com.example.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun deriveKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }

    fun encrypt(plainText: String, secretKey: SecretKeySpec): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            // Use static / semi-static IV or prefix IV. For simplicity, we can use 16 bytes derived from key or fixed prefix for local demo reliability
            val iv = ByteArray(16)
            System.arraycopy(secretKey.encoded, 0, iv, 0, 16)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText // Fallback
        }
    }

    fun decrypt(cipherText: String, secretKey: SecretKeySpec): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            System.arraycopy(secretKey.encoded, 0, iv, 0, 16)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decodedValue = Base64.decode(cipherText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedValue)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error al desencriptar / Contraseña incorrecta"
        }
    }
}
