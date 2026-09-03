package dev.x3d.dayline.data.rpc

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object Totp {
    fun generate(secretHex: String, timeMillis: Long = System.currentTimeMillis(), digits: Int = 6, periodSeconds: Int = 30): String {
        val key = decodeSecret(secretHex)
        val counter = timeMillis / 1000L / periodSeconds
        val data = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFF).toByte()
            value = value ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash.last().toInt() and 0x0F
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 10.0.pow(digits).toInt()
        return otp.toString().padStart(digits, '0')
    }

    internal fun decodeSecret(secretHex: String): ByteArray {
        val cleaned = secretHex.trim().replace(" ", "").replace("-", "")
        if (cleaned.length % 2 == 0 && cleaned.matches(Regex("[0-9a-fA-F]+"))) {
            return cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
        return decodeBase32(cleaned)
    }

    private fun decodeBase32(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val normalized = input.uppercase().replace("=", "").filter { it in alphabet }
        var buffer = 0
        var bitsLeft = 0
        val out = ArrayList<Byte>()
        for (ch in normalized) {
            buffer = (buffer shl 5) or alphabet.indexOf(ch)
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}
