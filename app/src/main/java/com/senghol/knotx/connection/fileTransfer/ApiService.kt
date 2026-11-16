package com.senghol.knotx.connection.fileTransfer

import com.google.gson.annotations.SerializedName
import com.senghol.knotx.utils.data.ProfileVisibility
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {

    // ------------------------------------
    // 🔹 PROFILE DOWNLOAD (receiver → profile)
    // ------------------------------------
//    @GET("download/profile")
    @GET("download/profile")
    fun downloadProfile(
        @Header("Authorization") authToken: String,
        @Query("sender") sender: String
    ): Call<SignedUrlResponse>   // ← changed

    @GET("download/group/profile")
    fun downloadGroupProfile(
        @Header("Authorization") authToken: String,
        @Query("sender") sender: String
    ): Call<SignedUrlResponse>   // ← add this if group is separate endpoint

    // ------------------------------------
    // 🔹 FILE DOWNLOAD (Signed URL metadata)
    //     Returns: { "downloadUrl": "https://signed-url..." }
    // ------------------------------------
    @GET("download/file")
    fun downloadFile(
        @Header("Authorization") token: String,
        @Query("sender") sender: String,
        @Query("uuid") uuid: String
    ): Call<SignedUrlResponse>

    // ------------------------------------
// 🔹 UPDATE PROFILE VISIBILITY (POST)
// ------------------------------------
    @POST("profile/visibility")
    fun updateProfileVisibility(
        @Header("Authorization") token: String,
        @Body request: ProfileVisibility
    ): Call<ResponseBody>

    // ------------------------------------
// 🔹 DELETE SELF PROFILE
// ------------------------------------
    @POST("profile/delete")
    fun deleteProfile(
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    // ------------------------------------
// 🔹 DELETE GROUP PROFILE
// ------------------------------------
    @POST("profile/delete/group")
    fun deleteGroupProfile(
        @Header("Authorization") token: String,
        @Query("groupId") groupId: String
    ): Call<ResponseBody>



}


data class SignedUrlResponse(
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("expiresIn") val expiresIn: Int? = null
) {
    // ← Just rename it to anything except getUrl()
    fun validUrl(): String? = downloadUrl ?: url
}
