package org.freedomsuite.testing.audit

import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Static audit: manifest permissions and privacy flags per app.
 */
class ManifestPrivacyAuditTest {
    private val projectRoot: Path =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
            .toPath()

    private val appsDir = projectRoot.resolve("apps")

    private val forbiddenPermissions = setOf(
        "android.permission.ACCESS_ADSERVICES_AD_ID",
        "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
        "android.permission.AD_ID",
        "com.google.android.gms.permission.AD_ID",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.WAKE_LOCK",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_PHONE_STATE",
        "android.permission.GET_ACCOUNTS",
    )

    private val offlineApps = setOf("keyboard", "search")

    private val networkApps = setOf("inbox", "calendar", "messages", "auth", "files", "chat")

    @Test
    fun noForbiddenPermissionsDeclared() {
        val violations = mutableListOf<String>()
        eachAppManifest { app, relative, content ->
            forbiddenPermissions.forEach { perm ->
                if (content.contains(perm)) {
                    violations += "$relative declares forbidden permission $perm"
                }
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    @Test
    fun offlineAppsMustNotDeclareInternet() {
        val violations = mutableListOf<String>()
        eachAppManifest { app, relative, content ->
            if (app !in offlineApps) return@eachAppManifest
            if (content.contains("android.permission.INTERNET")) {
                violations += "$relative must not declare INTERNET (offline-only app)"
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    @Test
    fun networkAppsRequireNetworkSecurityConfig() {
        val violations = mutableListOf<String>()
        eachAppManifest { app, relative, content ->
            if (app !in networkApps) return@eachAppManifest
            if (!content.contains("networkSecurityConfig")) {
                violations += "$relative must set android:networkSecurityConfig"
            }
            if (!content.contains("""android:allowBackup="false"""")) {
                violations += "$relative must set android:allowBackup=\"false\""
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    @Test
    fun exportedSearchProvidersRequireSignaturePermission() {
        val violations = mutableListOf<String>()
        eachAppManifest { _, relative, content ->
            if (!content.contains("SearchProvider")) return@eachAppManifest
            val blocks = content.split("<provider")
            blocks.drop(1).forEach { block ->
                if (!block.contains("SearchProvider")) return@forEach
                if (!block.contains("android:exported=\"true\"")) return@forEach
                if (!block.contains("org.freedomsuite.permission.SEARCH")) {
                    violations += "$relative: exported SearchProvider missing SEARCH permission"
                }
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    @Test
    fun startupTelemetryInitializersRemoved() {
        val violations = mutableListOf<String>()
        eachAppManifest { _, relative, content ->
            listOf(
                "EmojiCompatInitializer",
                "ProfileInstallerInitializer",
                "WorkManagerInitializer",
            ).forEach { initializer ->
                if (content.contains(initializer) && !content.contains("tools:node=\"remove\"")) {
                    // Allow if the remove is on the meta-data line in same block — coarse check:
                    val idx = content.indexOf(initializer)
                    val window = content.substring(maxOf(0, idx - 200), minOf(content.length, idx + 200))
                    if (!window.contains("tools:node=\"remove\"")) {
                        violations += "$relative: $initializer not removed via tools:node"
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    private fun eachAppManifest(block: (app: String, relative: String, content: String) -> Unit) {
        if (!Files.exists(appsDir)) return
        Files.list(appsDir).use { paths ->
            paths.filter { Files.isDirectory(it) }
                .forEach { appPath ->
                    val app = appPath.fileName.toString()
                    val manifest = appPath.resolve("src/main/AndroidManifest.xml")
                    if (!Files.exists(manifest)) return@forEach
                    val relative = projectRoot.relativize(manifest).toString().replace('\\', '/')
                    block(app, relative, manifest.readText())
                }
        }
    }
}
