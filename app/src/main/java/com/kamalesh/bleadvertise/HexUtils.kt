package com.kamalesh.bleadvertise

object HexUtils {

    fun decodeHex(hex: String): ByteArray {
        val clean = hex.replace(" ", "")
        require(clean.length % 2 == 0) { "Hex string must have even length: $hex" }
        return ByteArray(clean.length / 2) { index ->
            val hi = Character.digit(clean[index * 2], 16)
            val lo = Character.digit(clean[index * 2 + 1], 16)
            require(hi != -1 && lo != -1) { "Invalid hex string: $hex" }
            ((hi shl 4) or lo).toByte()
        }
    }

    fun toHexString(bytes: ByteArray): String =
        bytes.joinToString("") { String.format("%02X", it.toInt() and 0xFF) }

    fun toHexString(byte: Byte): String = String.format("%02X", byte.toInt() and 0xFF)
}
