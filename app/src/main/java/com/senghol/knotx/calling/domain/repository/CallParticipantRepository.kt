package com.senghol.knotx.calling.domain.repository

import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallParticipantUi


interface CallParticipantRepository {
    suspend fun resolveParticipant(participant: CallParticipant): CallParticipantUi
}