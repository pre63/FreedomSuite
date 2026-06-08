package org.freedomsuite.testing.audit

import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Static audit: fails CI if production code introduces unencrypted local storage.
 */
class StorageAuditTest {
    private val projectRoot: Path =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
            .toPath()

    private val scanRoots = listOf("apps", "core", "sync")
    private val allowedWriteBytes = setOf(
        "core/crypto/src/main/java/org/freedomsuite/core/crypto/FileEncryption.kt",
        "core/storage/src/main/java/org/freedomsuite/core/storage/EncryptedFileStore.kt",
    )

    @Test
    fun noPlainSharedPreferences() {
        assertNoMatches(
            pattern = Regex("""getSharedPreferences\s*\("""),
            message = "Use SecurePreferences instead of plain SharedPreferences",
        )
    }

    @Test
    fun noPreferenceManager() {
        assertNoMatches(
            pattern = Regex("""PreferenceManager"""),
            message = "Use SecurePreferences instead of PreferenceManager",
        )
    }

    @Test
    fun noDataStore() {
        assertNoMatches(
            pattern = Regex("""androidx\.datastore"""),
            message = "DataStore is not approved; use SecurePreferences for secrets",
        )
    }

    @Test
    fun roomDatabasesMustUseSqlCipher() {
        val violations = mutableListOf<String>()
        forEachKotlinSource { relative, content ->
            if (!content.contains("Room.databaseBuilder")) return@forEachKotlinSource
            val usesEncryptedRoom = content.contains("EncryptedRoom.build")
            val usesOpenHelper = content.contains("openHelperFactory")
            if (!usesEncryptedRoom && !usesOpenHelper) {
                violations += "$relative: Room.databaseBuilder without SQLCipher"
            }
        }
        if (violations.isNotEmpty()) {
            fail("Unencrypted Room databases detected:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun sensitiveFilesMustUseEncryptedFileStore() {
        val violations = mutableListOf<String>()
        forEachKotlinSource { relative, content ->
            if (relative in allowedWriteBytes) return@forEachKotlinSource
            if (relative.startsWith("testing/")) return@forEachKotlinSource
            if (content.contains(".writeBytes(") && content.contains("File(")) {
                violations += "$relative: use EncryptedFileStore instead of File.writeBytes"
            }
        }
        if (violations.isNotEmpty()) {
            fail("Unencrypted file writes detected:\n${violations.joinToString("\n")}")
        }
    }

    private fun assertNoMatches(pattern: Regex, message: String) {
        val violations = mutableListOf<String>()
        forEachKotlinSource { relative, content ->
            if (pattern.containsMatchIn(content)) {
                violations += "$relative"
            }
        }
        if (violations.isNotEmpty()) {
            fail("$message in:\n${violations.joinToString("\n")}")
        }
    }

    private fun forEachKotlinSource(block: (relative: String, content: String) -> Unit) {
        scanRoots.forEach { root ->
            val dir = projectRoot.resolve(root)
            if (!Files.exists(dir)) return@forEach
            Files.walk(dir).use { paths ->
                paths.filter { it.toString().endsWith(".kt") }
                    .filter { !it.toString().contains("/build/") }
                    .forEach { path ->
                        val relative = projectRoot.relativize(path).toString().replace('\\', '/')
                        block(relative, path.readText())
                    }
            }
        }
    }
}
