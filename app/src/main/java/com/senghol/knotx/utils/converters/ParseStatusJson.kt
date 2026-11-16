package com.senghol.knotx.utils.converters

import com.senghol.knotx.utils.data.SandeshStatusInfo
import com.senghol.knotx.utils.data.SingleMessageStatus
import org.json.JSONObject

fun parseStatusJson(json: String?): SandeshStatusInfo {
    if (json.isNullOrEmpty()) {
        return SandeshStatusInfo(null, null, emptyMap(), emptyMap())
    }

    val obj = JSONObject(json)

    val likhit = obj.optLong("0", -1).takeIf { it != -1L }
    val prekshit = obj.optLong("1", -1).takeIf { it != -1L }

    val samarpit = mutableMapOf<String, Long>()
    val dhrist = mutableMapOf<String, Long>()

    // delivered map (2)
    if (obj.has("2")) {
        val d2 = obj.getJSONObject("2")
        d2.keys().forEach { number ->
            samarpit[number] = d2.optLong(number)
        }
    }

    // seen map (3)
    if (obj.has("3")) {
        val d3 = obj.getJSONObject("3")
        d3.keys().forEach { number ->
            dhrist[number] = d3.optLong(number)
        }
    }

    return SandeshStatusInfo(
        likhit = likhit,
        prekshit = prekshit,
        samarpit = samarpit,
        dhrist = dhrist
    )
}


fun parseSingleStatusJson(json: String?): SingleMessageStatus {
    if (json.isNullOrEmpty()) return SingleMessageStatus(null, null, null, null)

    val obj = JSONObject(json)

    val likhit   = obj.optLong("0", 0).takeIf { it != 0L }
    val prekshit = obj.optLong("1", 0).takeIf { it != 0L }
    val samarpit = obj.optLong("2", 0).takeIf { it != 0L }
    val dhrist   = obj.optLong("3", 0).takeIf { it != 0L }

    return SingleMessageStatus(
        likhit, prekshit, samarpit, dhrist
    )
}