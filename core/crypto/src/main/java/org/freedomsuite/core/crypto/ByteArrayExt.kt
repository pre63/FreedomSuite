package org.freedomsuite.core.crypto

/**
 * Best-effort zeroing of sensitive byte arrays.
 */
fun ByteArray.wipe() {
    fill(0)
}
