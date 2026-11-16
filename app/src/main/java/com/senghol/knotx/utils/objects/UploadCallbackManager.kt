package com.senghol.knotx.utils.objects

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
object UploadCallbackManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<String, Job>()

    fun registerCallback(messageId: String, callback: (responseCode: Int) -> Unit) {
        // Cancel any previous
        activeJobs[messageId]?.cancel()
        activeJobs.remove(messageId)

        val job = scope.launch {
            try {
                val result = WorkManager.getInstance()
                    .getWorkInfosForUniqueWorkFlow("upload_$messageId")
                    .filter { it.isNotEmpty() && it.first().state.isFinished }
                    .map { it.first() }
                    .first()

                val code = if (result.state == WorkInfo.State.SUCCEEDED) {
                    200
                } else {
                    result.outputData.getInt("final_code", -502)
                }

                callback(code)
            } catch (e: Exception) {
                callback(-999)
            } finally {
                activeJobs.remove(messageId)
            }
        }

        activeJobs[messageId] = job
    }

    fun cancelCallback(messageId: String) {
        activeJobs.remove(messageId)?.cancel()
    }
}