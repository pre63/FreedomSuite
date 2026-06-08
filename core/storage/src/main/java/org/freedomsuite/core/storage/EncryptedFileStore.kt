package org.freedomsuite.core.storage

import org.freedomsuite.core.crypto.FileEncryption
import java.io.File

/**
 * Single entry point for writing sensitive files to app storage.
 * Plaintext [File.writeBytes] must not be used elsewhere in production code.
 */
object EncryptedFileStore {
    fun writeEncrypted(key: ByteArray, file: File, plaintext: ByteArray) {
        FileEncryption.writeEncrypted(key, file, plaintext)
    }

    fun readEncrypted(key: ByteArray, file: File): ByteArray {
        return FileEncryption.readEncrypted(key, file)
    }

    /** Stores a blob that is already encrypted (e.g. Freedom Sync backup ciphertext). */
    fun writeCiphertext(file: File, ciphertext: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(ciphertext)
    }

    fun readCiphertext(file: File): ByteArray {
        require(file.exists()) { "Encrypted file not found: ${file.path}" }
        return file.readBytes()
    }
}
