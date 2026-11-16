package com.senghol.knotx.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.senghol.knotx.connection.Knotx

class SandeshViewModelManager(
    private val repository: SandeshRepository,
    private val knotx: Knotx
) : ViewModel() {

    private val cacheSize = 20
    private val viewModelCache = object : LinkedHashMap<String, SandeshViewModel>(cacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SandeshViewModel>): Boolean {
            return size > cacheSize
        }
    }

    fun get(userNumber: String, isGroup: Boolean): SandeshViewModel {
        return viewModelCache.getOrPut(userNumber) {
            SandeshViewModel(repository, userNumber,isGroup,knotx)
        }
    }

    fun evict(userNumber: String) {
        viewModelCache.remove(userNumber)
    }

    override fun onCleared() {
        viewModelCache.clear()
        super.onCleared()
    }
}
