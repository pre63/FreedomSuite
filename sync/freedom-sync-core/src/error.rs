use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SyncError {
    InvalidInput(String),
    Crypto(String),
    NotImplemented(String),
}

impl fmt::Display for SyncError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            SyncError::InvalidInput(msg) => write!(f, "invalid input: {msg}"),
            SyncError::Crypto(msg) => write!(f, "crypto error: {msg}"),
            SyncError::NotImplemented(msg) => write!(f, "not implemented: {msg}"),
        }
    }
}

impl std::error::Error for SyncError {}
