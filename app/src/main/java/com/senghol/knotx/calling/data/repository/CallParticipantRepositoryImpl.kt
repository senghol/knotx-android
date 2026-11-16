package com.senghol.knotx.calling.data.repository


import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallParticipantUi
import com.senghol.knotx.calling.domain.repository.CallParticipantRepository

import com.senghol.knotx.data.database.ContactDao
import com.senghol.knotx.data.database.UpbhogtaDao


import javax.inject.Inject

class CallParticipantRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val upbhogtaDao: UpbhogtaDao
) : CallParticipantRepository {

    override suspend fun resolveParticipant(
        participant: CallParticipant
    ): CallParticipantUi {
        val contact = contactDao.getUserContactByNumber(participant.userNumber)
        val user = upbhogtaDao.getUserByNumber(participant.userNumber)

        val displayName =
            contact?.name
                ?: user?.contactName
                ?: user?.onlineName
                ?: participant.userNumber

        val profileUrl =
            contact?.imageUrl
                ?: user?.photoUrl

        return CallParticipantUi(
            participant = participant,
            displayName = displayName,
            profileUrl = profileUrl
        )
    }
}