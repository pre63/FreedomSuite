package org.freedomsuite.testing.audit

import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Ensures outbound HTTP is centralized in [org.freedomsuite.core.network.PrivacyHttpClient].
 */
class NetworkEgressAuditTest {
    private val projectRoot: Path =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
            .toPath()

    private val scanDirs = listOf("apps", "core", "protocol", "sync")

    private val allowedOkHttpBuilderFiles = setOf(
        "core/network/src/main/java/org/freedomsuite/core/network/PrivacyHttpClient.kt",
    )

    @Test
    fun noDirectOkHttpClientConstructionOutsidePrivacyHttpClient() {
        val violations = mutableListOf<String>()
        forEachKotlinSource { relative, content ->
            if (relative in allowedOkHttpBuilderFiles) return@forEachKotlinSource
            if (content.contains("OkHttpClient.Builder")) {
                violations += "$relative constructs OkHttpClient.Builder outside PrivacyHttpClient"
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    @Test
    fun noHttpUrlConnectionOrRetrofit() {
        val banned = listOf(
            Regex("""import\s+java\.net\.HttpURLConnection"""),
            Regex("""import\s+retrofit2"""),
            Regex("""import\s+io\.ktor"""),
        )
        val violations = mutableListOf<String>()
        forEachKotlinSource { relative, content ->
            banned.forEach { pattern ->
                if (pattern.containsMatchIn(content)) {
                    violations += "$relative matches ${pattern.pattern}"
                }
            }
        }
        if (violations.isNotEmpty()) {
            fail(violations.joinToString("\n"))
        }
    }

    @Test
    fun privacyHttpClientBlocksTelemetryHosts() {
        val path = projectRoot.resolve(
            "core/network/src/main/java/org/freedomsuite/core/network/PrivacyHttpClient.kt",
        )
        val content = path.readText()
        val required = listOf(
            "google-analytics.com",
            "firebaseio.com",
            "sentry.io",
            "mixpanel.com",
            "amplitude.com",
            "segment.io",
        )
        val missing = required.filter { !content.contains(it) }
        if (missing.isNotEmpty()) {
            fail("PrivacyHttpClient missing blocked hosts: ${missing.joinToString()}")
        }
    }

    private fun forEachKotlinSource(block: (relative: String, content: String) -> Unit) {
        scanDirs.forEach { dir ->
            val root = projectRoot.resolve(dir)
            if (!Files.exists(root)) return@forEach
            Files.walk(root).use { paths ->
                paths.filter { it.toString().endsWith(".kt") }
                    .filter { !it.toString().contains("/build/") }
                    .filter { !it.toString().contains("/testing/") }
                    .forEach { path ->
                        val relative = projectRoot.relativize(path).toString().replace('\\', '/')
                        block(relative, path.readText())
                    }
            }
        }
    }
}
