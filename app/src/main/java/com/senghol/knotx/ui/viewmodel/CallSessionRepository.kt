package com.senghol.knotx.ui.viewmodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.data.database.CallSession
import com.senghol.knotx.data.database.CallSessionDao
import com.senghol.knotx.data.sqldelight.Call_participant
import com.senghol.knotx.data.sqldelight.Call_session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.senghol.knotx.data.sqldelight.SengholDB
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.Upbhogta
import com.senghol.knotx.data.database.UpbhogtaDao
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// Three small pieces, put each in its own file/package (package lines are guesses):
//   1) CallHistoryItem          -> domain/model
//   2) CallSessionRepository    -> data/repository
//   3) CallHistoryViewModel(+Factory) -> ui/history
//
// Dependency (build.gradle): app.cash.sqldelight:coroutines-extensions
// (SQLDelight 1.x: com.squareup.sqldelight:coroutines-extensions, imports under
//  com.squareup.sqldelight.runtime.coroutines instead of app.cash.sqldelight.coroutines)

// =============================================================================
// 1) Model: one row of the call history list
// =============================================================================

data class CallHistoryItem(
    val callId: String,
    val callType: CallType,
    val direction: CallDirection,
    val state: CallState,
    val participants: List<CallHistoryParticipant>,
    val createdAt: Long,
    val answeredAt: Long?,
    val endedAt: Long?,
    val endReason: CallEndReason?
) {
    val durationSeconds: Long
        get() {
            val start = answeredAt ?: return 0L
            val end = endedAt ?: return 0L
            return ((end - start) / 1000L).coerceAtLeast(0L)
        }

    val wasAnswered: Boolean
        get() = answeredAt != null
}

data class CallHistoryParticipant(
    val userNumber: String,
    val displayName: String,
    val profileUrl: String?,
    val connectionState: ParticipantConnectionState
)
class CallSessionRepository(
    private val dootawasDB: SengholDB,
    private val database: DootawasDatabase
) {
    private val upbhogtaDao get() = database.upbhogtaDao()
    private val queries get() = dootawasDB.call_sessionQueries
    private val participantQueries get() = dootawasDB.call_participantQueries

    private val finishedStates = setOf(
        CallState.ENDED,
        CallState.REJECTED,
        CallState.CANCELLED,
        CallState.BUSY,
        CallState.MISSED,
        CallState.FAILED
    )



    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAll(): Flow<List<CallHistoryItem>> =
        queries.observeAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sessions ->
                sessions.filter { session ->
                    runCatching { CallState.valueOf(session.state) }
                        .getOrNull() in finishedStates
                }
            }
            .flatMapLatest { sessions ->
                if (sessions.isEmpty()) {
                    flowOf<List<CallHistoryItem>>(emptyList())
                } else {
                    val participantFlows: List<Flow<List<Call_participant>>> =
                        sessions.map { session ->
                            participantQueries.observeForCall(session.callId)
                                .asFlow()
                                .mapToList(Dispatchers.IO)
                        }

                    combine(participantFlows) { participantLists: Array<List<Call_participant>> ->
                        sessions.mapIndexed { index, session ->
                            session to participantLists[index]
                        }
                    }.map { sessionParticipants ->
                        val numbers = sessionParticipants
                            .flatMap { it.second }
                            .map { it.userNumber }
                            .distinct()

                        val contacts: Map<String, Upbhogta> =
                            if (numbers.isEmpty()) {
                                emptyMap()
                            } else {
                                withContext(Dispatchers.IO) {
                                    upbhogtaDao.getUpbhogtasByUserNumbers(numbers)
                                }.associateBy { it.userNumber }
                            }

                        sessionParticipants.mapNotNull { (session, rows) ->
                            session.toHistoryItem(
                                rows = rows,
                                contacts = contacts
                            )
                        }
                    }
                }
            }

    suspend fun getById(callId: String): CallHistoryItem? =
        withContext(Dispatchers.IO) {
            val session = queries.getById(callId).executeAsOneOrNull()
                ?: return@withContext null

            val rows = participantQueries.getForCall(callId).executeAsList()
            val contacts = if (rows.isEmpty()) {
                emptyMap()
            } else {
                upbhogtaDao.getUpbhogtasByUserNumbers(
                    rows.map { it.userNumber }.distinct()
                ).associateBy { it.userNumber }
            }

            session.toHistoryItem(rows, contacts)
        }

    private fun Call_session.toHistoryItem(
        rows: List<Call_participant>,
        contacts: Map<String, Upbhogta>
    ): CallHistoryItem? {
        val callType = runCatching { CallType.valueOf(callType) }.getOrNull()
            ?: return null
        val direction = runCatching { CallDirection.valueOf(direction) }.getOrNull()
            ?: return null
        val state = runCatching { CallState.valueOf(state) }.getOrNull()
            ?: return null

        val participants = rows.map { row ->
            val contact = contacts[row.userNumber]

            CallHistoryParticipant(
                userNumber = row.userNumber,
                displayName = contact?.contactName?.takeIf { it.isNotBlank() }
                    ?: contact?.onlineName?.takeIf { it.isNotBlank() }
                    ?: row.userNumber,
                profileUrl = contact?.photoUrl,
                connectionState = runCatching {
                    ParticipantConnectionState.valueOf(row.connectionState)
                }.getOrDefault(ParticipantConnectionState.INVITED)
            )
        }

        return CallHistoryItem(
            callId = callId,
            callType = callType,
            direction = direction,
            state = state,
            participants = participants,
            createdAt = createdAt,
            answeredAt = answeredAt,
            endedAt = endedAt,
            endReason = endReason?.let {
                runCatching { CallEndReason.valueOf(it) }.getOrNull()
            }
        )
    }
}
