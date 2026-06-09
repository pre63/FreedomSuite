package org.freedomsuite.core.account

import android.content.Context
import org.freedomsuite.core.account.discovery.MailServerSettings
import org.freedomsuite.core.crypto.SecurePreferences

class AccountStore(context: Context) {
    private val prefs = SecurePreferences(context.applicationContext, PREFS_NAME)

    fun hasAccount(): Boolean = prefs.getString(KEY_EMAIL) != null

    fun getAccount(): MailAccount? {
        val email = prefs.getString(KEY_EMAIL) ?: return null
        return MailAccount(
            email = email,
            imapHost = prefs.getString(KEY_IMAP_HOST) ?: return null,
            imapPort = prefs.getInt(KEY_IMAP_PORT, 993),
            smtpHost = prefs.getString(KEY_SMTP_HOST) ?: return null,
            smtpPort = prefs.getInt(KEY_SMTP_PORT, 465),
            caldavUrl = prefs.getString(KEY_CALDAV_URL)
                ?: org.freedomsuite.core.network.MailboxOrgDefaults.CALDAV_URL,
            plainText = prefs.getBoolean(KEY_PLAIN_TEXT, false),
            aliases = prefs.getString(KEY_ALIASES)?.split(ALIAS_DELIMITER)?.filter { it.isNotBlank() }.orEmpty(),
            ownedDomains = prefs.getString(KEY_OWNED_DOMAINS)?.split(ALIAS_DELIMITER)?.filter { it.isNotBlank() }.orEmpty(),
            identities = buildIdentities(email, prefs.getString(KEY_ALIASES)),
        )
    }

    fun getPassword(): CharArray? = prefs.getCharArray(KEY_PASSWORD)

    fun saveAccount(account: MailAccount, password: CharArray) {
        prefs.putString(KEY_EMAIL, account.email)
        prefs.putString(KEY_IMAP_HOST, account.imapHost)
        prefs.putInt(KEY_IMAP_PORT, account.imapPort)
        prefs.putString(KEY_SMTP_HOST, account.smtpHost)
        prefs.putInt(KEY_SMTP_PORT, account.smtpPort)
        prefs.putString(KEY_CALDAV_URL, account.caldavUrl)
        prefs.putBoolean(KEY_PLAIN_TEXT, account.plainText)
        prefs.putString(KEY_ALIASES, account.aliases.joinToString(ALIAS_DELIMITER))
        prefs.putString(
            KEY_OWNED_DOMAINS,
            account.ownedDomains.joinToString(ALIAS_DELIMITER),
        )
        prefs.putCharArray(KEY_PASSWORD, password)
    }

    fun clearAccount() {
        prefs.remove(KEY_EMAIL)
        prefs.remove(KEY_IMAP_HOST)
        prefs.remove(KEY_IMAP_PORT)
        prefs.remove(KEY_SMTP_HOST)
        prefs.remove(KEY_SMTP_PORT)
        prefs.remove(KEY_CALDAV_URL)
        prefs.remove(KEY_PLAIN_TEXT)
        prefs.remove(KEY_ALIASES)
        prefs.remove(KEY_OWNED_DOMAINS)
        prefs.remove(KEY_PASSWORD)
    }

    fun updateMailboxExtras(aliases: List<String>, ownedDomains: List<String>) {
        val account = getAccount() ?: return
        saveAccount(
            account.copy(
                aliases = aliases,
                ownedDomains = ownedDomains,
                identities = buildIdentities(account.email, aliases.joinToString(ALIAS_DELIMITER)),
            ),
            getPassword() ?: return,
        )
    }

    fun accountFromSettings(email: String, settings: MailServerSettings): MailAccount =
        settings.toMailAccount(email).copy(
            ownedDomains = listOfNotNull(
                email.trim().substringAfter('@', "").lowercase().takeIf { it.isNotEmpty() },
            ),
            identities = listOf(EmailIdentity(email.trim(), "Primary", isDefault = true)),
        )

    private fun buildIdentities(primary: String, aliasesRaw: String?): List<EmailIdentity> {
        val identities = mutableListOf(EmailIdentity(primary, "Primary", isDefault = true))
        aliasesRaw?.split(ALIAS_DELIMITER)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it.contains('@') }
            ?.forEachIndexed { index, alias ->
                identities += EmailIdentity(alias, "Alias ${index + 1}")
            }
        return identities
    }

    companion object {
        private const val ALIAS_DELIMITER = ","
        private const val PREFS_NAME = "freedom_mail_account"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IMAP_HOST = "imap_host"
        private const val KEY_IMAP_PORT = "imap_port"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_CALDAV_URL = "caldav_url"
        private const val KEY_PLAIN_TEXT = "plain_text"
        private const val KEY_ALIASES = "aliases"
        private const val KEY_OWNED_DOMAINS = "owned_domains"
    }
}
