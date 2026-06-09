package org.freedomsuite.inbox.spam

object SpamFolderNames {
    private val SPAM_LEAF_NAMES = setOf(
        "spam", "junk", "junk e-mail", "bulk mail", "bulk", "[gmail]/spam",
    )

    fun isSpamFolder(folder: String): Boolean {
        val leaf = folder.substringAfterLast('/').trim().lowercase()
        return leaf in SPAM_LEAF_NAMES || leaf.contains("spam") || leaf.contains("junk")
    }

    fun resolveSpamFolder(folders: List<String>): String? =
        folders.firstOrNull { isSpamFolder(it) }
            ?: folders.firstOrNull { it.equals("Spam", ignoreCase = true) }
            ?: "Spam"
}
