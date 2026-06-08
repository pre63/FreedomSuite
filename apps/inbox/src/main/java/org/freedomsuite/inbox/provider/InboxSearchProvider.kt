package org.freedomsuite.inbox.provider

import android.content.ContentProvider
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchCursor
import org.freedomsuite.core.searchapi.SearchHit
import org.freedomsuite.core.searchapi.SearchQueryExpander
import org.freedomsuite.inbox.data.InboxDatabase

class InboxSearchProvider : ContentProvider() {
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
        val dao = InboxDatabase.getInstance(ctx).inboxDao()
        val terms = SearchQueryExpander.expand(query)
        val seen = mutableSetOf<String>()
        val hits = mutableListOf<SearchHit>()

        runBlocking {
            terms.forEach { term ->
                dao.searchAllFolders(term).forEach { mail ->
                    val key = "${mail.folder}:${mail.uid}"
                    if (!seen.add(key)) return@forEach
                    hits += SearchHit(
                        id = key,
                        source = SearchBridge.Source.MAIL,
                        title = mail.subject.ifBlank { "(No subject)" },
                        subtitle = mail.from,
                        snippet = mail.snippet.ifBlank { mail.body.take(120) },
                        timestampMs = mail.dateEpochMs,
                        packageName = SearchBridge.Source.MAIL.packageName,
                        payload = "${mail.folder}|${mail.uid}",
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
            addURI(SearchBridge.Source.MAIL.authority, "results", RESULTS)
        }
    }
}
