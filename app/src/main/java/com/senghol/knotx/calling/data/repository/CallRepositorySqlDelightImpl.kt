package com.senghol.knotx.calling.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.senghol.knotx.data.sqldelight.SengholDB
import com.senghol.knotx.calling.data.model.toDomain
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.repository.CallRepository
//import com.senghol.knotx.data.sqldelight.DootawasDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CallRepositorySqlDelightImpl @Inject constructor(
    private val database: SengholDB
) : CallRepository {

    private val sessionQueries = database.call_sessionQueries
    private val participantQueries = database.call_participantQueries

    override suspend fun saveCall(call: CallSession) {
        database.transaction {
            sessionQueries.insert(
                callId = call.callId.value,
                callType = call.callType.name,
                direction = call.direction.name,
                state = call.state.name,
                createdAt = call.createdAt,
                startedAt = call.startedAt,
                answeredAt = call.answeredAt,
                endedAt = call.endedAt,
                endReason = call.endReason?.name
            )

            call.participants.forEach { participant ->
                insertParticipant(
                    callId = call.callId.value,
                    participant = participant
                )
            }
        }
    }

    override suspend fun updateCall(call: CallSession) {
        database.transaction {
            sessionQueries.update(
                callType = call.callType.name,
                direction = call.direction.name,
                state = call.state.name,
                createdAt = call.createdAt,
                startedAt = call.startedAt,
                answeredAt = call.answeredAt,
                endedAt = call.endedAt,
                endReason = call.endReason?.name,
                callId = call.callId.value
            )

            participantQueries.deleteForCall(call.callId.value)

            call.participants.forEach { participant ->
                insertParticipant(
                    callId = call.callId.value,
                    participant = participant
                )
            }
        }
    }

    override suspend fun getCall(callId: String): CallSession? {
        val session = sessionQueries
            .getById(callId)
            .executeAsOneOrNull()
            ?: return null

        val participants = participantQueries
            .getForCall(callId)
            .executeAsList()

        return session.toDomain(participants)
    }

    override fun observeCall(callId: String): Flow<CallSession?> {
        return sessionQueries
            .observeById(callId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sessions ->
                sessions.firstOrNull()?.let { session ->
                    val participants = participantQueries
                        .getForCall(callId)
                        .executeAsList()

                    session.toDomain(participants)
                }
            }
    }

    override fun observeCalls(): Flow<List<CallSession>> {
        return sessionQueries
            .observeAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sessions ->
                sessions.map { session ->
                    val participants = participantQueries
                        .getForCall(session.callId)
                        .executeAsList()

                    session.toDomain(participants)
                }
            }
    }

    override suspend fun deleteCall(callId: String) {
        database.transaction {
            participantQueries.deleteForCall(callId)
            sessionQueries.deleteById(callId)
        }
    }

    override suspend fun addParticipant(
        callId: String,
        participant: CallParticipant
    ) {
        insertParticipant(
            callId = callId,
            participant = participant
        )
    }

    override suspend fun updateParticipant(
        callId: String,
        participant: CallParticipant
    ) {
        participantQueries.update(
            userNumber = participant.userNumber,
            userNameId = participant.userNameId,
            connectionState = participant.connectionState.name,
            isMicrophoneEnabled = if (participant.isMicrophoneEnabled) 1 else 0,
            isCameraEnabled = if (participant.isCameraEnabled) 1 else 0,
            isScreenSharing = if (participant.isScreenSharing) 1 else 0,
            joinedAt = participant.joinedAt,
            leftAt = participant.leftAt,
            callId = callId,
            participantId = participant.participantId
        )
    }

    override suspend fun removeParticipant(
        callId: String,
        participantId: String
    ) {
        participantQueries.deleteParticipant(
            callId = callId,
            participantId = participantId
        )
    }

    override suspend fun getParticipants(
        callId: String
    ): List<CallParticipant> {
        return participantQueries
            .getForCall(callId)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun observeParticipants(
        callId: String
    ): Flow<List<CallParticipant>> {
        return participantQueries
            .observeForCall(callId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { participants ->
                participants.map { it.toDomain() }
            }
    }

    override suspend fun getActiveCall(): CallSession? {
        val session = sessionQueries
            .getActiveCall()
            .executeAsOneOrNull()
            ?: return null

        val participants = participantQueries
            .getForCall(session.callId)
            .executeAsList()

        return session.toDomain(participants)
    }

    private fun insertParticipant(
        callId: String,
        participant: CallParticipant
    ) {
        participantQueries.insert(
            callId = callId,
            participantId = participant.participantId,
            userNumber = participant.userNumber,
            userNameId = participant.userNameId,
            connectionState = participant.connectionState.name,
            isMicrophoneEnabled = if (participant.isMicrophoneEnabled) 1 else 0,
            isCameraEnabled = if (participant.isCameraEnabled) 1 else 0,
            isScreenSharing = if (participant.isScreenSharing) 1 else 0,
            joinedAt = participant.joinedAt,
            leftAt = participant.leftAt
        )
    }
}