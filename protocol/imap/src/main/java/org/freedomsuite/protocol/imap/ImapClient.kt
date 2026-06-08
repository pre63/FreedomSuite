package org.freedomsuite.protocol.imap

import org.freedomsuite.core.account.MailAccount
import javax.net.ssl.SSLSocketFactory

interface ImapClient {
    suspend fun connect(account: MailAccount, password: CharArray): Result<Unit>
    suspend fun disconnect()
    suspend fun listFolders(): Result<List<String>>
    suspend fun fetchFolder(folder: String, limit: Int = 50): Result<List<MailMessageSummary>>
    suspend fun searchFolder(folder: String, query: String, limit: Int = 50): Result<List<MailMessageSummary>>
    suspend fun fetchMessageBody(folder: String, uid: Long): Result<String>
    suspend fun fetchRawMessage(folder: String, uid: Long): Result<String>
    suspend fun moveToArchive(folder: String, uid: Long): Result<Unit>

    suspend fun fetchInbox(limit: Int = 50): Result<List<MailMessageSummary>> =
        fetchFolder("INBOX", limit)
}

data class MailMessageSummary(
    val uid: Long,
    val subject: String,
    val from: String,
    val dateEpochMs: Long,
    val snippet: String,
)

object ImapClientFactory {
    fun create(
        plainText: Boolean = false,
        sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
    ): ImapClient = JavaImapClient(plainText, sslSocketFactory)
}
