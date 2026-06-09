package org.freedomsuite.inbox.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.freedomsuite.core.account.AccountStore
import org.freedomsuite.core.account.EmailIdentity
import org.freedomsuite.core.account.MailAccount
import org.freedomsuite.core.account.discovery.MailServerDiscovery
import org.freedomsuite.core.account.discovery.MailServerSettings
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
import org.freedomsuite.inbox.spam.OwnedAddresses
import org.freedomsuite.inbox.spam.SpamClassifier
import org.freedomsuite.inbox.spam.SpamFolderNames
import org.freedomsuite.inbox.spam.SpamInput
import org.freedomsuite.inbox.spam.SpamVerdict

class InboxRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = InboxDatabase.getInstance(appContext).inboxDao()
    private val accountStore = AccountStore(appContext)
    private val calendarBridge = CalendarBridgeClient(appContext)
    private val spamClassifier = SpamClassifier()

    fun hasAccount(): Boolean = accountStore.hasAccount()

    fun observeFolder(folder: String): Flow<List<MailMessageEntity>> = dao.observeFolder(folder)

    fun observeFolderSearch(folder: String, query: String): Flow<List<MailMessageEntity>> =
        dao.observeFolderSearch(folder, query)

    fun isCalendarInstalled(): Boolean = calendarBridge.isCalendarInstalled()

    suspend fun configureAccount(
        email: String,
        password: String,
        manualSettings: MailServerSettings? = null,
    ): Result<Unit> {
        val trimmedEmail = email.trim()
        require(trimmedEmail.contains("@")) { "Invalid email address" }

        val passwordChars = password.toCharArray()
        val candidates = manualSettings?.let { listOf(it) }
            ?: MailServerDiscovery(appContext).discover(trimmedEmail)

        if (candidates.isEmpty()) {
            return Result.failure(
                IllegalStateException("Could not discover mail servers for this domain. Enter settings manually."),
            )
        }

        val errors = mutableListOf<String>()
        for (settings in candidates) {
            val account = accountStore.accountFromSettings(trimmedEmail, settings)
            val client = imapClient(account)
            val connectResult = runCatching {
                client.connect(account, passwordChars).getOrThrow()
                client.disconnect()
            }
            if (connectResult.isSuccess) {
                accountStore.saveAccount(account, passwordChars)
                return Result.success(Unit)
            }
            errors += "${settings.label} (${settings.imapHost}): ${connectResult.exceptionOrNull()?.message}"
        }

        return Result.failure(
            IllegalStateException(
                "Could not connect with discovered settings. Check email, app password, or enter server details manually.\n" +
                    errors.take(3).joinToString("\n"),
            ),
        )
    }

    suspend fun listFolders(): Result<List<String>> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = imapClient(account)
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
        val client = imapClient(account)
        return runCatching {
            client.connect(account, password).getOrThrow()
            val summaries = client.fetchFolder(folder, limit = 50).getOrThrow()
            val isInbox = folder.equals("INBOX", ignoreCase = true)
            val entities = mutableListOf<MailMessageEntity>()
            for (summary in summaries) {
                val entity = mapSummaryToEntity(
                    client = client,
                    folder = folder,
                    summary = summary,
                    fetchBodies = true,
                    account = account,
                )
                if (isInbox && entity.spamVerdict == SpamVerdict.SPAM.name) {
                    client.moveToSpam(folder, entity.uid).getOrNull()
                    continue
                }
                entities += entity
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
        val client = imapClient(account)
        return runCatching {
            client.connect(account, password).getOrThrow()
            val summaries = client.searchFolder(folder, trimmed, limit = 50).getOrThrow()
            val entities = summaries.map { summary ->
                mapSummaryToEntity(
                    client = client,
                    folder = folder,
                    summary = summary,
                    fetchBodies = false,
                    account = account,
                )
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
        val client = imapClient(account)
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
                account = account,
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
        account: MailAccount? = null,
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
        val spam = classifySpam(
            account = account ?: accountStore.getAccount(),
            subject = summary.subject,
            from = summary.from,
            body = body,
            headers = parsed?.headers.orEmpty(),
            hasCalendarInvite = invite != null,
        )
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
            spamScore = spam?.score ?: existing?.spamScore ?: 0,
            spamVerdict = spam?.verdict?.name ?: existing?.spamVerdict ?: SpamVerdict.HAM.name,
            spamReasons = spam?.reasons ?: existing?.spamReasons.orEmpty(),
        )
    }

    private fun classifySpam(
        account: MailAccount?,
        subject: String,
        from: String,
        body: String,
        headers: Map<String, List<String>>,
        hasCalendarInvite: Boolean,
    ) = account?.let { mailAccount ->
        spamClassifier.classify(
            SpamInput(
                subject = subject,
                from = from,
                body = body,
                headers = headers,
                owned = OwnedAddresses.fromAccount(
                    mailAccount.email,
                    mailAccount.aliases,
                    mailAccount.ownedDomains,
                ),
                hasCalendarInvite = hasCalendarInvite,
            ),
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
        val smtp = smtpClient(account)
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

    suspend fun reportSpam(folder: String, uid: Long): Result<Unit> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = imapClient(account)
        return runCatching {
            client.connect(account, password).getOrThrow()
            client.moveToSpam(folder, uid).getOrThrow()
            client.disconnect()
            dao.deleteByFolderAndUid(folder, uid)
        }
    }

    suspend fun markNotSpam(folder: String, uid: Long): Result<Unit> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = imapClient(account)
        return runCatching {
            client.connect(account, password).getOrThrow()
            client.moveToInbox(folder, uid).getOrThrow()
            client.disconnect()
            dao.deleteByFolderAndUid(folder, uid)
        }
    }

    fun isSpamFolder(folder: String): Boolean = SpamFolderNames.isSpamFolder(folder)

    suspend fun archiveMessage(folder: String, uid: Long): Result<Unit> {
        val account = accountStore.getAccount() ?: return Result.failure(IllegalStateException("No account"))
        val password = accountStore.getPassword() ?: return Result.failure(IllegalStateException("No password"))
        val client = imapClient(account)
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
        return smtpClient(account).send(
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

    private fun imapClient(account: MailAccount) =
        ImapClientFactory.create(plainText = account.plainText)

    private fun smtpClient(account: MailAccount) =
        SmtpClientFactory.create(account, plainText = account.plainText)
}
