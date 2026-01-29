package com.smartattendance.student.models

data class QrAttendancePayload(
    val attendanceId: String,
    val encryptedCode: String,
    val expireTime: Long
)