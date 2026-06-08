package org.freedomsuite.protocol.mime

data class ParsedEmail(
    val plainBody: String,
    val calendarParts: List<String>,
)

object MimeParser {
    fun parse(raw: String): ParsedEmail {
        val normalized = raw.replace("\r\n", "\n")
        val split = splitHeadersAndBody(normalized)
        if (split == null) {
            return ParsedEmail(plainBody = normalized.trim(), calendarParts = emptyList())
        }
        val (headers, body) = split
        val contentType = headerValue(headers, "Content-Type") ?: "text/plain"
        return parsePart(headers, body, contentType)
    }

    private fun parsePart(headers: String, body: String, contentType: String): ParsedEmail {
        val type = contentType.lowercase().substringBefore(";").trim()
        val boundary = parameter(contentType, "boundary")
        return when {
            type.startsWith("multipart/") && boundary != null ->
                parseMultipart(body, boundary)
            type.contains("text/calendar") || type.contains("application/ics") ->
                ParsedEmail(plainBody = "", calendarParts = listOf(decodeBody(headers, body).trim()))
            else ->
                ParsedEmail(plainBody = decodeBody(headers, body), calendarParts = emptyList())
        }
    }

    private fun parseMultipart(body: String, boundary: String): ParsedEmail {
        val marker = "--$boundary"
        val parts = body.split(marker)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("--") }

        var plain = ""
        val calendars = mutableListOf<String>()
        for (part in parts) {
            val split = splitHeadersAndBody(part) ?: continue
            val (headers, partBody) = split
            val contentType = headerValue(headers, "Content-Type") ?: "text/plain"
            val type = contentType.lowercase()
            when {
                type.startsWith("multipart/") -> {
                    val nested = parsePart(headers, partBody, contentType)
                    if (plain.isBlank()) plain = nested.plainBody
                    calendars += nested.calendarParts
                }
                type.contains("text/calendar") || type.contains("application/ics") ->
                    calendars += decodeBody(headers, partBody).trim()
                type.contains("text/plain") && plain.isBlank() ->
                    plain = decodeBody(headers, partBody)
            }
        }
        return ParsedEmail(plainBody = plain.trim(), calendarParts = calendars)
    }

    private fun splitHeadersAndBody(raw: String): Pair<String, String>? {
        val index = raw.indexOf("\n\n")
        if (index < 0) return null
        return raw.substring(0, index) to raw.substring(index + 2)
    }

    private fun headerValue(headers: String, name: String): String? {
        val pattern = Regex("""(?im)^${Regex.escape(name)}:\s*(.+)$""")
        return headers.lineSequence()
            .mapNotNull { line ->
                val match = pattern.find(line) ?: return@mapNotNull null
                match.groupValues[1].trim()
            }
            .firstOrNull()
    }

    private fun parameter(contentType: String, key: String): String? {
        val pattern = Regex("""(?i)$key="?([^";]+)"?""")
        return pattern.find(contentType)?.groupValues?.get(1)
    }

    private fun decodeBody(headers: String, body: String): String {
        val encoding = headerValue(headers, "Content-Transfer-Encoding")?.lowercase()
        return when (encoding) {
            "base64" -> runCatching {
                String(android.util.Base64.decode(body.replace("\n", ""), android.util.Base64.DEFAULT))
            }.getOrDefault(body)
            "quoted-printable" -> decodeQuotedPrintable(body)
            else -> body
        }
    }

    private fun decodeQuotedPrintable(input: String): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        val text = input.replace("=\n", "")
        while (i < text.length) {
            if (text[i] == '=' && i + 2 < text.length) {
                val hex = text.substring(i + 1, i + 3)
                bytes += hex.toInt(16).toByte()
                i += 3
            } else {
                bytes += text[i].code.toByte()
                i++
            }
        }
        return String(bytes.toByteArray())
    }
}
