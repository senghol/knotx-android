package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
@Dao
interface StoryDao {

    @Query("SELECT * FROM stories WHERE expiresAt > :nowMs ORDER BY createdAt DESC")
    fun observeActiveFeed(nowMs: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE ownerId = :userId AND expiresAt > :nowMs ORDER BY createdAt ASC")
    fun observeUserStories(
        userId: String,
        nowMs: Long
    ): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getById(storyId: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingUpload(): List<StoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stories: List<StoryEntity>)

    @Update
    suspend fun update(story: StoryEntity)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteById(storyId: String)

    @Query("DELETE FROM stories WHERE expiresAt < :nowMs")
    suspend fun purgeExpired(nowMs: Long)

    // ---- Local cached state updates ----

    @Query("UPDATE stories SET isLikedByMe = :liked, likeCount = :likeCount WHERE id = :storyId")
    suspend fun setLikeState(
        storyId: String,
        liked: Boolean,
        likeCount: Int
    )

    @Query("UPDATE stories SET commentCount = commentCount + :delta WHERE id = :storyId")
    suspend fun adjustCommentCount(
        storyId: String,
        delta: Int
    )

    @Query("UPDATE stories SET viewCount = :viewCount WHERE id = :storyId")
    suspend fun setViewCount(
        storyId: String,
        viewCount: Int
    )

    @Query("UPDATE stories SET likeCount = :likeCount WHERE id = :storyId")
    suspend fun setLikeCount(
        storyId: String,
        likeCount: Int
    )

    @Query("UPDATE stories SET commentCount = :commentCount WHERE id = :storyId")
    suspend fun setCommentCount(
        storyId: String,
        commentCount: Int
    )
    @Query("SELECT * FROM stories WHERE expiresAt < :nowMs")
    suspend fun getExpiredStories(nowMs: Long): List<StoryEntity>


    @Query("SELECT * FROM stories WHERE ownerId = :ownerId")
    suspend fun getStoriesByOwner(ownerId: String): List<StoryEntity>

    @Query("DELETE FROM stories WHERE ownerId = :ownerId")
    suspend fun deleteByOwner(ownerId: String)


    @Query("DELETE FROM stories WHERE ownerId = :ownerId AND id NOT IN (:storyIds)")
    suspend fun deleteStoriesNotIn(ownerId: String, storyIds: List<String>)

    @Query("UPDATE stories SET storyPass = :storyPass WHERE id = :storyId")
    suspend fun updateStoryPass(
        storyId: String,
        storyPass: String
    )

    @Query("""
    UPDATE stories
    SET localPath = :localPath,
        isStoryDownloaded = 1
    WHERE id = :storyId
""")
    suspend fun markStoryDownloaded(
        storyId: String,
        localPath: String
    )

    @Query("UPDATE stories SET commentsPrivate = :isPrivate WHERE id = :storyId")
    suspend fun setCommentsPrivate(storyId: String, isPrivate: Boolean)
}


@Dao
interface StoryViewDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(view: StoryViewEntity): Long

    @Query("SELECT * FROM story_views WHERE storyId = :storyId ORDER BY viewedAt DESC")
    fun observeViewers(storyId: String): Flow<List<StoryViewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(views: List<StoryViewEntity>)

    @Query("SELECT COUNT(*) FROM story_views WHERE storyId = :storyId")
    suspend fun countFor(storyId: String): Int

    @Query("""
        SELECT storyId
        FROM story_views
        WHERE viewerId = :viewerId
        AND storyId IN (:storyIds)
    """)
    suspend fun getViewedStoryIds(
        viewerId: String,
        storyIds: List<String>
    ): List<String>
}


@Dao
interface StoryLikeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(like: StoryLikeEntity)

    @Query("DELETE FROM story_likes WHERE storyId = :storyId AND userId = :userId")
    suspend fun delete(
        storyId: String,
        userId: String
    )

    @Query("SELECT * FROM story_likes WHERE storyId = :storyId ORDER BY likedAt DESC")
    fun observeLikers(storyId: String): Flow<List<StoryLikeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(likes: List<StoryLikeEntity>)

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM story_likes
            WHERE storyId = :storyId
            AND userId = :userId
        )
    """)
    suspend fun hasLiked(
        storyId: String,
        userId: String
    ): Boolean

    @Query("SELECT COUNT(*) FROM story_likes WHERE storyId = :storyId")
    suspend fun countFor(storyId: String): Int
}


@Dao
interface StoryCommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: StoryCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(comments: List<StoryCommentEntity>)

    @Query("SELECT * FROM story_comments WHERE storyId = :storyId ORDER BY createdAt ASC")
    fun observeComments(storyId: String): Flow<List<StoryCommentEntity>>

    @Query("SELECT * FROM story_comments WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<StoryCommentEntity>

    @Query("SELECT COUNT(*) FROM story_comments WHERE storyId = :storyId")
    suspend fun countFor(storyId: String): Int

    @Query("DELETE FROM story_comments WHERE id = :commentId")
    suspend fun deleteById(commentId: String)

    @Query("SELECT * FROM story_comments WHERE id = :commentId")
    suspend fun getById(commentId: String): StoryCommentEntity?

    @Delete
    suspend fun delete(comment: StoryCommentEntity)
}