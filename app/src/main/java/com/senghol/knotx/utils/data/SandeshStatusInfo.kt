package com.senghol.knotx.utils.data

import org.json.JSONObject

data class SandeshStatusInfo(
    val likhit: Long?,               // 0
    val prekshit: Long?,             // 1
    val samarpit: Map<String, Long>, // 2
    val dhrist: Map<String, Long>    // 3
)

data class  SingleMessageStatus(
    val likhit: Long?,
    val prekshit: Long?,
    val samarpit: Long?,
    val dhrist: Long?
)



