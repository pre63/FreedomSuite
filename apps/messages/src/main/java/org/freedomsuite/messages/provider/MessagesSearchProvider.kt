package org.freedomsuite.messages.provider

import android.content.ContentProvider
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchCursor
import org.freedomsuite.core.searchapi.SearchHit
import org.freedomsuite.core.searchapi.SearchQueryExpander
import org.freedomsuite.messages.data.MessagesDatabase

class MessagesSearchProvider : ContentProvider() {
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
        val dao = MessagesDatabase.getInstance(ctx).messageDao()
        val terms = SearchQueryExpander.expand(query)
        val seen = mutableSetOf<String>()
        val hits = mutableListOf<SearchHit>()

        runBlocking {
            terms.forEach { term ->
                dao.searchMessages(term).forEach { message ->
                    if (!seen.add(message.id)) return@forEach
                    hits += SearchHit(
                        id = message.id,
                        source = SearchBridge.Source.MESSAGE,
                        title = message.channelName,
                        subtitle = message.authorLabel,
                        snippet = message.body.take(120),
                        timestampMs = message.createdAtEpochMs,
                        packageName = SearchBridge.Source.MESSAGE.packageName,
                        payload = "${message.channelId}|${message.id}",
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
            addURI(SearchBridge.Source.MESSAGE.authority, "results", RESULTS)
        }
    }
}
