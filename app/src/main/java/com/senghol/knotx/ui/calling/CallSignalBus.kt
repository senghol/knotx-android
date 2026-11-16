package com.senghol.knotx.ui.calling

import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object CallSignalBus {
    val flow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    fun startObserving(scope: CoroutineScope, onSignal: suspend (String) -> Unit) {
        scope.launch {
            flow.collectLatest(onSignal)
        }
    }

    suspend fun emit(json: String) {
        flow.emit(json)
    }
}


object CallIntentBus {
    val flow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    suspend fun emit(json: String) = flow.emit(json)
}
