package com.senghol.knotx.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TypingState(
    val userNumber: String,
    val isTyping: Boolean,
    val isGroup: Boolean = false,
    val groupId: String? = null,
    val memberNumber: String? = null,
    val isAi: Boolean = false
)

object TypingManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private const val USER_TIMEOUT = 2000L
    private const val AI_TIMEOUT = 15000L

    private val _typingStates =
        MutableStateFlow<Map<String, TypingState>>(emptyMap())

    val typingStates: StateFlow<Map<String, TypingState>> =
        _typingStates.asStateFlow()

    private val typingJobs = mutableMapOf<String, Job>()

    private fun key(state: TypingState): String {
        return if (state.isGroup) {
            "${state.groupId}:${state.memberNumber}"
        } else {
            state.userNumber
        }
    }

    fun startTyping(state: TypingState) {
        val id = key(state)

        typingJobs.remove(id)?.cancel()

        _typingStates.update {
            it + (id to state.copy(isTyping = true))
        }

        val timeout = if (state.isAi) AI_TIMEOUT else USER_TIMEOUT

        typingJobs[id] = scope.launch {
            delay(timeout)
            clearTyping(id)
        }
    }

    fun clearTyping(state: TypingState) {
        clearTyping(key(state))
    }

    private fun clearTyping(id: String) {
        typingJobs.remove(id)?.cancel()

        _typingStates.update {
            it - id
        }
    }

    fun clearConversation(userNumber: String) {
        val keys = _typingStates.value
            .filterValues { it.userNumber == userNumber }
            .keys

        keys.forEach { clearTyping(it) }
    }

    fun clearAll() {
        typingJobs.values.forEach { it.cancel() }
        typingJobs.clear()
        _typingStates.value = emptyMap()
    }
}