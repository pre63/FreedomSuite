use crate::SyncError;

/// Encrypted sync manifest — lists blob references and vector clocks.
#[derive(Debug, Clone, Default)]
pub struct SyncManifest {
    pub version: u64,
    pub device_ids: Vec<String>,
    pub record_count: usize,
}

impl SyncManifest {
    pub fn new(device_id: &str) -> Self {
        Self {
            version: 1,
            device_ids: vec![device_id.to_string()],
            record_count: 0,
        }
    }

    pub fn encrypt_placeholder(&self) -> Result<Vec<u8>, SyncError> {
        // Full ML-KEM + AES-GCM implementation in Phase 0b
        let json = format!(
            r#"{{"version":{},"devices":{},"records":{}}}"#,
            self.version,
            self.device_ids.len(),
            self.record_count,
        );
        Ok(json.into_bytes())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn manifest_serializes() {
        let m = SyncManifest::new("device-a");
        assert!(m.encrypt_placeholder().unwrap().len() > 0);
    }
}
