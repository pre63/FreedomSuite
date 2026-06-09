package org.freedomsuite.core.account.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.freedomsuite.core.network.PrivacyHttpClient
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Mozilla / Thunderbird ISPDB autoconfig over HTTPS.
 * https://wiki.mozilla.org/Thunderbird:Autoconfiguration
 */
class MailAutoconfigDiscovery(
    private val httpClient: okhttp3.OkHttpClient = PrivacyHttpClient.create(
        connectTimeoutSeconds = 10,
        readTimeoutSeconds = 15,
    ),
) {
    suspend fun discover(domain: String): MailServerSettings? = withContext(Dispatchers.IO) {
        val normalized = domain.trim().lowercase()
        if (normalized.isEmpty()) return@withContext null

        val urls = listOf(
            "https://autoconfig.$normalized/mail/config-v1.1.xml",
            "https://$normalized/.well-known/autoconfig/mail/config-v1.1.xml",
            "https://www.$normalized/.well-known/autoconfig/mail/config-v1.1.xml",
        )

        for (url in urls) {
            val xml = fetchXml(url) ?: continue
            parseAutoconfig(xml)?.let { return@withContext it }
        }
        null
    }

    private fun fetchXml(url: String): String? = runCatching {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()?.takeIf { it.contains("clientConfig") }
        }
    }.getOrNull()

    internal fun parseAutoconfig(xml: String): MailServerSettings? {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var imapHost: String? = null
        var imapPort = 993
        var smtpHost: String? = null
        var smtpPort = 587

        var event = parser.eventType
        var inIncomingImap = false
        var inOutgoingSmtp = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "incomingServer" -> {
                        inIncomingImap = parser.getAttributeValue(null, "type") == "imap"
                        inOutgoingSmtp = false
                    }
                    "outgoingServer" -> {
                        inOutgoingSmtp = parser.getAttributeValue(null, "type") == "smtp"
                        inIncomingImap = false
                    }
                    "hostname" -> when {
                        inIncomingImap -> imapHost = readText(parser)
                        inOutgoingSmtp -> smtpHost = readText(parser)
                    }
                    "port" -> when {
                        inIncomingImap -> imapPort = readText(parser).toIntOrNull() ?: imapPort
                        inOutgoingSmtp -> smtpPort = readText(parser).toIntOrNull() ?: smtpPort
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "incomingServer" -> inIncomingImap = false
                    "outgoingServer" -> inOutgoingSmtp = false
                }
            }
            event = parser.next()
        }

        if (imapHost.isNullOrBlank() || smtpHost.isNullOrBlank()) return null
        return MailServerSettings(
            imapHost = imapHost,
            imapPort = imapPort,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            source = DiscoverySource.AUTOCONFIG,
            label = "autoconfig",
        )
    }

    private fun readText(parser: XmlPullParser): String {
        if (parser.next() == XmlPullParser.TEXT) {
            return parser.text?.trim().orEmpty()
        }
        return ""
    }
}
