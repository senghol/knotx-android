package com.senghol.knotx.data.database

import androidx.room.*

@Dao
interface SandeshStatusDao {

    @Query("SELECT statusJson FROM sandeshStatus WHERE userNumber = :userNumber AND sandeshId = :sandeshId")
    suspend fun getStatusJson(userNumber: String, sandeshId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStatus(status: SandeshStatus)

    @Query("""
    UPDATE sandeshStatus
    SET userNumber = :newUser
    WHERE userNumber = :oldUser
""")
    suspend fun updateSandeshStatusUserNumber(oldUser: String, newUser: String)

}
