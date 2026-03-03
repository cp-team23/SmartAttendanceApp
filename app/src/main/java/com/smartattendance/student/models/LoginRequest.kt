package com.smartattendance.student.models

data class LoginRequest(
    val userId: String,
    val password: String,
    val role : String = "STUDENT"
)
