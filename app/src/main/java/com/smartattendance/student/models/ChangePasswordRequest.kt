package com.smartattendance.student.models

data class ChangePasswordRequest(
    val password: String,
    val newPassword: String,
    val confirmPassword: String
)