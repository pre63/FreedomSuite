package org.freedomsuite.files.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FolderKind {
    PHOTOS,
    DOCUMENTS,
    CUSTOM,
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "file_items")
data class FileItemEntity(
    @PrimaryKey val id: String,
    val folderId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val createdAtEpochMs: Long,
    val isImage: Boolean,
)

@Entity(tableName = "image_analysis")
data class ImageAnalysisEntity(
    @PrimaryKey val fileId: String,
    val objectLabels: String,
    val ocrText: String,
    val faceEmbeddingsJson: String,
    val searchBlob: String,
    val indexedAtEpochMs: Long,
)

data class FolderWithCount(
    val id: String,
    val name: String,
    val kind: String,
    val createdAtEpochMs: Long,
    val fileCount: Int,
)
