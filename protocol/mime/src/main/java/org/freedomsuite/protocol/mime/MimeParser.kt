package org.freedomsuite.protocol.mime

data class ParsedEmail(
    val plainBody: String,
    val calendarParts: List<String>,
    val headers: Map<String, List<String>> = emptyMap(),
)

object MimeParser {
    fun parse(raw: String): ParsedEmail {
        val normalized = raw.replace("\r\n", "\n")
        val split = splitHeadersAndBody(normalized)
        if (split == null) {
            return ParsedEmail(plainBody = normalized.trim(), calendarParts = emptyList())
        }
        val (headers, body) = split
        val headerMap = parseHeaders(headers)
        val contentType = headerValues(headerMap, "Content-Type").firstOrNull() ?: "text/plain"
        val parsed = parsePart(headers, body, contentType)
        return parsed.copy(headers = headerMap)
    }

    fun parseHeaders(headerBlock: String): Map<String, List<String>> {
        val unfolded = unfoldHeaders(headerBlock.replace("\r\n", "\n"))
        val result = linkedMapOf<String, MutableList<String>>()
        for (line in unfolded.lineSequence()) {
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val name = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            if (value.isEmpty()) continue
            result.getOrPut(name) { mutableListOf() } += value
        }
        return result
    }

    fun headerValues(headers: Map<String, List<String>>, name: String): List<String> =
        headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            .orEmpty()

    private fun unfoldHeaders(block: String): String {
        val lines = block.lines()
        if (lines.isEmpty()) return block
        val out = StringBuilder(lines.first())
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.startsWith(" ") || line.startsWith("\t")) {
                out.append(' ')
                out.append(line.trim())
            } else {
                out.append('\n')
                out.append(line)
            }
        }
        return out.toString()
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

    private fun headerValue(headers: String, name: String): String? =
        parseHeaders(headers)[name]?.firstOrNull()
            ?: parseHeaders(headers).entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

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
