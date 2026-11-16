package com.senghol.knotx.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class StoryMediaType { IMAGE, VIDEO, TEXT }
enum class StoryVisibility { PRIVATE, EVERYONE, CUSTOM }

/** Local upload lifecycle for a story you created — the server doesn't know about this. */
enum class StorySyncStatus { PENDING_UPLOAD, UPLOADING, SYNCED, FAILED }

/**
 * The story itself. Count fields (viewCount/likeCount/commentCount) and isLikedByMe are
 * denormalized copies of server truth — refreshed whenever the feed syncs — so the tray
 * and viewer UI can render instantly from Room without a join or a network round trip.
 */
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val ownerName: String? = null,
    val ownerProfile: String? = null,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val localPath: String?,
    val mediaType: StoryMediaType,
    val visibility: StoryVisibility,
    val allowedContactIds: List<String>,
    val durationMs: Long?,
    val createdAt: Long,
    val expiresAt: Long,
    @ColumnInfo(defaultValue = "0") val viewCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val likeCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val commentCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val isLikedByMe: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isStoryDownloaded: Boolean = false,
    @ColumnInfo(defaultValue = "0") val commentsPrivate: Boolean = false,
    val storyPass: String? = null,
    val syncStatus: StorySyncStatus = StorySyncStatus.SYNCED
)

@Entity(
    tableName = "story_views",
    primaryKeys = ["storyId", "viewerId"],
    indices = [Index("storyId")]
)
data class StoryViewEntity(
    val storyId: String,
    val viewerId: String,
    val viewedAt: Long
)

@Entity(
    tableName = "story_likes",
    primaryKeys = ["storyId", "userId"],
    indices = [Index("storyId")]
)
data class StoryLikeEntity(
    val storyId: String,
    val userId: String,
    val likedAt: Long
)

@Entity(tableName = "story_comments", indices = [Index("storyId")])
data class StoryCommentEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val userId: String,
    val text: String,
    val createdAt: Long,
    // true until the server confirms creation — lets the UI show it immediately (optimistic)
    // while a background job retries the POST if it failed.
    @ColumnInfo(defaultValue = "0") val pendingSync: Boolean = false
)

class StoryTypeConverters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toStringList(raw: String): List<String> = if (raw.isBlank()) emptyList() else raw.split(",")

    @TypeConverter
    fun fromMediaType(type: StoryMediaType): String = type.name

    @TypeConverter
    fun toMediaType(raw: String): StoryMediaType = StoryMediaType.valueOf(raw)

    @TypeConverter
    fun fromVisibility(v: StoryVisibility): String = v.name

    @TypeConverter
    fun toVisibility(raw: String): StoryVisibility = StoryVisibility.valueOf(raw)

    @TypeConverter
    fun fromSyncStatus(s: StorySyncStatus): String = s.name

    @TypeConverter
    fun toSyncStatus(raw: String): StorySyncStatus = StorySyncStatus.valueOf(raw)
}
