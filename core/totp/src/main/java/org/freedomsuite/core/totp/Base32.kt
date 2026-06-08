package org.freedomsuite.core.totp

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        val cleaned = input.uppercase().replace(" ", "").replace("=", "")
        var buffer = 0
        var bitsLeft = 0
        val output = mutableListOf<Byte>()
        for (char in cleaned) {
            val value = ALPHABET.indexOf(char)
            require(value >= 0) { "Invalid Base32 character: $char" }
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val output = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (byte in input) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                output.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            output.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return output.toString()
    }
}
