package org.freedomsuite.files.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FilesDao {
    @Query(
        """
        SELECT f.id, f.name, f.kind, f.createdAtEpochMs, COUNT(i.id) AS fileCount
        FROM folders f
        LEFT JOIN file_items i ON i.folderId = f.id
        GROUP BY f.id
        ORDER BY f.createdAtEpochMs ASC
        """,
    )
    fun observeFoldersWithCount(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolder(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id AND kind = 'CUSTOM'")
    suspend fun deleteCustomFolder(id: String)

    @Query("SELECT * FROM file_items WHERE folderId = :folderId ORDER BY createdAtEpochMs DESC")
    fun observeFilesInFolder(folderId: String): Flow<List<FileItemEntity>>

    @Query("SELECT * FROM file_items WHERE id = :id")
    suspend fun getFile(id: String): FileItemEntity?

    @Query("SELECT * FROM file_items")
    suspend fun getAllFiles(): List<FileItemEntity>

    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFile(file: FileItemEntity)

    @Query("DELETE FROM file_items WHERE id = :id")
    suspend fun deleteFileById(id: String)

    @Query("SELECT * FROM image_analysis WHERE fileId = :fileId LIMIT 1")
    suspend fun getAnalysis(fileId: String): ImageAnalysisEntity?

    @Query("SELECT * FROM image_analysis")
    suspend fun getAllAnalysis(): List<ImageAnalysisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalysis(analysis: ImageAnalysisEntity)

    @Query("DELETE FROM image_analysis WHERE fileId = :fileId")
    suspend fun deleteAnalysis(fileId: String)

    @Query(
        """
        SELECT f.* FROM file_items f
        INNER JOIN image_analysis a ON a.fileId = f.id
        WHERE a.searchBlob LIKE '%' || :term || '%' AND f.isImage = 1
        ORDER BY f.createdAtEpochMs DESC
        """,
    )
    suspend fun searchImagesByTerm(term: String): List<FileItemEntity>
}
