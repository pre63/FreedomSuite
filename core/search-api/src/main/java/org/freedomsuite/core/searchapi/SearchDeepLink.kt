package org.freedomsuite.core.searchapi

import android.content.Intent

/**
 * Parsed navigation target from [UnifiedSearchClient.openHit] launch intent extras.
 */
data class SearchDeepLink(
    val source: SearchBridge.Source,
    val hitId: String,
    val mailFolder: String? = null,
    val mailUid: Long? = null,
) {
    fun mailRoute(): String? {
        val uid = mailUid ?: return null
        if (uid < 0) return null
        return "message/$uid"
    }

    fun fileRoute(): String = "file/$hitId"

    fun calendarRoute(): String = "event/$hitId"

    fun messageRoute(): String = "channel/$hitId"
}

object SearchDeepLinkHandler {
    fun parse(intent: Intent?): SearchDeepLink? {
        if (intent == null) return null
        val sourceId = intent.getStringExtra(SearchBridge.EXTRA_HIT_SOURCE) ?: return null
        val hitId = intent.getStringExtra(SearchBridge.EXTRA_HIT_ID) ?: return null
        val source = SearchBridge.Source.entries.firstOrNull { it.id == sourceId } ?: return null
        val mailFolder = intent.getStringExtra(SearchBridge.EXTRA_MAIL_FOLDER)
        val mailUid = intent.getLongExtra(SearchBridge.EXTRA_MAIL_UID, -1L).takeIf { it >= 0 }
        return SearchDeepLink(
            source = source,
            hitId = hitId,
            mailFolder = mailFolder,
            mailUid = mailUid,
        )
    }

    fun consume(intent: Intent?) {
        intent ?: return
        intent.removeExtra(SearchBridge.EXTRA_HIT_SOURCE)
        intent.removeExtra(SearchBridge.EXTRA_HIT_ID)
        intent.removeExtra(SearchBridge.EXTRA_MAIL_FOLDER)
        intent.removeExtra(SearchBridge.EXTRA_MAIL_UID)
    }
}
