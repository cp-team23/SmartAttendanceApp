package com.smartattendance.student.network

import com.smartattendance.student.models.*
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

    @PATCH("api/user/change-password")
    fun changePassword(
        @Body request: ChangePasswordRequest
    ): Call<Map<String, String>>

    @GET("/api/student/all-attendance")
    fun getAllAttendance(): Call<AttendanceResponse>

    @Multipart
    @PATCH("api/student/scan-face/{attendanceId}")
    fun scanFace(
        @Path("attendanceId") attendanceId: String,
        @Part image: MultipartBody.Part
    ): Call<Map<String, String>>

}
