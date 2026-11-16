package com.senghol.knotx.connection

import java.nio.charset.Charset
import kotlin.random.Random



object JsonCorrupter {

    /** Master function — randomly corrupts JSON based on probability (0.0–1.0) */
    fun maybeCorrupt(json: String, corruptionChance: Double): ByteArray {
        // Roll a random chance
        if (Random.nextDouble() >= corruptionChance) {
            // No corruption → return original bytes
            
            return json.toByteArray(Charsets.UTF_8)
        } else{
            
        }

        // Choose a random corruption method
        val methods = listOf<(String) -> ByteArray>(
            { truncate(it).toByteArray() },
            { removePunctuation(it).toByteArray() },
            { insertControlChar(it).toByteArray() },
            { appendGarbageA(it).toByteArray() },
            { appendGarbageB(it).toByteArray() },
            { appendGarbageC(it).toByteArray() },
            { withBomBytes(it) },
            { flipRandomByte(it.toByteArray()) },
            { swapRandomFieldType(it).toByteArray() },
            { duplicateRandomField(it).toByteArray() },
            { addTrailingComma(it).toByteArray() }
        )

        val chosen = methods.random()
        val corrupted = chosen(json)
        
        return corrupted
    }

    // 1. truncate at random position
    fun truncate(json: String, minKeep: Int = 1): String {
        if (json.length <= minKeep) return json
        val cut = Random.nextInt(minKeep, json.length)
        return json.substring(0, cut)
    }

    // 2. remove a random comma or colon
    fun removePunctuation(json: String): String {
        val indices = json.mapIndexedNotNull { i, c -> if (c == ',' || c == ':') i else null }
        if (indices.isEmpty()) return json
        val i = indices.random()
        return json.removeRange(i, i + 1)
    }

    // 3. insert random control char
    fun insertControlChar(json: String): String {
        val quoteIndices = json.mapIndexedNotNull { i, c -> if (c == '"') i else null }
        if (quoteIndices.size < 2) return json
        val start = quoteIndices.random()
        val end = quoteIndices.firstOrNull { it > start } ?: return json
        val pos = Random.nextInt(start + 1, end)
        val ctrl = (0..31).random().toChar()
        return json.substring(0, pos) + ctrl + json.substring(pos)
    }

    // 4. flip a random byte
    fun flipRandomByte(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return bytes
        val clone = bytes.copyOf()
        val idx = Random.nextInt(clone.size)
        clone[idx] = (clone[idx].toInt() xor (1 shl Random.nextInt(0, 8))).toByte()
        return clone
    }

    // 5. swap type for a random field
    fun swapRandomFieldType(json: String): String {
        val regex = """"(\w+)"\s*:\s*("[^"]*"|\d+|true|false|null)""".toRegex()
        val matches = regex.findAll(json).toList()
        if (matches.isEmpty()) return json
        val m = matches.random()
        val newValue = if (m.groupValues[2].startsWith("\"")) Random.nextInt(0, 100).toString() else "\"oops\""
        return json.replaceRange(m.range, "\"${m.groupValues[1]}\": $newValue")
    }

    // 6. append garbage (three variants)
    fun appendGarbageA(json: String, garbage: String = "GARBAGE" + '\u0000' + '\u00FF') = json + garbage
    fun appendGarbageB(json: String): String {
        val randomGarbage = ByteArray(5) { (0..255).random().toByte() }
        return json + String(randomGarbage, Charsets.ISO_8859_1)
    }
    fun appendGarbageC(json: String, garbage: String = """GARBAGE\x00\xFF""") = json + garbage

    // 7. add BOM bytes
    fun withBomBytes(json: String): ByteArray {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        return bom + json.toByteArray(Charset.forName("UTF-8"))
    }

    // 8. duplicate a random key
    fun duplicateRandomField(json: String): String {
        val regex = """"(\w+)"\s*:\s*("[^"]*"|\d+|true|false|null)""".toRegex()
        val matches = regex.findAll(json).toList()
        if (matches.isEmpty()) return json
        val m = matches.random()
        val insertPoint = json.lastIndexOf('}')
        return if (insertPoint > 0)
            json.substring(0, insertPoint) + ", " + m.value + json.substring(insertPoint)
        else json
    }

    // 9. add trailing comma before end brace
    fun addTrailingComma(json: String): String {
        val idx = json.lastIndexOf('}')
        return if (idx > 0) json.substring(0, idx) + ",}" else json
    }
}
