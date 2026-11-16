package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.senghol.knotx.utils.enums.CallResult
import kotlinx.coroutines.flow.Flow


@Dao
interface CallSessionDao {

    // Insert a new CallSession
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(callSession: CallSession)

    // Update an existing CallSession
    @Update
    suspend fun updateCall(callSession: CallSession)

    // Delete a CallSession
    @Delete
    suspend fun deleteCall(callSession: CallSession)

    // Get all calls ordered by newest first
    @Query("SELECT * FROM call_sessions ORDER BY start_time DESC")
    fun getAllCalls(): Flow<List<CallSession>>

    // Get calls filtered by outcome
    @Query("SELECT * FROM call_sessions WHERE outcome = :outcome ORDER BY start_time DESC")
    fun getCallsByOutcome(outcome: Int): Flow<List<CallSession>>

    // Update duration by room ID
    @Query("UPDATE call_sessions SET duration = :duration WHERE room_id = :roomId")
    suspend fun updateCallDuration(roomId: String, duration: Long)

    // Update outcome by room ID
    @Query("UPDATE call_sessions SET outcome = :outcome WHERE room_id = :roomId")
    suspend fun updateCallOutcome(roomId: String, outcome: CallResult)

    // Get specific call by ID
    @Query("SELECT * FROM call_sessions WHERE id = :callId")
    suspend fun getCallById(callId: Long): CallSession?

    // Get all calls by sender
    @Query("SELECT * FROM call_sessions WHERE sender = :sender ORDER BY start_time DESC")
    fun getCallsBySender(sender: String): Flow<List<CallSession>>

    // Get all calls by receiver
    @Query("SELECT * FROM call_sessions WHERE receiver = :receiver ORDER BY start_time DESC")
    fun getCallsByReceiver(receiver: String): Flow<List<CallSession>>

    // Get calls in a specific time range
    @Query("SELECT * FROM call_sessions WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time DESC")
    fun getCallsByTimeRange(startTime: Long, endTime: Long): Flow<List<CallSession>>

    @Query("""
    UPDATE call_sessions
    SET outcome = :outcome,
        duration = :duration
    WHERE room_id = :roomId
""")
    suspend fun updateCallOutcomeAndDuration(
        roomId: String,
        outcome: CallResult,
        duration: Long
    ): Int


    @Query("""
    UPDATE call_sessions
    SET receiver = :newUser
    WHERE receiver = :oldUser
""")
    suspend fun updateCallReceiver(oldUser: String, newUser: String)
}