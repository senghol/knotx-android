package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType

import kotlinx.coroutines.flow.Flow

@Dao
interface SandeshDao {
    @Query("SELECT * FROM sandesh WHERE userNumber = :userName ORDER BY time DESC")
    fun getMessagesByUser(userName: String): Flow<List<Sandesh>>
    @Query("SELECT * FROM sandesh")
    suspend fun getAllSandesh(): List<Sandesh>
    @Query("SELECT * FROM sandesh WHERE sandeshStithi = :stithi AND userNumber = :userNumber")
    suspend fun getAllMessagesByStithi(stithi: SandeshStithi, userNumber: String): List<Sandesh>


    @Query("SELECT * FROM sandesh WHERE sandeshId IN (:sandeshIds) AND userNumber = :userNumber")
    suspend fun getSandeshesByIdsAndUser(sandeshIds: List<String>, userNumber: String): List<Sandesh>
    @Query("""
    UPDATE sandesh
    SET samagri = :samagri
    WHERE sandeshId = :sandeshId
    AND userNumber = :userNumber
""")
    suspend fun updateSamagri(
        sandeshId: String,
        userNumber: String,
        samagri: String
    )
    // Live Flow of all messages for a user, sorted newest first
    // Load older messages (keyset paging)
    @Query("SELECT * FROM sandesh WHERE userNumber = :userNumber AND time < :lastTime ORDER BY time DESC LIMIT :limit")
    suspend fun getMessagesBefore(userNumber: String, lastTime: Long, limit: Int): List<Sandesh>
    @Query("SELECT * FROM sandesh WHERE sandeshId = :sandeshId AND userNumber = :userNumber LIMIT 1")
    suspend fun getSandeshByIdAndUser(sandeshId: String, userNumber: String): Sandesh? // ✅ New method
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(sandesh: Sandesh)
    @Query("SELECT * FROM sandesh WHERE userNumber = :userNumber AND sandeshType = :type")
    fun getSandeshByUserAndType(userNumber: String, type: Int): Flow<List<Sandesh>>

    @Query("""
    SELECT * FROM sandesh
    WHERE userNumber = :userNumber
    AND isSelf = 0
""")
    suspend fun getReceivedMessagesForUser(userNumber: String): List<Sandesh>

    @Query("""
    DELETE FROM sandesh
    WHERE sandeshId IN (:sandeshIds)
    AND userNumber = :userNumber
""")
    suspend fun deleteMessagesByIds(
        sandeshIds: List<String>,
        userNumber: String
    )

    @Query("SELECT EXISTS(SELECT 1 FROM sandesh WHERE sandeshId = :sandeshId)")
    suspend fun existsById(sandeshId: String): Boolean

    @Query("""
    UPDATE sandesh 
    SET sandeshStithi = :newStithi 
    WHERE sandeshId = :messageId 
      AND userNumber = :userNumber 
      AND sandeshStithi < :newStithi
""")
    fun updateStithiIfForwardProgress(
        messageId: String,
        userNumber: String,
        newStithi: Int  // use `stithi.value`
    )
    @Query("SELECT * FROM upbhogta WHERE userNumber = :number LIMIT 1")
    fun observeUserByNumber(number: String): Flow<Upbhogta?>

    @Query("UPDATE upbhogta SET draftMessage = :draft WHERE userNumber = :userNumber")
    suspend fun updateDraft(userNumber: String, draft: String)

    @Query("UPDATE sandesh SET time = :newTime WHERE userNumber = :userNumber AND sandeshId = :sandeshId")
    suspend fun updateTimeByUserAndId(userNumber: String, sandeshId: String, newTime: Long)


//    @Query(
//        """
//    DELETE FROM sandesh
//    WHERE sandeshId IN (:messageIds)
//    AND userNumber = :receiverId
//    AND (:skipSelfCheck = 1 OR isSelf = 0)
//    AND sandeshType != 26
//    """
//    )
//    suspend fun deleteMessages(
//        messageIds: List<String>,
//        receiverId: String,
//        skipSelfCheck: Boolean
//    )
    @Query("UPDATE sandesh SET samagri = :samagri WHERE sandeshId = :sandeshId")
    suspend fun updateSamagri(sandeshId: String, samagri: String)

    @Delete
    suspend fun deleteMessage(sandesh: Sandesh)

    @Query("DELETE FROM sandesh WHERE userNumber = :number")
    suspend fun clearChatByNumber(number: String)


    // ✅ Get unread RECEIVED messages
    @Query("""
        SELECT sandeshId 
        FROM sandesh
        WHERE userNumber = :userId
          AND isSelf = 0
          AND sandeshStithi != :dhrist
    """)
    suspend fun getUnreadUserMessageIds(
        userId: String,
        dhrist: SandeshStithi = SandeshStithi.DHRIST
    ): List<String>


    // ✅ ✅ SAFE UPDATE — ONLY FOR IDS YOU ALREADY SENT
    @Query("""
        UPDATE sandesh
        SET sandeshStithi = :dhrist
        WHERE sandeshId IN (:messageIds)
    """)
    suspend fun markMessagesAsReadByIds(
        messageIds: List<String>,
        dhrist: SandeshStithi = SandeshStithi.DHRIST
    )


    @Query(
        """
    UPDATE Sandesh
    SET sandeshStithi = :status
    """
    )
    suspend fun markAllMessagesAsViewed(
        status: Int = SandeshStithi.DHRIST.value
    )

    @Query("""
    SELECT * FROM sandesh
    WHERE sandeshType = :listType
    AND userNumber = :userNumber
    ORDER BY time DESC
""")
    fun observeLists(
        userNumber: String,
        listType: Int = SandeshType.LIST.value
    ): Flow<List<Sandesh>>


    @Query("""
    DELETE FROM sandesh
    WHERE sandeshId = :sandeshId
    AND userNumber = :userNumber
    AND sandeshType = :listType
""")
    suspend fun deleteList(
        sandeshId: String,
        userNumber: String,
        listType: Int = SandeshType.LIST.value
    )

    @Query("""
    UPDATE sandesh
    SET userNumber = :newUser
    WHERE userNumber = :oldUser
""")
    suspend fun updateSandeshUserNumber(oldUser: String, newUser: String)
}

