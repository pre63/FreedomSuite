package org.freedomsuite.protocol.imap

import org.freedomsuite.core.account.MailAccount
import javax.net.ssl.SSLSocketFactory

internal class JavaImapClient(
    private val plainText: Boolean = false,
    private val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
) : ImapClient {
    private var connection: ImapConnection? = null

    override suspend fun connect(account: MailAccount, password: CharArray): Result<Unit> {
        val conn = ImapConnection(account.imapHost, account.imapPort, plainText, sslSocketFactory)
        return conn.connect()
            .mapCatching {
                conn.login(account.email, password).getOrThrow()
                connection = conn
            }
    }

    override suspend fun disconnect() {
        connection?.disconnect()
        connection = null
    }

    override suspend fun listFolders(): Result<List<String>> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return conn.listFolders()
    }

    override suspend fun fetchFolder(folder: String, limit: Int): Result<List<MailMessageSummary>> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return conn.fetchFolder(folder, limit)
    }

    override suspend fun searchFolder(folder: String, query: String, limit: Int): Result<List<MailMessageSummary>> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return conn.searchFolder(folder, query, limit)
    }

    override suspend fun fetchMessageBody(folder: String, uid: Long): Result<String> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return conn.fetchMessageBody(folder, uid)
    }

    override suspend fun fetchRawMessage(folder: String, uid: Long): Result<String> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return conn.fetchRawMessage(folder, uid)
    }

    override suspend fun moveToArchive(folder: String, uid: Long): Result<Unit> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return conn.moveToArchive(folder, uid)
    }
}
