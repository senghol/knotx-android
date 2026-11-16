package com.senghol.knotx.utils.objects

import com.google.gson.GsonBuilder
import com.senghol.knotx.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.getValue


// --- FIXED & PRODUCTION-READY RetrofitClient (November 2025) ---
object RetrofitClient {

    // ⚠️ CRITICAL: You MUST reuse the same OkHttpClient instance everywhere
    // (especially for downloading from signed URLs later)
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)    // ← Increased: signed URLs can be slow (S3, R2, etc.)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)

        // ↓ VERY IMPORTANT for large profile ZIPs (some are 5–15 MB encrypted)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Accept", "application/json,*/*")
                .header("User-Agent", "DootawasAndroid/1.0")  // some CDNs block default OkHttp UA
                .build()
            chain.proceed(request)
        }

        .build()

    // Make it public so you can reuse it for signed URL downloads
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            //            .baseUrl("https://api.dootawas.com/") // your base URL
            .baseUrl(BuildConfig.BASE_HOST+ BuildConfig.API_PREFIX)  // ← correct, keep this
            .client(okHttpClient)             // ← same instance
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().setLenient().create()  // ← handles minor JSON issues from Node.js
            ))
            .build()
    }
}