package org.freedomsuite.core.totp

import java.net.URI

data class OtpAuthAccount(
    val issuer: String,
    val accountName: String,
    val secretBase32: String,
    val algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int = 30,
)

object OtpAuthParser {
    fun parse(uriString: String): Result<OtpAuthAccount> = runCatching {
        val uri = URI(uriString.trim())
        require(uri.scheme == "otpauth") { "Not an otpauth URI" }
        require(uri.host == "totp") { "Only TOTP is supported" }

        val params = uri.rawQuery?.split("&").orEmpty().associate { part ->
            val pieces = part.split("=", limit = 2)
            pieces[0].lowercase() to pieces.getOrElse(1) { "" }
        }

        val secret = params["secret"] ?: error("Missing secret")
        val path = uri.path.removePrefix("/")
        val label = uri.schemeSpecificPart.substringAfter("totp/").substringBefore("?")
        val issuerFromLabel = label.substringBefore(":", missingDelimiterValue = "")
        val accountFromLabel = label.substringAfter(":", missingDelimiterValue = label)
        val issuer = params["issuer"]?.takeIf { it.isNotBlank() }
            ?: issuerFromLabel.ifBlank { "Unknown" }

        OtpAuthAccount(
            issuer = issuer,
            accountName = accountFromLabel.ifBlank { issuer },
            secretBase32 = secret.uppercase(),
            algorithm = TotpAlgorithm.fromName(params["algorithm"]),
            digits = params["digits"]?.toIntOrNull() ?: 6,
            periodSeconds = params["period"]?.toIntOrNull() ?: 30,
        )
    }

    fun normalizeSecret(input: String): String {
        val cleaned = input.uppercase().replace(" ", "")
        Base32.decode(cleaned)
        return cleaned
    }
}
