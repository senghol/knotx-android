package com.senghol.knotx.utils
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromUserList(value: List<String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toUserList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}