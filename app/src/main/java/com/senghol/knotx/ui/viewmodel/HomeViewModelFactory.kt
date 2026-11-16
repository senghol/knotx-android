package com.senghol.knotx.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.datastore.DootawasStore
import com.senghol.knotx.ui.repository.HomeGroupRepository
import com.senghol.knotx.ui.repository.HomeSingleRepository
import com.senghol.knotx.ui.repository.StoryRepository

class HomeViewModelFactory(
    private val singleRepository: HomeSingleRepository,
    private val groupRepository: HomeGroupRepository,
    private val callRepository: CallSessionRepository,
    private val storyRepository: StoryRepository,
    private val knotx: Knotx,
    private val dootawasStore: DootawasStore
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                singleRepository = singleRepository,
                groupRepository = groupRepository,
                callRepository = callRepository,
                storyRepository = storyRepository,
                knotx = knotx,
                dootawasStore = dootawasStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}