package org.freedomsuite.auth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_accounts")
data class AuthAccountEntity(
    @PrimaryKey val id: String,
    val issuer: String,
    val accountName: String,
    val secretBase32: String,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
    val createdAtEpochMs: Long,
    val sortOrder: Int,
)
