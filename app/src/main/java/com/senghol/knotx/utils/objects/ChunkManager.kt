package com.senghol.knotx.utils.objects

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object ChunkManager {
    private const val DIR = "fcm_chunks"
    private const val DONE = ".done"
    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000
    private val lock = Any()

    fun safe(mid: String) = mid.replace(Regex("[^A-Za-z0-9_-]"), "_")
    private fun root(c: Context) = File(c.filesDir, DIR)

    fun isDone(c: Context, mid: String) = File(File(root(c), safe(mid)), DONE).exists()

    fun isIncomplete(c: Context, mid: String): Boolean {
        val d = File(root(c), safe(mid))
        return d.exists() && !File(d, DONE).exists()
    }

    fun handleChunk(context: Context, chunkJson: String, onComplete: (String, JSONObject) -> Unit) {
        val json = JSONObject(chunkJson)
        val mid = safe(json.getString("mid"))
        val tc = json.getInt("TC")
        val cn = json.getInt("CN")
        if (tc <= 0 || cn !in 0 until tc) {
            Log.w("DakiyaFCM", "bad chunk $cn/$tc")
            return
        }
        val data = json.getString("chunk")

        var assembled: String? = null
        synchronized(lock) {
            val root = root(context).apply { mkdirs() }
            val now = System.currentTimeMillis()
            root.listFiles()?.filter { now - it.lastModified() > MAX_AGE_MS }
                ?.forEach { it.deleteRecursively() }

            val dir = File(root, mid).apply { mkdirs() }
            if (File(dir, DONE).exists()) return          // duplicate push after completion

            val f = File(dir, cn.toString())
            if (!f.exists()) {
                val tmp = File(dir, "$cn.tmp")
                tmp.writeText(data)
                tmp.renameTo(f)                            // atomic: no half-written chunks
            }
            val have = (0 until tc).count { File(dir, it.toString()).exists() }

            if (have == tc) {
                assembled = (0 until tc).joinToString("") { File(dir, it.toString()).readText() }
                (0 until tc).forEach { File(dir, it.toString()).delete() }
                File(dir, DONE).createNewFile()
            }
        }
        val full = assembled
        if (full != null) onComplete(full, json)           // outside the lock
    }
}