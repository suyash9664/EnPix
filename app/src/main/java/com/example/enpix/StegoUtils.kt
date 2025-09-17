package com.example.enpix

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object StegoUtils {

    /** Convert Bitmap → PNG bytes */
    fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }

    /** Embed encrypted message into Bitmap using LSB */
    fun embedMessageInBitmap(src: Bitmap, message: String, password: String): Bitmap {
        val encrypted = encryptMessage(message, password)
        val length = encrypted.size

        val width = src.width
        val height = src.height
        val totalPixels = width * height

        // Need space for length (4 bytes = 32 bits) + message bits
        val requiredBits = 32 + (length * 8)
        if (requiredBits > totalPixels * 4) {
            throw IllegalArgumentException("Message too large for this image")
        }

        val result = src.copy(Bitmap.Config.ARGB_8888, true)

        var bitIndex = 0

        // First store length (4 bytes)
        for (i in 0 until 4) {
            val b = (length shr (i * 8)) and 0xFF
            for (j in 0 until 8) {
                setPixelBit(result, bitIndex, (b shr j) and 1)
                bitIndex++
            }
        }

        // Then store encrypted message
        for (byte in encrypted) {
            for (j in 0 until 8) {
                setPixelBit(result, bitIndex, (byte.toInt() shr j) and 1)
                bitIndex++
            }
        }

        return result
    }

    /** Extract message from Bitmap using password */
    fun extractMessageFromBitmap(src: Bitmap, password: String): String {
        val width = src.width
        val height = src.height
        val totalPixels = width * height

        // Read 32 bits for length
        var bitIndex = 0
        var length = 0
        for (i in 0 until 4) {
            var b = 0
            for (j in 0 until 8) {
                val bit = getPixelBit(src, bitIndex)
                b = b or (bit shl j)
                bitIndex++
            }
            length = length or (b shl (i * 8))
        }

        // Safety check
        val requiredBits = 32 + (length * 8)
        if (requiredBits > totalPixels * 4) {
            throw IllegalArgumentException("Corrupted image or wrong password")
        }

        // Extract encrypted message
        val encrypted = ByteArray(length)
        for (i in 0 until length) {
            var b = 0
            for (j in 0 until 8) {
                val bit = getPixelBit(src, bitIndex)
                b = b or (bit shl j)
                bitIndex++
            }
            encrypted[i] = b.toByte()
        }

        return decryptMessage(encrypted, password)
    }

    /** Modify one pixel channel’s LSB */
    private fun setPixelBit(bitmap: Bitmap, bitIndex: Int, bit: Int) {
        val x = (bitIndex / 4) % bitmap.width
        val y = (bitIndex / 4) / bitmap.width
        val channel = bitIndex % 4

        var color = bitmap.getPixel(x, y)

        when (channel) {
            0 -> { // A
                val a = (color ushr 24 and 0xFE) or bit
                color = (color and 0x00FFFFFF) or (a shl 24)
            }
            1 -> { // R
                val r = (color ushr 16 and 0xFE) or bit
                color = (color and 0xFF00FFFF.toInt()) or (r shl 16)
            }
            2 -> { // G
                val g = (color ushr 8 and 0xFE) or bit
                color = (color and 0xFFFF00FF.toInt()) or (g shl 8)
            }
            3 -> { // B
                val b = (color and 0xFE) or bit
                color = (color and 0xFFFFFF00.toInt()) or b
            }
        }

        bitmap.setPixel(x, y, color)
    }

    /** Read one pixel channel’s LSB */
    private fun getPixelBit(bitmap: Bitmap, bitIndex: Int): Int {
        val x = (bitIndex / 4) % bitmap.width
        val y = (bitIndex / 4) / bitmap.width
        val channel = bitIndex % 4

        val color = bitmap.getPixel(x, y)

        return when (channel) {
            0 -> (color ushr 24) and 1
            1 -> (color ushr 16) and 1
            2 -> (color ushr 8) and 1
            3 -> color and 1
            else -> 0
        }
    }

    // ---------------- ENCRYPTION HELPERS ---------------- //

    private fun generateKey(password: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes.copyOf(16), "AES")
    }

    private fun encryptMessage(message: String, password: String): ByteArray {
        val key = generateKey(password)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) { 0 } // fixed IV for simplicity
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(message.toByteArray(Charsets.UTF_8))
    }

    private fun decryptMessage(bytes: ByteArray, password: String): String {
        val key = generateKey(password)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) { 0 }
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return String(cipher.doFinal(bytes), Charsets.UTF_8)
    }
}
