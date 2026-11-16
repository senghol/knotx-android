package com.senghol.knotx.data.database

import com.google.gson.Gson
import com.google.gson.JsonObject


// Helper functions for working with Gson
object DakiyaSettingHelper {
    private val gson = Gson()

    fun toJson(obj: Any): String = gson.toJson(obj)
    fun fromJson(json: String): JsonObject = gson.fromJson(json, JsonObject::class.java)
}