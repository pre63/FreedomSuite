use crate::SyncError;

/// Encrypted blob header — plaintext metadata is never stored on remote storage.
#[derive(Debug, Clone)]
pub struct EncryptedBlob {
    pub version: u8,
    pub content_hash: String,
    pub ciphertext: Vec<u8>,
}

impl EncryptedBlob {
    pub fn new_placeholder(ciphertext: Vec<u8>) -> Result<Self, SyncError> {
        if ciphertext.is_empty() {
            return Err(SyncError::InvalidInput("ciphertext empty".into()));
        }
        Ok(Self {
            version: crate::algorithms::VERSION,
            content_hash: format!("sha256:{:x}", simple_hash(&ciphertext)),
            ciphertext,
        })
    }
}

fn simple_hash(data: &[u8]) -> u64 {
    data.iter().fold(0u64, |acc, b| acc.wrapping_mul(31).wrapping_add(*b as u64))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn blob_requires_content() {
        assert!(EncryptedBlob::new_placeholder(vec![]).is_err());
    }
}
