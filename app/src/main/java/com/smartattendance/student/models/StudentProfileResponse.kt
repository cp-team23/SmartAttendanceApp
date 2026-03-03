package com.smartattendance.student.models

data class StudentProfileResponse(
    val response: StudentData
)

data class StudentData(
    val userId: String,
    val name: String,
    val email: String,
    val collegeName: String,
    val enrollmentNo: String,
    val year: String,
    val branch: String,
    val semester: String,
    val className: String,
    val batch: String,
    val newImage : String,
    val curImage: String?
)
