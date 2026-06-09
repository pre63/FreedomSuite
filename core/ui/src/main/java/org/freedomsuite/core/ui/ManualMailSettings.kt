package org.freedomsuite.core.ui

data class ManualMailSettings(
    val imapHost: String,
    val imapPort: Int,
    val smtpHost: String,
    val smtpPort: Int,
)
