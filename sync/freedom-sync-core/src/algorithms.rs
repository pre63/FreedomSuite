//! Cryptographic algorithm identifiers (crypto-agility version 1).

pub const VERSION: u8 = 1;
pub const SYMMETRIC: &str = "AES-256-GCM";
pub const KDF: &str = "Argon2id+HKDF-SHA3-512";
pub const CLASSICAL_KEX: &str = "X25519";
pub const PQC_KEM: &str = "ML-KEM-768";
pub const HYBRID_KEX: &str = "X25519+ML-KEM-768";
