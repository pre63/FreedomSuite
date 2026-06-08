package org.freedomsuite.core.logging

/**
 * Best-effort wipe of sensitive in-memory caches on fatal exit.
 */
object SecureMemory {
    fun wipeAll() {
        // Placeholder — apps register sensitive buffers here as crypto module matures
    }
}
