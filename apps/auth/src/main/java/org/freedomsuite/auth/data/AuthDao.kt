package org.freedomsuite.auth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {
    @Query("SELECT * FROM auth_accounts ORDER BY sortOrder ASC, issuer ASC, accountName ASC")
    fun observeAll(): Flow<List<AuthAccountEntity>>

    @Query("SELECT * FROM auth_accounts ORDER BY sortOrder ASC, issuer ASC, accountName ASC")
    suspend fun getAll(): List<AuthAccountEntity>

    @Query("SELECT * FROM auth_accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AuthAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AuthAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AuthAccountEntity>)

    @Query("DELETE FROM auth_accounts WHERE id = :id")
    suspend fun deleteById(id: String)
}
