package org.freedomsuite.core.logging

import kotlin.system.exitProcess

internal class FreedomUncaughtExceptionHandler(
    private val previous: Thread.UncaughtExceptionHandler?,
    private val strict: Boolean,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (strict) {
            SecureMemory.wipeAll()
            exitProcess(1)
        } else {
            previous?.uncaughtException(thread, throwable)
        }
    }
}
