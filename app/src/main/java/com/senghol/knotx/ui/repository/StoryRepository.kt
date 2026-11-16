package com.senghol.knotx.ui.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.StoryCommentEntity
import com.senghol.knotx.data.database.StoryEntity
import com.senghol.knotx.data.database.StoryLikeEntity
import com.senghol.knotx.data.database.StoryViewEntity
import com.senghol.knotx.data.database.StoryVisibility
import com.senghol.knotx.data.database.UnsentSandesh
import com.senghol.knotx.utils.data.UserStoryGroup
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.FileDataTypeToken
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.StoryAction
import com.senghol.knotx.utils.objects.DLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

class StoryRepository(
    private val database: DootawasDatabase,
    private val knotx: Knotx
) {
    private val storyDao get() = database.storyDao()
    private val viewDao get() = database.storyViewDao()
    private val likeDao get() = database.storyLikeDao()
    private val commentDao get() = database.storyCommentDao()
    private val contactDao get() = database.contactDao()

    private fun expirationTicker(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }

    fun observeMyStories(
        myUserId: String
    ): Flow<List<StoryEntity>> =
        expirationTicker().flatMapLatest { now ->
            storyDao.observeUserStories(myUserId, now)
        }

    fun observeFeedStoryGroups(
        myUserId: String,
        getUserName: (String) -> String
    ): Flow<List<UserStoryGroup>> {
        return expirationTicker()
            .flatMapLatest { now ->
                storyDao.observeActiveFeed(now)
            }
            .map { allStories ->
                allStories
                    .filter { it.ownerId != myUserId }
                    .groupBy { it.ownerId }
                    .map { (ownerId, stories) ->
                        val storyIds = stories.map { it.id }

                        val viewedStoryIds = viewDao
                            .getViewedStoryIds(
                                viewerId = myUserId,
                                storyIds = storyIds
                            )
                            .toSet()

                        UserStoryGroup(
                            userId = ownerId,
                            userName = getUserName(ownerId),
                            userAvatarUrl = null,
                            stories = stories.sortedBy { it.createdAt },
                            hasUnseenStories = stories.any {
                                it.id !in viewedStoryIds
                            }
                        )
                    }
                    .sortedByDescending { it.hasUnseenStories }
            }
    }


    fun observeComments(
        storyId: String
    ): Flow<List<StoryCommentEntity>> =
        commentDao.observeComments(storyId)

    fun observeViewers(
        storyId: String
    ): Flow<List<StoryViewEntity>> =
        viewDao.observeViewers(storyId)

    fun observeLikers(
        storyId: String
    ): Flow<List<StoryLikeEntity>> =
        likeDao.observeLikers(storyId)

    suspend fun getStoryOwner(
        storyId: String
    ): String? {
        return storyDao.getById(storyId)?.ownerId
    }
    suspend fun markStoryAsViewed(
        storyId: String,
        currentUserId: String,
    ) {
        val ownerId = storyDao.getById(storyId)?.ownerId
            ?: throw IllegalStateException("Story owner not found: $storyId")

        val inserted = database.withTransaction {
            val result = viewDao.insert(
                StoryViewEntity(
                    storyId = storyId,
                    viewerId = currentUserId,
                    viewedAt = System.currentTimeMillis()
                )
            )

            if (result == -1L) {
                return@withTransaction false
            }

            storyDao.setViewCount(
                storyId = storyId,
                viewCount = viewDao.countFor(storyId)
            )

            true
        }

        if (inserted) {
            val request = JsonObject().apply {
                addProperty(
                    JsonKeys.STORY_ACTION,
                    StoryAction.VIEW_STORY.value
                )

                addProperty(
                    DootawasMessage.STORY_ID,
                    storyId
                )
            }

            knotx.sendStoryAction(
                storyOwner = ownerId,
                request = request
            )
        }
    }

    suspend fun toggleLike(
        storyId: String,
        currentUserId: String,
    ) {
        val ownerId = storyDao.getById(storyId)?.ownerId
            ?: throw IllegalStateException("Story owner not found: $storyId")
        val newLikedState = database.withTransaction {
            val currentlyLiked = likeDao.hasLiked(
                storyId = storyId,
                userId = currentUserId
            )

            if (currentlyLiked) {
                likeDao.delete(storyId, currentUserId)
            } else {
                likeDao.insert(
                    StoryLikeEntity(
                        storyId = storyId,
                        userId = currentUserId,
                        likedAt = System.currentTimeMillis()
                    )
                )
            }

            val liked = !currentlyLiked

            storyDao.setLikeState(
                storyId = storyId,
                liked = liked,
                likeCount = likeDao.countFor(storyId)
            )

            liked
        }

        val request = JsonObject().apply {
            addProperty(
                JsonKeys.STORY_ACTION,
                if (newLikedState) {
                    StoryAction.LIKE_STORY.value
                } else {
                    StoryAction.UNLIKE_STORY.value
                }
            )

            addProperty(
                DootawasMessage.STORY_ID,
                storyId
            )
        }

        knotx.sendStoryAction(
            storyOwner = ownerId,
            request = request
        )
    }

    suspend fun addComment(
        storyId: String,
        currentUserId: String,
        text: String
    ): StoryCommentEntity {
        val ownerId = storyDao.getById(storyId)?.ownerId
            ?: throw IllegalStateException("Story owner not found: $storyId")
        // isPrivate always starts false — it's not something the commenter sets.
        // Only the owner can flip it later via setCommentPrivate(), below.
        val comment = StoryCommentEntity(
            id = UUID.randomUUID().toString(),
            storyId = storyId,
            userId = currentUserId,
            text = text,
            createdAt = System.currentTimeMillis(),
            pendingSync = true,
        )
        database.withTransaction {
            commentDao.insert(comment)
            storyDao.adjustCommentCount(storyId, 1)
        }
        val request = JsonObject().apply {
            addProperty(JsonKeys.STORY_ACTION, StoryAction.ADD_COMMENT.value)
            addProperty(DootawasMessage.STORY_ID, storyId)
            addProperty(JsonKeys.COMMENT, text)
            addProperty(JsonKeys.COMMENT_ID, comment.id)
        }
        knotx.sendStoryAction(storyOwner = ownerId, request = request)
        return comment
    }

    /**
     * Owner-only, whole-comment-section toggle — NOT per-comment. Everyone can
     * still comment regardless of this setting. When true, the server only
     * returns each non-owner their OWN comments (so they can still see/delete
     * what they wrote), while the owner keeps seeing everyone's. That
     * filtering is entirely server-side; this just flips the setting and
     * updates the local cache so the UI can reflect the current state.
     */
    suspend fun setCommentsPrivate(
        storyId: String,
        currentUserId: String,
        isPrivate: Boolean
    ) {
        val story = storyDao.getById(storyId)
            ?: throw IllegalStateException("Story not found: $storyId")
        if (story.ownerId != currentUserId) {
            throw IllegalAccessException("Only the story owner can change this")
        }

        storyDao.setCommentsPrivate(storyId, isPrivate)

        val request = JsonObject().apply {
            addProperty(JsonKeys.STORY_ACTION, StoryAction.SET_COMMENT_PRIVATE.value)
            addProperty(DootawasMessage.STORY_ID, storyId)
            addProperty(JsonKeys.COMMENT_IS_PRIVATE, isPrivate) // TODO: add this key to JsonKeys if it isn't there yet
        }
        knotx.sendStoryAction(storyOwner = story.ownerId, request = request)
    }
    /**
     * Owner-only: changes who can see the STORY itself (separate axis from
     * comment privacy above). Updates the local cache immediately and notifies
     * the server via the same action-send channel as everything else here.
     */
    suspend fun setVisibility(
        storyId: String,
        currentUserId: String,
        visibility: StoryVisibility
    ) {
        val story = storyDao.getById(storyId)
            ?: throw IllegalStateException("Story not found: $storyId")
        if (story.ownerId != currentUserId) {
            throw IllegalAccessException("Only the story owner can change visibility")
        }

        storyDao.upsert(story.copy(visibility = visibility))

        val request = JsonObject().apply {
            addProperty(JsonKeys.STORY_ACTION, StoryAction.SET_VISIBILITY.value)
            addProperty(DootawasMessage.STORY_ID, storyId)
            addProperty(
                DootawasMessage.VISIBILITY,
                when (visibility) {
                    StoryVisibility.EVERYONE -> 0
                    StoryVisibility.CUSTOM -> 1
                    StoryVisibility.PRIVATE -> 2
                }
            )
        }
        knotx.sendStoryAction(storyOwner = story.ownerId, request = request)
    }

    suspend fun deleteComment(
        commentId: String,
        storyId: String,
        currentUserId: String
    ) {
        val story = storyDao.getById(storyId)
            ?: throw IllegalStateException("Story not found: $storyId")
        val comment = commentDao.getById(commentId)
            ?: throw IllegalStateException("Comment not found: $commentId")

        // Either the comment's own author, or the story owner (moderation), can delete it.
        if (comment.userId != currentUserId && story.ownerId != currentUserId) {
            throw IllegalAccessException("Not allowed to delete this comment")
        }

        database.withTransaction {
            commentDao.deleteById(commentId)
            storyDao.adjustCommentCount(storyId, -1)
        }
        val request = JsonObject().apply {
            addProperty(JsonKeys.STORY_ACTION, StoryAction.DELETE_COMMENT.value)
            addProperty(DootawasMessage.STORY_ID, storyId)
            addProperty(JsonKeys.COMMENT_ID, commentId)
        }
        knotx.sendStoryAction(storyOwner = story.ownerId, request = request)
    }

    /**
     * Fetches the complete details of a story from the server.
     *
     * The response contains:
     * - Comments
     * - Likes
     * - Views (owner only)
     * - Like count
     * - Comment count
     * - View count
     * - Comment privacy
     */
    suspend fun fetchStoryDetails(
        storyId: String,
        ownerId: String
    ) {
        val request = JsonObject().apply {
            addProperty(
                JsonKeys.STORY_ACTION,
                StoryAction.GET_STORY_DETAILS.value
            )
            addProperty(
                DootawasMessage.STORY_ID,
                storyId
            )
        }

        knotx.sendStoryAction(
            storyOwner = ownerId,
            request = request
        )
    }
    suspend fun saveNewStory(
        story: StoryEntity
    ) {
        storyDao.upsert(story)
    }

    suspend fun deleteStory(
        storyId: String,

    ) {
        val ownerId = storyDao.getById(storyId)?.ownerId
            ?: throw IllegalStateException("Story owner not found: $storyId")
        val story = storyDao.getById(storyId)

        storyDao.deleteById(storyId)

        story?.localPath?.let { path ->
            File(path)
                .takeIf { it.exists() }
                ?.delete()
        }

        val request = JsonObject().apply {
            addProperty(
                JsonKeys.STORY_ACTION,
                StoryAction.DELETE_STORY.value
            )

            addProperty(
                DootawasMessage.STORY_ID,
                storyId
            )
        }

        knotx.sendStoryAction(
            storyOwner = ownerId,
            request = request
        )
    }
    suspend fun purgeExpiredStories() {
        val now = System.currentTimeMillis()
        val expired = storyDao.getExpiredStories(now) // add this query to StoryDao, see below
        storyDao.purgeExpired(now)
        expired.forEach { story ->
            story.localPath?.let { path ->
                File(path).takeIf { it.exists() }?.delete()
            }
        }
    }

    suspend fun getAllContactStories() {
        val contacts = contactDao.getAllContacts().first()

        val phoneNumbers = contacts
            .map { it.number }
            .filter { it.isNotBlank() }
            .distinct()

        if (phoneNumbers.isEmpty()) {
            DLog.i("STORY HOME: no contacts found")
            return
        }

        try {
            val request = JsonObject().apply {
                addProperty(
                    JsonKeys.FILE_TOKEN_TYPE,
                    FileDataTypeToken.GET_STORIES_DIRECT.value
                )

                add(
                    JsonKeys.RECEIVERS,
                    Gson().toJsonTree(phoneNumbers)
                )
            }

            val unsentSandesh = UnsentSandesh(
                messId = UUID.randomUUID().toString(),
                receiver = "server",
                message = request.toString(),
                dataType = DataType.FILE_PROCESS,
                isGroup = false
            )

            knotx.insertUnsentData(unsentSandesh)

            DLog.i(
                "STORY HOME: requested direct stories for ${phoneNumbers.size} contacts"
            )

        } catch (e: Exception) {
            DLog.e(
                "STORY HOME: failed requesting stories: ${e.message}",
                e
            )
        }
    }}