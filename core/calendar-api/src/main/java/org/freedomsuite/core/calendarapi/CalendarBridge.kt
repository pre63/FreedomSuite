package org.freedomsuite.core.calendarapi

import android.content.ContentValues
import android.content.Context
import android.net.Uri

object CalendarBridge {
    const val AUTHORITY = "org.freedomsuite.calendar.events"
    const val PERMISSION = "org.freedomsuite.permission.CALENDAR_BRIDGE"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/events")

    object Columns {
        const val UID = "uid"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val LOCATION = "location"
        const val START_EPOCH_MS = "start_epoch_ms"
        const val END_EPOCH_MS = "end_epoch_ms"
        const val RESPONSE_STATUS = "response_status"
        const val ORGANIZER = "organizer"
        const val SOURCE_MAIL_UID = "source_mail_uid"
        const val RAW_INVITE_ICS = "raw_invite_ics"
    }

    object ResponseStatus {
        const val ACCEPTED = "ACCEPTED"
        const val TENTATIVE = "TENTATIVE"
        const val DECLINED = "DECLINED"
        const val NEEDS_ACTION = "NEEDS_ACTION"
        const val NONE = "NONE"
    }
}

class CalendarBridgeClient(private val context: Context) {
    fun isCalendarInstalled(): Boolean = runCatching {
        context.packageManager.resolveContentProvider(CalendarBridge.AUTHORITY, 0) != null
    }.getOrDefault(false)

    fun insertEmailEvent(
        uid: String,
        title: String,
        description: String?,
        location: String?,
        startEpochMs: Long,
        endEpochMs: Long,
        responseStatus: String,
        organizer: String?,
        sourceMailUid: Long,
        rawInviteIcs: String,
    ): Result<Unit> = runCatching {
        val values = ContentValues().apply {
            put(CalendarBridge.Columns.UID, uid)
            put(CalendarBridge.Columns.TITLE, title)
            put(CalendarBridge.Columns.DESCRIPTION, description)
            put(CalendarBridge.Columns.LOCATION, location)
            put(CalendarBridge.Columns.START_EPOCH_MS, startEpochMs)
            put(CalendarBridge.Columns.END_EPOCH_MS, endEpochMs)
            put(CalendarBridge.Columns.RESPONSE_STATUS, responseStatus)
            put(CalendarBridge.Columns.ORGANIZER, organizer)
            put(CalendarBridge.Columns.SOURCE_MAIL_UID, sourceMailUid)
            put(CalendarBridge.Columns.RAW_INVITE_ICS, rawInviteIcs)
        }
        val uri = context.contentResolver.insert(CalendarBridge.CONTENT_URI, values)
        require(uri != null) { "Calendar app rejected event insert" }
    }
}
