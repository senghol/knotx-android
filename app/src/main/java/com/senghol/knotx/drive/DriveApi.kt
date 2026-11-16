package com.senghol.knotx.drive

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class DriveListResponse(
    val files: List<DriveFile>
)

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String? = null  // <-- FIX
)


interface DriveApi {
    @GET("files")
    suspend fun listFiles(
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name,mimeType)"
    ): DriveListResponse

    @POST("files?uploadType=multipart")
    @Multipart
    suspend fun uploadFile(
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): DriveFile

    @PATCH("files/{fileId}?uploadType=multipart")
    @Multipart
    suspend fun updateFile(
        @Path("fileId") fileId: String,
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): DriveFile

    @POST("files")
    suspend fun createMetadataOnly(
        @Body body: RequestBody
    ): DriveFile

    @GET("files/{fileId}?alt=media")
    suspend fun downloadFile(
        @Path("fileId") fileId: String
    ): okhttp3.ResponseBody

}
