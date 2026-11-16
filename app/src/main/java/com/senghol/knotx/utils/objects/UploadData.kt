package com.senghol.knotx.utils.objects

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonObject
import com.senghol.knotx.utils.enums.FileDataType

@SuppressLint("ParcelCreator")
@Parcelize
data class UploadData(
    val filePaths: List<String>, // ✅ instead of single filePath
    val uuid: String,
    val receiversJson: String,
    val password: String,
    val isGroup: Boolean,
    val receiver: String,
    val messageId: String,
    val dataType: FileDataType,
    val mainJson: String,   //todo add one more file size or add this directly to json
) : Parcelable {
    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        TODO("Not yet implemented")
    }
}


annotation class Parcelize
