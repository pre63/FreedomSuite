package org.freedomsuite.core.totp

enum class TotpAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
    ;

    companion object {
        fun fromName(name: String?): TotpAlgorithm = when (name?.uppercase()) {
            "SHA256", "SHA-256" -> SHA256
            "SHA512", "SHA-512" -> SHA512
            else -> SHA1
        }
    }
}
