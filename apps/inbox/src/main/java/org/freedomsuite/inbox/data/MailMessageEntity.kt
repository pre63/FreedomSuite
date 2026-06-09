package org.freedomsuite.inbox.data

import androidx.room.Entity
import org.freedomsuite.inbox.spam.SpamVerdict

enum class InviteStatus {
    NONE,
    PENDING,
    ACCEPTED,
    TENTATIVE,
    DECLINED,
}

@Entity(
    tableName = "messages",
    primaryKeys = ["folder", "uid"],
)
data class MailMessageEntity(
    val folder: String,
    val uid: Long,
    val subject: String,
    val from: String,
    val body: String,
    val dateEpochMs: Long,
    val snippet: String,
    val isRead: Boolean = false,
    val hasCalendarInvite: Boolean = false,
    val inviteUid: String? = null,
    val inviteTitle: String? = null,
    val inviteStartEpochMs: Long? = null,
    val inviteEndEpochMs: Long? = null,
    val inviteOrganizer: String? = null,
    val inviteRawIcs: String? = null,
    val inviteStatus: String = InviteStatus.NONE.name,
    val spamScore: Int = 0,
    val spamVerdict: String = SpamVerdict.HAM.name,
    val spamReasons: String = "",
)
