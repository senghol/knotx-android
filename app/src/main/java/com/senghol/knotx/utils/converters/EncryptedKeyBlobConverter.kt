package com.senghol.knotx.utils.converters
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.senghol.knotx.utils.data.EncryptedKeyBlob

class EncryptedKeyBlobConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromBlob(blob: EncryptedKeyBlob?): String? {
        return blob?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toBlob(json: String?): EncryptedKeyBlob? {
        return json?.let { gson.fromJson(it, EncryptedKeyBlob::class.java) }
    }
}