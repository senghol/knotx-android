package com.senghol.knotx.data.database

import androidx.room.*
import com.senghol.knotx.utils.enums.DataType

@Dao
interface UnsentSandeshDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unsentSandesh: UnsentSandesh)

    @Delete
    suspend fun delete(unsentSandesh: UnsentSandesh)


    @Query(
        """
        DELETE FROM UnsentSandesh
        WHERE messId IN (:deleteSandeshList)
        AND receiver = :receiverId
        """
    )
    suspend fun deleteUserListMessage(
        deleteSandeshList: List<String>,
        receiverId: String
    )
    @Query("SELECT COUNT(*) FROM UnsentSandesh WHERE messId = :messId AND receiver = :receiver AND dataType = :dataType")
    suspend fun countExistingEntries(messId: String, receiver: String, dataType: DataType): Int

    @Query("SELECT COUNT(*) FROM UnsentSandesh WHERE dataType = :dataType")
    suspend fun countDataType(dataType: DataType): Int

    @Query("SELECT * FROM UnsentSandesh WHERE receiver = :receiver AND dataType = :dataType LIMIT 1")
    suspend fun getUnsentSandeshByReceiverAndType(receiver: String, dataType: DataType): UnsentSandesh?

    @Query("""
    SELECT * FROM UnsentSandesh 
    WHERE dataSent = 0 
    ORDER BY time ASC 
    LIMIT 1
""")
    suspend fun getEarliestInsertedMessage(): UnsentSandesh?

    @Query("""
    UPDATE UnsentSandesh
    SET dataType = :holdType,
        dataSent = 1
    WHERE messId = :messageId AND receiver = :receiver
""")
    suspend fun markMessageOnHold(
        messageId: String,
        receiver: String,
        holdType: DataType = DataType.MESSAGE_ON_HOLD
    )


    @Query("""
    UPDATE UnsentSandesh
    SET dataType = :normalType,
        dataSent = 0
    WHERE dataType = :holdType AND receiver = :receiver
""")
    suspend fun restoreHeldMessagesForUser(
        receiver: String,
        normalType: DataType = DataType.MESSAGE,
        holdType: DataType = DataType.MESSAGE_ON_HOLD
    )

    @Query("""
    SELECT * FROM UnsentSandesh 
    WHERE dataSent = 0 
      AND dataType IN (:types)
    ORDER BY time ASC 
    LIMIT 1
""")
    suspend fun getEarliestUnsentByTypes(
        types: List<DataType>
    ): UnsentSandesh?


    @Query("DELETE FROM UnsentSandesh WHERE messId = :messId AND dataType = :chunk AND receiver = :receiver")
    suspend fun deleteByMessIdAndSender(messId: String, chunk: DataType, receiver: String)

    @Query("""
    UPDATE unsentSandesh 
    SET dataSent = 1 
    WHERE messId = :messId AND receiver = :receiver AND dataType = :dataType
""")
    suspend fun markAsSent(
        messId: String,
        dataType: DataType,
        receiver: String
    ): Int



    @Query("""
    DELETE FROM unsentSandesh
    WHERE id IN (
        SELECT id FROM unsentSandesh
        WHERE dataSent = 1
        ORDER BY time ASC
        LIMIT (
            SELECT CASE 
                WHEN COUNT(*) > 100 THEN COUNT(*) - 100
                ELSE 0
            END
            FROM unsentSandesh
            WHERE dataSent = 1
        )
    )
""")
    suspend fun deleteOldestSentMessagesOverLimit()
    @Query("SELECT COUNT(*) FROM unsentSandesh WHERE dataSent = 1")
    suspend fun countSentMessages(): Int

    @Query("UPDATE UnsentSandesh SET dataSent = 0 WHERE messId = :messId AND receiver = :receiver")
    suspend fun markDataSentFalse(messId: String, receiver: String)


    @Query("""
    SELECT * FROM unsentSandesh 
    WHERE receiver = :receiver 
      AND message = :message 
      AND dataType = :dataType 
      AND dataSent = 0
    LIMIT 1
""")
    suspend fun findSimilarUnsentSandesh(
        receiver: String,
        message: String,
        dataType: DataType
    ): UnsentSandesh?

}
