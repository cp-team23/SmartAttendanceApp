package com.smartattendance.student.network

import android.content.Context
import android.content.Intent
import okhttp3.Interceptor
import okhttp3.Response

class SessionExpiredInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            // Clear both SharedPreferences stores
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
                .edit().clear().apply()

            // Navigate to LoginActivity, clear back stack
            val intent = Intent(context, com.smartattendance.student.LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}
