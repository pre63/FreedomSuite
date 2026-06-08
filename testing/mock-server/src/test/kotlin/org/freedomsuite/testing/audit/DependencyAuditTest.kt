package org.freedomsuite.testing.audit

import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Static audit: fails CI if banned analytics/Google SDK dependencies are declared.
 */
class DependencyAuditTest {
    private val projectRoot: Path =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
            .toPath()

    private val bannedPatterns = listOf(
        Regex("""com\.google\.firebase"""),
        Regex("""com\.google\.android\.gms"""),
        Regex("""play-services"""),
        Regex("""firebase-analytics"""),
        Regex("""crashlytics"""),
        Regex("""io\.sentry"""),
        Regex("""com\.bugsnag"""),
        Regex("""com\.datadog"""),
        Regex("""com\.mixpanel"""),
        Regex("""com\.amplitude"""),
        Regex("""com\.facebook\.android"""),
        Regex("""com\.adjust"""),
        Regex("""com\.appsflyer"""),
        Regex("""com\.google\.android\.play\.integrity"""),
        Regex("""androidx\.work:work-runtime"""),
        Regex("""com\.google\.mlkit"""),
    )

    private val scanFiles = listOf(
        "gradle/libs.versions.toml",
    )

    private val scanDirs = listOf("apps", "core", "protocol", "sync")

    @Test
    fun noBannedDependenciesInVersionCatalogOrGradle() {
        val violations = mutableListOf<String>()
        scanFiles.forEach { relative ->
            val path = projectRoot.resolve(relative)
            if (Files.exists(path)) {
                checkText(relative, path.readText(), violations)
            }
        }
        scanDirs.forEach { dir ->
            val root = projectRoot.resolve(dir)
            if (!Files.exists(root)) return@forEach
            Files.walk(root).use { paths ->
                paths.filter { path -> isGradleFile(path) }
                    .forEach { path ->
                        val relative = projectRoot.relativize(path).toString().replace('\\', '/')
                        val text = runCatching { path.readText() }.getOrNull() ?: return@forEach
                        checkText(relative, text, violations)
                    }
            }
        }
        if (violations.isNotEmpty()) {
            fail("Banned third-party dependencies found:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun noAnalyticsOrCrashReportingInSource() {
        val bannedSource = listOf(
            Regex("""import\s+com\.google\.firebase"""),
            Regex("""import\s+io\.sentry"""),
            Regex("""import\s+com\.bugsnag"""),
            Regex("""FirebaseAnalytics"""),
            Regex("""Crashlytics"""),
            Regex("""Sentry\."""),
        )
        val violations = mutableListOf<String>()
        scanDirs.forEach { dir ->
            val root = projectRoot.resolve(dir)
            if (!Files.exists(root)) return@forEach
            Files.walk(root).use { paths ->
                paths.filter { path -> isSourceFile(path) }
                    .forEach { path ->
                        val relative = projectRoot.relativize(path).toString().replace('\\', '/')
                        val text = runCatching { path.readText() }.getOrNull() ?: return@forEach
                        bannedSource.forEach { pattern ->
                            if (pattern.containsMatchIn(text)) {
                                violations += "$relative matches ${pattern.pattern}"
                            }
                        }
                    }
            }
        }
        if (violations.isNotEmpty()) {
            fail("Banned analytics/crash imports in source:\n${violations.joinToString("\n")}")
        }
    }

    private fun isSourceFile(path: Path): Boolean {
        val normalized = path.toString().replace('\\', '/')
        return normalized.endsWith(".kt") &&
            !normalized.contains("/build/") &&
            !normalized.contains("/.gradle/")
    }

    private fun isGradleFile(path: Path): Boolean {
        val normalized = path.toString().replace('\\', '/')
        return normalized.endsWith(".gradle.kts") &&
            !normalized.contains("/build/") &&
            !normalized.contains("/.gradle/")
    }

    private fun checkText(relative: String, text: String, violations: MutableList<String>) {
        bannedPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(text)) {
                violations += "$relative matches ${pattern.pattern}"
            }
        }
    }
}
