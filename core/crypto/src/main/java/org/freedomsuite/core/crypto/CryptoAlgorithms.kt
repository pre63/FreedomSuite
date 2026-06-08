package org.freedomsuite.core.crypto

/**
 * Cryptographic algorithm identifiers for crypto-agility.
 * See docs/CRYPTO.md for rationale.
 */
object CryptoAlgorithms {
    const val VERSION = 1

    const val SYMMETRIC = "AES-256-GCM"
    const val SYMMETRIC_ALT = "XChaCha20-Poly1305"
    const val KDF = "Argon2id+HKDF-SHA3-512"
    const val CLASSICAL_KEX = "X25519"
    const val PQC_KEM = "ML-KEM-768"
    const val HYBRID_KEX = "X25519+ML-KEM-768"
    const val SIGNATURE = "Ed25519"
}
