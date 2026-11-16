package com.senghol.knotx.data.database

import android.provider.SyncStateContract.Helpers.insert
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.senghol.knotx.utils.data.CallWithUser
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlinx.coroutines.flow.Flow
@Dao
interface UpbhogtaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpbhogta(upbhogta: Upbhogta)

    @Query("SELECT * FROM Upbhogta ORDER BY lastMessageTime DESC")
    fun getAllUpbhogtas(): Flow<List<Upbhogta>>

    @Query("SELECT * FROM Upbhogta ORDER BY lastMessageTime DESC LIMIT :limit")
    suspend fun getTopUpbhogtas(limit: Int): List<Upbhogta>

    @Query("SELECT * FROM upbhogta WHERE isGroup = 1") //todo delete this
    fun getGroupUpbhogtas(): Flow<List<Upbhogta>>

    @Query("SELECT * FROM upbhogta WHERE userNumber = :number LIMIT 1")
    suspend fun getUserByNumber(number: String): Upbhogta?

    @Query("SELECT COUNT(*) FROM upbhogta WHERE userNumber = :userNumber")
    suspend fun doesUserExist(userNumber: String): Int  // ✅ Returns count (0 = not exists, >0 = exists)

    @Query("SELECT * FROM upbhogta WHERE userNumber IN (:userNumbers)")
    suspend fun getUpbhogtasByUserNumbers(
        userNumbers: List<String>
    ): List<Upbhogta>
    @Query("UPDATE upbhogta SET photoUrl = :newUrl WHERE userNumber = :number")
    suspend fun updatePhotoUrlByNumber(newUrl: String, number: String)

    @Query("SELECT photoUrl FROM upbhogta WHERE userNumber = :number")
    suspend fun getImageUrlByNumber(number: String): String?

    @Query("SELECT contactName FROM upbhogta WHERE userNumber = :number")
    suspend fun getUserNameByNumber(number: String): String?

    @Query("SELECT onlineName FROM upbhogta WHERE userNumber = :number")
    suspend fun getUserOnlineNameByNumber(number: String): String?

    @Query("SELECT * FROM upbhogta WHERE userNumber LIKE :query OR contactName LIKE :query")
    fun searchUpbhogtas(query: String): Flow<List<Upbhogta>>

    @Query("UPDATE Upbhogta SET isBlocked = :isBlocked WHERE userNumber = :number")
    suspend fun updateBlockedStatus( number: String, isBlocked: Boolean)

    @Query("UPDATE Upbhogta SET isPinned = :isPinned WHERE userNumber = :number")
    suspend fun updatePinnedStatus(number: String, isPinned: Boolean)

    @Query("UPDATE Upbhogta SET isMuted = :isMuted WHERE contactName = :userName AND userNumber = :number")
    suspend fun updateMutedStatus(userName: String, number: String, isMuted: Boolean)

    @Query("UPDATE Upbhogta SET isMuted = :isMuted WHERE userNumber = :userNumber")
    suspend fun setMutedByNumber(userNumber: String, isMuted: Boolean)
    @Query("DELETE FROM Upbhogta WHERE userNumber = :number")
    suspend fun deleteUpbhogta(number: String)

        @Query("""
        UPDATE upbhogta 
        SET lastMessage = :lastMessage, 
            lastMessageTime = :lastMessageTime, 
            lastMessageId = :lastMessageId,
            sandeshStithi = :sandeshStithi
        WHERE userNumber = :userNumber
    """)
        suspend fun updateLastMessageAndId(
            userNumber: String,
            lastMessage: String,
            lastMessageTime: Long,
            lastMessageId: String,
            sandeshStithi: SandeshStithi
        )

            @Query("""
            UPDATE upbhogta
            SET lastMessage = :deletedText,
                lastMessageId = '',
                sandeshStithi = :sandeshStithi,
                lastMessageTime = :lastMessageTime
            WHERE lastMessageId = :messageId AND userNumber = :number
        """)
            suspend fun resetLastMessageByMessageId(
                messageId: String,
                deletedText: String = "",
                sandeshStithi: SandeshStithi = SandeshStithi.LIKHIT,
                lastMessageTime: Long = System.currentTimeMillis(),
                number: String
            )


            @Query("""
            UPDATE upbhogta 
            SET sandeshStithi = :newStithi 
            WHERE lastMessageId = :messageId 
              AND userNumber = :userNumber 
              AND sandeshStithi < :newStithi
        """)
            fun updateStithiUpbhogtaIfForwardProgress(
                messageId: String,
                userNumber: String,
                newStithi: Int  // use `stithi.value`
            )


    // 🖼 Update profile image timestamp
    @Query("UPDATE upbhogta SET imageTime = :newImageTime WHERE userNumber = :userNumber")
    fun updateImageTimeByNumber(userNumber: String, newImageTime: Long)


    @Query("UPDATE upbhogta SET totalUnread = totalUnread + 1 WHERE userNumber = :userNumber")
    suspend fun incrementUnreadByUserNumber(userNumber: String)

    // Optional: to reset unread
    @Query("UPDATE upbhogta SET totalUnread = 0 WHERE userNumber = :userNumber")
    suspend fun resetUnread(userNumber: String)


    @Query("UPDATE upbhogta SET lastOnline = :lastOnline WHERE userNumber = :userNumber")
    fun updateLastOnlineByNumber(lastOnline: Long, userNumber: String)

    @Query("UPDATE Upbhogta SET onlineName = :onlineName, about = :about WHERE userNumber = :userNumber")
    suspend fun updateOnlineNameAndAboutByNumber(
        userNumber: String,
        onlineName: String?,
        about: String?
    )

    @Query("""
    UPDATE Upbhogta
    SET wallpaperImage = :wallpaperImage
    WHERE userNumber = :userNumber
""")
    suspend fun updateWallpaper(
        userNumber: String,
        wallpaperImage: String?
    )

    @Query("""
    UPDATE upbhogta
    SET customColors = NULL
""")
    suspend fun clearAllCustomColors()

    // ✅ Get mute status for a given userNumber
    @Query("SELECT isMuted FROM Upbhogta WHERE userNumber = :userNumber LIMIT 1")
    suspend fun isMuted(userNumber: String): Boolean?

    @Query("""
    SELECT c.*, u.contactName, u.photoUrl
    FROM call_sessions c
    LEFT JOIN upbhogta u
    ON c.receiver = u.userNumber
    ORDER BY c.start_time DESC
""")
    fun getAllCallsWithUser(): Flow<List<CallWithUser>>


    @Query("""
    UPDATE upbhogta
    SET customColors = :colorsJson
    WHERE userNumber = :userNumber
""")
    suspend fun updateUserColors(
        userNumber: String,
        colorsJson: String
    )
    @Query(
        """
    UPDATE Upbhogta
    SET photoUrl = NULL
    """
    )
    suspend fun clearAllProfilePictures()


    @Query("UPDATE upbhogta SET bakaya = bakaya + :delta WHERE userNumber = :userNumber")
    suspend fun incrementBakaya(userNumber: String, delta: Double)  // ← Double

    @Query("UPDATE upbhogta SET bakaya = :amount WHERE userNumber = :userNumber")
    suspend fun setBakaya(userNumber: String, amount: Double)  // ← Double

    @Query("""
    SELECT revealSender
    FROM upbhogta
    WHERE userNumber = :userNumber
    LIMIT 1
""")
    suspend fun getRevealSender(userNumber: String): Boolean?

    @Query("""
    UPDATE upbhogta
    SET revealSender = :revealSender
    WHERE userNumber = :userNumber
""")
    suspend fun updateRevealSender(
        userNumber: String,
        revealSender: Boolean
    )

    @Query("UPDATE upbhogta SET autoDisappearMsg = :duration WHERE userNumber = :userNumber")
    suspend fun updateAutoDisappearDuration(
        userNumber: String,
        duration: Int
    )
    @Query("SELECT autoDisappearMsg FROM upbhogta WHERE userNumber = :userNumber")
    suspend fun getAutoDisappearDuration(userNumber: String): Int?

    @Query("""
    UPDATE upbhogta
    SET userNumber = :newUser
    WHERE userNumber = :oldUser
""")
    suspend fun updateUserNumber(oldUser: String, newUser: String): Int

    @Query("UPDATE upbhogta SET revealSender = 1")
    suspend fun setRevealSenderTrueForAll()

    @Query("UPDATE upbhogta SET hideKey = :key, isMuted = :isMuted WHERE userNumber = :number")
    suspend fun updateHideKey(number: String, key: String?, isMuted: Boolean)
}