package org.freedomsuite.calendar.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.freedomsuite.calendar.data.CalendarRepository
import org.freedomsuite.core.calendarapi.CalendarBridge

class CalendarEventProvider : ContentProvider() {
    private lateinit var repository: CalendarRepository

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        repository = CalendarRepository(ctx.applicationContext)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/event"

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != EVENTS) return null
        val data = values ?: return null
        val uid = data.getAsString(CalendarBridge.Columns.UID) ?: return null
        val title = data.getAsString(CalendarBridge.Columns.TITLE) ?: return null
        val start = data.getAsLong(CalendarBridge.Columns.START_EPOCH_MS) ?: return null
        val end = data.getAsLong(CalendarBridge.Columns.END_EPOCH_MS) ?: return null
        val responseStatus = data.getAsString(CalendarBridge.Columns.RESPONSE_STATUS)
            ?: CalendarBridge.ResponseStatus.ACCEPTED
        val sourceMailUid = data.getAsLong(CalendarBridge.Columns.SOURCE_MAIL_UID) ?: return null
        val rawIcs = data.getAsString(CalendarBridge.Columns.RAW_INVITE_ICS) ?: return null
        runBlocking {
            repository.importEmailEvent(
                uid = uid,
                title = title,
                description = data.getAsString(CalendarBridge.Columns.DESCRIPTION),
                location = data.getAsString(CalendarBridge.Columns.LOCATION),
                startEpochMs = start,
                endEpochMs = end,
                responseStatus = responseStatus,
                organizer = data.getAsString(CalendarBridge.Columns.ORGANIZER),
                sourceMailUid = sourceMailUid,
                rawInviteIcs = rawIcs,
            ).getOrThrow()
        }
        return Uri.withAppendedPath(uri, uid)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val EVENTS = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(CalendarBridge.AUTHORITY, "events", EVENTS)
        }
    }
}
