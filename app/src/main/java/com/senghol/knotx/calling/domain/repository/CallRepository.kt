package com.senghol.knotx.calling.domain.repository

import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallSession
import kotlinx.coroutines.flow.Flow

interface CallRepository {
    suspend fun saveCall(call: CallSession)
    suspend fun updateCall(call: CallSession)
    suspend fun getCall(callId: String): CallSession?
    suspend fun getActiveCall(): CallSession?
    fun observeCall(callId: String): Flow<CallSession?>
    fun observeCalls(): Flow<List<CallSession>>
    suspend fun deleteCall(callId: String)

    suspend fun addParticipant(callId: String, participant: CallParticipant)
    suspend fun updateParticipant(callId: String, participant: CallParticipant)
    suspend fun removeParticipant(callId: String, participantId: String)
    suspend fun getParticipants(callId: String): List<CallParticipant>
    fun observeParticipants(callId: String): Flow<List<CallParticipant>>
}