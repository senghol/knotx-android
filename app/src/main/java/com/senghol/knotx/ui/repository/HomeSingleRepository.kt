package com.senghol.knotx.ui.repository

import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.Upbhogta
import com.senghol.knotx.utils.data.CallSessionDisplay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class HomeSingleRepository(
    private val database: DootawasDatabase
) {
    private val upbhogtaDao get() = database.upbhogtaDao()

    fun observeUpbhogtas(): Flow<List<Upbhogta>> =
        upbhogtaDao.getAllUpbhogtas()

    suspend fun updatePinnedStatus(
        number: String,
        isPinned: Boolean
    ) {
        upbhogtaDao.updatePinnedStatus( number, isPinned)
    }

    suspend fun updateMutedStatus(
        number: String,
        isMuted: Boolean
    ) {
        upbhogtaDao.setMutedByNumber(number, isMuted)
    }

    suspend fun deleteUpbhogta(
        number: String
    ) {
        upbhogtaDao.deleteUpbhogta(number)
    }

    fun observeCallSessionDisplays(): Flow<List<CallSessionDisplay>> =
        upbhogtaDao.getAllCallsWithUser().map { list ->
            list.map { item ->
                CallSessionDisplay(
                    callSession = item.callSession,
                    userName = item.contactName ?: item.callSession.receiver,
                    photoUrl = item.photoUrl
                )
            }
        }
}