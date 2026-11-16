package com.senghol.knotx.ui.repository

import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.GroupEntity
import kotlinx.coroutines.flow.Flow

class HomeGroupRepository(
    private val database: DootawasDatabase
) {
    private val groupDao get() = database.groupDao()

    fun observeGroups(): Flow<List<GroupEntity>> =
        groupDao.getAllGroups()

    suspend fun updatePinnedStatus(
        groupId: String,
        isPinned: Boolean
    ) {
        groupDao.updatePinnedStatus(groupId, isPinned)
    }

    suspend fun updateMutedStatus(
        groupId: String,
        isMuted: Boolean
    ) {
        groupDao.setGroupMuted(groupId, isMuted)
    }

    suspend fun deleteGroup(
        groupId: String
    ) {
        groupDao.deleteGroup(groupId)
    }

    suspend fun clearGroupChat(
        groupId: String
    ) {
        groupDao.clearGroupChat(groupId)
        groupDao.clearGroupChatEntity(groupId)
    }
}