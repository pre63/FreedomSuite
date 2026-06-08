package org.freedomsuite.core.logging

import org.freedomsuite.core.logging.BuildConfig

/**
 * Privacy-aware logging. In release (PRIVACY_STRICT) builds, all log calls are no-ops
 * and R8 strips them from bytecode.
 */
object FreedomLog {
    inline fun v(tag: String, message: () -> String) {
        if (!BuildConfig.PRIVACY_STRICT) {
            timberLog(tag, "V", message())
        }
    }

    inline fun d(tag: String, message: () -> String) {
        if (!BuildConfig.PRIVACY_STRICT) {
            timberLog(tag, "D", message())
        }
    }

    inline fun i(tag: String, message: () -> String) {
        if (!BuildConfig.PRIVACY_STRICT) {
            timberLog(tag, "I", message())
        }
    }

    inline fun w(tag: String, message: () -> String) {
        if (!BuildConfig.PRIVACY_STRICT) {
            timberLog(tag, "W", message())
        }
    }

    inline fun e(tag: String, message: () -> String) {
        if (!BuildConfig.PRIVACY_STRICT) {
            timberLog(tag, "E", message())
        }
    }

    inline fun e(tag: String, throwable: Throwable, message: () -> String) {
        if (!BuildConfig.PRIVACY_STRICT) {
            timberLog(tag, "E", "${message()}: ${throwable.message}")
        }
    }

    @PublishedApi
    internal fun timberLog(tag: String, level: String, message: String) {
        // Reflection-free Timber access so release builds compile without Timber on classpath
        try {
            val forest = Class.forName("timber.log.Timber\$Forest")
            val redacted = LogRedactor.redact(message)
            when (level) {
                "V" -> forest.getMethod("v", String::class.java, String::class.java, Array<Any>::class.java)
                    .invoke(null, tag, redacted, emptyArray<Any>())
                "D" -> forest.getMethod("d", String::class.java, String::class.java, Array<Any>::class.java)
                    .invoke(null, tag, redacted, emptyArray<Any>())
                "I" -> forest.getMethod("i", String::class.java, String::class.java, Array<Any>::class.java)
                    .invoke(null, tag, redacted, emptyArray<Any>())
                "W" -> forest.getMethod("w", String::class.java, String::class.java, Array<Any>::class.java)
                    .invoke(null, tag, redacted, emptyArray<Any>())
                "E" -> forest.getMethod("e", String::class.java, String::class.java, Array<Any>::class.java)
                    .invoke(null, tag, redacted, emptyArray<Any>())
            }
        } catch (_: ClassNotFoundException) {
            // Timber not on classpath (release) — no-op
        }
    }
}
