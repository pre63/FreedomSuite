package org.freedomsuite.core.logging

import java.util.regex.Pattern

/**
 * Redacts sensitive patterns from dev-mode log output.
 */
object LogRedactor {
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    )
    private val apiKeyPattern = Pattern.compile(
        "(api[_-]?key|token|password|secret)[=:\\s]+\\S+",
        Pattern.CASE_INSENSITIVE
    )

    fun redact(input: String): String {
        var result = emailPattern.matcher(input).replaceAll("[email]")
        result = apiKeyPattern.matcher(result).replaceAll("$1=[REDACTED]")
        return result
    }
}
