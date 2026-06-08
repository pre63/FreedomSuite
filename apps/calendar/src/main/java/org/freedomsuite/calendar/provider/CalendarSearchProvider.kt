package org.freedomsuite.calendar.provider

import android.content.ContentProvider
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.freedomsuite.calendar.data.CalendarDatabase
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchCursor
import org.freedomsuite.core.searchapi.SearchHit
import org.freedomsuite.core.searchapi.SearchQueryExpander

class CalendarSearchProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uriMatcher.match(uri) != RESULTS) return null
        val query = uri.getQueryParameter("q") ?: return null
        val ctx = context?.applicationContext ?: return null
        val dao = CalendarDatabase.getInstance(ctx).calendarDao()
        val terms = SearchQueryExpander.expand(query)
        val seen = mutableSetOf<String>()
        val hits = mutableListOf<SearchHit>()

        runBlocking {
            terms.forEach { term ->
                dao.searchEvents(term).forEach { event ->
                    if (!seen.add(event.uid)) return@forEach
                    hits += SearchHit(
                        id = event.uid,
                        source = SearchBridge.Source.CALENDAR,
                        title = event.title,
                        subtitle = event.location.orEmpty(),
                        snippet = event.description?.take(120).orEmpty(),
                        timestampMs = event.startEpochMs,
                        packageName = SearchBridge.Source.CALENDAR.packageName,
                        payload = event.uid,
                    )
                }
            }
        }
        return SearchCursor.fromHits(hits.sortedByDescending { it.timestampMs })
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/search_result"

    override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val RESULTS = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(SearchBridge.Source.CALENDAR.authority, "results", RESULTS)
        }
    }
}
