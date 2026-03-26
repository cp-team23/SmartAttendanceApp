package com.smartattendance.student.network

import android.content.Context
import com.smartattendance.student.adapters.AppConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    fun create(context: Context): AuthApi {

        // FIX: Use NONE in release so JWT token is never printed to Logcat
        // To debug during development, temporarily change to Level.BODY
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(AuthInterceptor(context))  // JWT token header
            .addInterceptor(SessionExpiredInterceptor(context)) // FIX: handle 401
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(AppConstants.BASE_URL_SLASH) // FIX: single source from AppConstants
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}