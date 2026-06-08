package org.freedomsuite.inbox.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.freedomsuite.core.account.AccountStore
import org.freedomsuite.core.account.EmailIdentity
import org.freedomsuite.core.calendarapi.CalendarBridge
import org.freedomsuite.core.calendarapi.CalendarBridgeClient
import org.freedomsuite.protocol.ical.IcalParser
import org.freedomsuite.protocol.ical.InviteMethod
import org.freedomsuite.protocol.ical.InviteResponseStatus
import org.freedomsuite.protocol.imap.ImapClientFactory
import org.freedomsuite.protocol.imap.MailMessageSummary
import org.freedomsuite.protocol.mime.MimeParser
import org.freedomsuite.protocol.smtp.OutgoingMessage
import org.freedomsuite.protocol.smtp.SmtpClientFactory

class InboxRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = InboxDatabase.getInstance(appContext).inboxDao()
    private val accountStore = AccountStore(appContext)
    private val calendarBridge = CalendarBridgeClient(appContext)

    fun hasAccount(): Boolean = accountStore.hasAccount()

    fun observeFolder(folder: String): Flow<List<MailMessageEntity>> = dao.observeFolder(folder)

    fun observeFolderSearch(folder: String, query: String): Flow<List<MailMessageEntity>> =
        dao.observeFolderSearch(folder, query)

    fun isCalendarInstalled(): Boolean = calendarBridge.isCalendarInstalled()

    suspend fun configureAccount(email: String, password: String): Result<Unit> {
        val account = accountStore.createMailboxOrgAccount(email)
        val passwordChars = password.toCharArray()
        return runCatching {
            val client = ImapClientFactory.create()
            client.connect(account, passwordChars).getOrThrow()
            client.disconnect()
            accountStore.saveAccount(account, passwordChars)
        }
    }

    suspend fun listFolders(): Result<List<String>> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = ImapClientFactory.create()
        return runCatching {
            client.connect(account, password).getOrThrow()
            val folders = client.listFolders().getOrThrow()
            client.disconnect()
            folders
        }
    }

    suspend fun syncFolder(folder: String): Result<Int> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = ImapClientFactory.create()
        return runCatching {
            client.connect(account, password).getOrThrow()
            val summaries = client.fetchFolder(folder, limit = 50).getOrThrow()
            val entities = summaries.map { summary ->
                mapSummaryToEntity(client, folder, summary, fetchBodies = true)
            }
            dao.clearFolder(folder)
            dao.upsertAll(entities)
            client.disconnect()
            entities.size
        }
    }

    suspend fun searchFolder(folder: String, query: String): Result<Int> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(0)
        val client = ImapClientFactory.create()
        return runCatching {
            client.connect(account, password).getOrThrow()
            val summaries = client.searchFolder(folder, trimmed, limit = 50).getOrThrow()
            val entities = summaries.map { summary ->
                mapSummaryToEntity(client, folder, summary, fetchBodies = false)
            }
            dao.upsertAll(entities)
            client.disconnect()
            entities.size
        }
    }

    suspend fun getMessage(folder: String, uid: Long): MailMessageEntity? {
        val cached = dao.getByFolderAndUid(folder, uid)
        if (cached != null && cached.body.isNotBlank()) {
            dao.markRead(folder, uid)
            return cached.copy(isRead = true)
        }
        val account = accountStore.getAccount() ?: return cached
        val password = accountStore.getPassword() ?: return cached
        val client = ImapClientFactory.create()
        return runCatching {
            client.connect(account, password).getOrThrow()
            val summaries = client.fetchFolder(folder, limit = 200).getOrThrow()
            val summary = summaries.firstOrNull { it.uid == uid }
            if (summary == null) {
                client.disconnect()
                return@runCatching cached
            }
            val entity = mapSummaryToEntity(
                client = client,
                folder = folder,
                summary = summary,
                fetchBodies = true,
                existing = cached,
            ).copy(isRead = true)
            client.disconnect()
            dao.upsert(entity)
            entity
        }.getOrDefault(cached)
    }

    private suspend fun mapSummaryToEntity(
        client: org.freedomsuite.protocol.imap.ImapClient,
        folder: String,
        summary: MailMessageSummary,
        fetchBodies: Boolean,
        existing: MailMessageEntity? = null,
    ): MailMessageEntity {
        val raw = if (fetchBodies) client.fetchRawMessage(folder, summary.uid).getOrDefault("") else ""
        val parsed = if (raw.isNotBlank()) MimeParser.parse(raw) else null
        val body = if (fetchBodies) {
            parsed?.plainBody?.takeIf { it.isNotBlank() }
                ?: client.fetchMessageBody(folder, summary.uid).getOrDefault(existing?.body.orEmpty())
        } else {
            existing?.body.orEmpty()
        }
        val invite = parsed?.calendarParts
            ?.mapNotNull { IcalParser.parseInvite(it) }
            ?.firstOrNull { it.method == InviteMethod.REQUEST || it.method == InviteMethod.PUBLISH }
        val inviteStatus = when {
            invite == null -> existing?.inviteStatus ?: InviteStatus.NONE.name
            existing?.inviteStatus == InviteStatus.ACCEPTED.name -> InviteStatus.ACCEPTED.name
            existing?.inviteStatus == InviteStatus.TENTATIVE.name -> InviteStatus.TENTATIVE.name
            existing?.inviteStatus == InviteStatus.DECLINED.name -> InviteStatus.DECLINED.name
            else -> InviteStatus.PENDING.name
        }
        return MailMessageEntity(
            folder = folder,
            uid = summary.uid,
            subject = summary.subject,
            from = summary.from,
            body = body,
            dateEpochMs = summary.dateEpochMs,
            snippet = summary.snippet,
            isRead = existing?.isRead ?: false,
            hasCalendarInvite = invite != null,
            inviteUid = invite?.event?.uid,
            inviteTitle = invite?.event?.title,
            inviteStartEpochMs = invite?.event?.startEpochMs,
            inviteEndEpochMs = invite?.event?.endEpochMs,
            inviteOrganizer = invite?.event?.organizer ?: summary.from,
            inviteRawIcs = invite?.rawIcs,
            inviteStatus = inviteStatus,
        )
    }

    suspend fun respondToInvite(
        folder: String,
        uid: Long,
        response: InviteResponseStatus,
    ): Result<Unit> {
        val message = dao.getByFolderAndUid(folder, uid)
            ?: return Result.failure(IllegalStateException("Message not found"))
        if (!message.hasCalendarInvite || message.inviteRawIcs == null) {
            return Result.failure(IllegalStateException("No calendar invite on this message"))
        }
        val invite = IcalParser.parseInvite(message.inviteRawIcs)
            ?: return Result.failure(IllegalStateException("Could not parse invite"))
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val identity = account.identities.firstOrNull { it.isDefault }
            ?: EmailIdentity(account.email, "Primary", isDefault = true)
        val organizer = message.inviteOrganizer ?: message.from
        val replyIcs = IcalParser.buildReply(invite, account.email, response)
        val statusLabel = when (response) {
            InviteResponseStatus.ACCEPTED -> "Accepted"
            InviteResponseStatus.TENTATIVE -> "Tentative"
            InviteResponseStatus.DECLINED -> "Declined"
            else -> "Updated"
        }
        val smtp = SmtpClientFactory.create(account)
        smtp.send(
            OutgoingMessage(
                from = identity,
                to = listOf(organizer),
                subject = "Re: ${message.subject}",
                bodyText = "$statusLabel: ${message.inviteTitle ?: message.subject}",
                calendarReplyIcs = replyIcs,
            ),
            password,
        ).getOrThrow()

        if (response == InviteResponseStatus.ACCEPTED || response == InviteResponseStatus.TENTATIVE) {
            if (calendarBridge.isCalendarInstalled()) {
                val bridgeStatus = when (response) {
                    InviteResponseStatus.ACCEPTED -> CalendarBridge.ResponseStatus.ACCEPTED
                    InviteResponseStatus.TENTATIVE -> CalendarBridge.ResponseStatus.TENTATIVE
                    else -> CalendarBridge.ResponseStatus.DECLINED
                }
                calendarBridge.insertEmailEvent(
                    uid = message.inviteUid ?: invite.event.uid,
                    title = message.inviteTitle ?: invite.event.title,
                    description = invite.event.description,
                    location = invite.event.location,
                    startEpochMs = message.inviteStartEpochMs ?: invite.event.startEpochMs,
                    endEpochMs = message.inviteEndEpochMs ?: invite.event.endEpochMs,
                    responseStatus = bridgeStatus,
                    organizer = organizer,
                    sourceMailUid = uid,
                    rawInviteIcs = message.inviteRawIcs,
                ).getOrThrow()
            }
        }

        val inviteStatus = when (response) {
            InviteResponseStatus.ACCEPTED -> InviteStatus.ACCEPTED
            InviteResponseStatus.TENTATIVE -> InviteStatus.TENTATIVE
            InviteResponseStatus.DECLINED -> InviteStatus.DECLINED
            else -> InviteStatus.PENDING
        }
        dao.upsert(message.copy(inviteStatus = inviteStatus.name))
        return Result.success(Unit)
    }

    suspend fun archiveMessage(folder: String, uid: Long): Result<Unit> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = ImapClientFactory.create()
        return runCatching {
            client.connect(account, password).getOrThrow()
            client.moveToArchive(folder, uid).getOrThrow()
            client.disconnect()
            dao.deleteByFolderAndUid(folder, uid)
        }
    }

    suspend fun sendMessage(to: String, subject: String, body: String): Result<Unit> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val identity = account.identities.firstOrNull { it.isDefault }
            ?: EmailIdentity(account.email, "Primary", isDefault = true)
        val client = SmtpClientFactory.create(account)
        return client.send(
            OutgoingMessage(
                from = identity,
                to = listOf(to.trim()),
                subject = subject.trim(),
                bodyText = body,
            ),
            password,
        )
    }

    fun signOut() {
        accountStore.clearAccount()
    }
}
