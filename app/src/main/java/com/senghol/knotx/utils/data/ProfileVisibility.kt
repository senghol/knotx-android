package com.senghol.knotx.utils.data

data class ProfileVisibility(
    val mode: String,         // "all", "none", "custom"
    val viewers: List<String>? = null // only needed if mode == "custom"
)
