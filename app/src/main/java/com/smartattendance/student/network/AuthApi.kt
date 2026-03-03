package com.smartattendance.student.network

import com.smartattendance.student.models.LoginRequest
import com.smartattendance.student.models.LoginResponse
import com.smartattendance.student.models.QrAttendancePayload
import com.smartattendance.student.models.StudentProfileResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface AuthApi {

    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("api/auth/logout")
    fun logout(): Call<Void>

    @GET("api/student/my")
    fun getStudentProfile(): Call<StudentProfileResponse>

    @Multipart
    @POST("api/student/change-my-image")
    fun uploadProfileImage(
        @Part image: MultipartBody.Part
    ): Call<ResponseBody>

    @DELETE("api/student/image-request")
    fun deleteImageRequest(): Call<Map<String, String>>

    @PATCH("api/student/scan-qr-code")
    fun scanQrCode(
        @Body payload: QrAttendancePayload
    ): Call<Map<String, String>>


}
