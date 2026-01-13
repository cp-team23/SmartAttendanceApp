package com.smartattendance.student

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnMarkAttendance = findViewById<MaterialButton>(R.id.btnMarkAttendance)
        val btnViewAttendance = findViewById<MaterialButton>(R.id.btnViewAttendance)
        val imgProfile = findViewById<ImageView>(R.id.imgProfile)
        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)

        btnMarkAttendance.setOnClickListener {
            Toast.makeText(this, "QR Scan screen will open", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, QrScanActivity::class.java))
        }

        btnViewAttendance.setOnClickListener {
            Toast.makeText(this, "Attendance history screen", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, AttendanceActivity::class.java))
        }

        imgProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        tvStudentName.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
