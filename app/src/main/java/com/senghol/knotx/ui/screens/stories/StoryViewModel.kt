package com.senghol.knotx.ui.screens.stories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senghol.knotx.KnotxApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap


class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dootawas = (application as KnotxApp).knotx

    private val _audioTracks = MutableStateFlow<List<AudioTrackItem>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackItem>> = _audioTracks.asStateFlow()

    private val _isLoadingMusic = MutableStateFlow(false)
    val isLoadingMusic: StateFlow<Boolean> = _isLoadingMusic.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // In-Memory Cache: Stores tracks per category for fast lookup
    private val musicCache = ConcurrentHashMap<String, List<AudioTrackItem>>()

    init {
        fetchMusic("all")
    }

    fun fetchMusic(category: String, forceRefresh: Boolean = false) {
        val normalizedCategory = category.lowercase()
        _selectedCategory.value = normalizedCategory

        // 1. Serve immediately from cache if available and refresh is not forced
        if (!forceRefresh && musicCache.containsKey(normalizedCategory)) {
            _audioTracks.value = musicCache[normalizedCategory] ?: emptyList()
            return
        }

        // 2. Fallback: Fetch from server over socket
        viewModelScope.launch {
            _isLoadingMusic.value = true
            try {
                val tracks = dootawas.requestStoryMp3Links(normalizedCategory)

                if (tracks.isNotEmpty()) {
                    // Save to in-memory cache
                    musicCache[normalizedCategory] = tracks
                    _audioTracks.value = tracks
                } else {
                    // If network returned empty, keep old cache or set empty list
                    _audioTracks.value = musicCache[normalizedCategory] ?: emptyList()
                }
            } catch (e: Exception) {
                // Return cached version on failure if present
                _audioTracks.value = musicCache[normalizedCategory] ?: emptyList()
            } finally {
                _isLoadingMusic.value = false
            }
        }
    }

    // Optional helper to clear cache if needed (e.g., on pull-to-refresh)
    fun clearMusicCache() {
        musicCache.clear()
    }
}