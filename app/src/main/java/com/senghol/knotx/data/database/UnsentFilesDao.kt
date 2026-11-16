package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.FileDataType


@Dao
interface UnsentFilesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(unsentFiles: UnsentFiles)

    @Delete
    fun delete(unsentFiles: UnsentFiles)

    @Query(
        """
        DELETE FROM unsentFiles
        WHERE messageId IN (:deleteSandeshList)
        AND receiver = :receiverId
        """
    )
    suspend fun deleteUserListMessage(
        deleteSandeshList: List<String>,
        receiverId: String
    )

    @Query("SELECT * FROM UnsentFiles WHERE messageId = :messageId AND receiver = :sender LIMIT 1")
    fun getDataByMessageIdAndSender(messageId: String, sender: String): UnsentFiles?

    @Query("SELECT EXISTS(SELECT 1 FROM UnsentFiles)")
    fun isAnyDataPresent(): Boolean

    @Query("DELETE FROM UnsentFiles WHERE messageId = :messageId AND receiver = :sender")
    suspend fun deleteByMessageIdAndSender(messageId: String, sender: String)

    @Query("DELETE FROM UnsentFiles WHERE receiver = :receiver")
    fun deleteUserAllData(receiver: String)

    @Query("SELECT * FROM UnsentFiles ORDER BY time ASC")
    fun getEarliestMessages(): List<UnsentFiles>


    @Query("SELECT * FROM unsentFiles WHERE receiver = :receiver AND dataType = :dataType LIMIT 1")
    suspend fun getExistingRequest(receiver: String, dataType: FileDataType): UnsentFiles?

    @Query("DELETE FROM unsentFiles WHERE receiver = :receiver AND dataType = :dataType")
    suspend fun deleteExisting(receiver: String, dataType: FileDataType)

    @Transaction
    suspend fun deleteAndInsert(unsentFile: UnsentFiles) {
        val existing = getExistingRequest(unsentFile.receiver, unsentFile.dataType)
        if (existing != null) {
            
            deleteExisting(unsentFile.receiver, unsentFile.dataType)
        }
        insert(unsentFile)
        
    }
}
