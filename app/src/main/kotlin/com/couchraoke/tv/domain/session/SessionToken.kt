package com.couchraoke.tv.domain.session

import java.security.SecureRandom
import java.nio.ByteBuffer

object SessionToken {

    private val random = SecureRandom()
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val TOKEN_LENGTH = 10

    fun generate(): String {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        // Map 8 random bytes to 10 uppercase chars (base-26)
        var value = ByteBuffer.wrap(bytes).long and Long.MAX_VALUE
        val sb = StringBuilder(TOKEN_LENGTH)
        repeat(TOKEN_LENGTH) {
            sb.append(ALPHABET[(value % 26).toInt()])
            value /= 26
        }
        return sb.reverse().toString()
    }

    fun normalize(input: String): String =
        input.uppercase().replace(" ", "").replace("-", "")

    fun matches(input: String, token: String): Boolean =
        normalize(input) == normalize(token)

    fun display(token: String): String {
        val normalized = normalize(token)
        return "${normalized.take(5)}-${normalized.drop(5)}"
    }
}
