package com.smartattendance.student.models

data class AttendanceResponse(
    val response: List<StudentAttendanceResponse>
)

data class StudentAttendanceResponse(
    val attendanceId: String,
    val attendanceDate: String,
    val attendanceTime: String,
    val subjectName: String,
    val status: String,
    val teacherName: String
)