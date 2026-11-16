package com.senghol.knotx.utils

import com.google.gson.JsonParser
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.utils.objects.DLog
import java.io.File

class SandeshDeleter(
    private val sandeshDao: SandeshDao
) {

    suspend fun deleteMessages(
        sandeshIds: List<String>,
        userNumber: String,
        skipSelfCheck: Boolean = false
    ) {
        if (sandeshIds.isEmpty()) return

        val sandeshes = sandeshDao.getSandeshesByIdsAndUser(
            sandeshIds = sandeshIds,
            userNumber = userNumber
        )

        val idsToDelete = sandeshes
            .filter { it.sandeshType != 27 }
            .filter { skipSelfCheck || !it.isSelf }
            .onEach { sandesh ->
                when (sandesh.sandeshType) {
                    2 -> deleteVideo(sandesh.samagri)
                }
            }
            .map { it.sandeshId }

        if (idsToDelete.isNotEmpty()) {
            sandeshDao.deleteMessagesByIds(
                sandeshIds = idsToDelete,
                userNumber = userNumber
            )
        }
    }
    private fun deleteVideo(samagri: String) {
        try {
            val json = JsonParser.parseString(samagri).asJsonObject

            json.getAsJsonArray("allData")?.forEach { item ->
                val uri = item.asJsonObject
                    .get("originalUri")
                    ?.asString

                if (!uri.isNullOrBlank()) {
                    deleteFile(uri)
                }
            }

            json.get("blurUri")?.asString?.let { uri ->
                if (uri.isNotBlank()) {
                    deleteFile(uri)
                }
            }

            json.getAsJsonArray("imagePreview")?.forEach { item ->
                val uri = item.asString

                if (uri.isNotBlank()) {
                    deleteFile(uri)
                }
            }

        } catch (e: Exception) {
            DLog.e("Failed to delete video files: ${e.message}")
        }
    }

    private fun deleteFile(uri: String) {
        try {
            val file = File(uri)

            if (file.exists()) {
                if (file.delete()) {
                    DLog.i("Deleted file: $uri")
                } else {
                    DLog.e("Failed to delete file: $uri")
                }
            }
        } catch (e: Exception) {
            DLog.e("Failed to delete file $uri: ${e.message}")
        }
    }
}