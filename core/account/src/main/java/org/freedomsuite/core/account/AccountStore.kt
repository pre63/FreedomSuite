package org.freedomsuite.core.account

import android.content.Context
import org.freedomsuite.core.crypto.SecurePreferences
import org.freedomsuite.core.network.isMailboxOrgDomain

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
            identities = listOf(EmailIdentity(email, "Primary", isDefault = true)),
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
        prefs.putCharArray(KEY_PASSWORD, password)
    }

    fun clearAccount() {
        prefs.remove(KEY_EMAIL)
        prefs.remove(KEY_IMAP_HOST)
        prefs.remove(KEY_IMAP_PORT)
        prefs.remove(KEY_SMTP_HOST)
        prefs.remove(KEY_SMTP_PORT)
        prefs.remove(KEY_CALDAV_URL)
        prefs.remove(KEY_PASSWORD)
    }

    fun createMailboxOrgAccount(email: String): MailAccount {
        return if (isMailboxOrgDomain(email)) {
            MailAccount.mailboxOrg(email.trim())
        } else {
            MailAccount(
                email = email.trim(),
                imapHost = org.freedomsuite.core.network.MailboxOrgDefaults.IMAP_HOST,
                imapPort = org.freedomsuite.core.network.MailboxOrgDefaults.IMAP_PORT,
                smtpHost = org.freedomsuite.core.network.MailboxOrgDefaults.SMTP_HOST,
                smtpPort = org.freedomsuite.core.network.MailboxOrgDefaults.SMTP_PORT_SSL,
                caldavUrl = org.freedomsuite.core.network.MailboxOrgDefaults.CALDAV_URL,
                identities = listOf(EmailIdentity(email.trim(), "Primary", isDefault = true)),
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "freedom_mail_account"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IMAP_HOST = "imap_host"
        private const val KEY_IMAP_PORT = "imap_port"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_CALDAV_URL = "caldav_url"
    }
}
