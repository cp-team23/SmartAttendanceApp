package com.smartattendance.student

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Views
        val btnMarkAttendance = findViewById<MaterialButton>(R.id.btnMarkAttendance)
        val btnViewAttendance = findViewById<MaterialButton>(R.id.btnViewAttendance)
        val imgProfile = findViewById<ImageView>(R.id.imgProfile)
        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // ------------------ BUTTON ACTIONS ------------------
        btnMarkAttendance.setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
        }

        btnViewAttendance.setOnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }

        // ------------------ PROFILE CLICK ------------------
        imgProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        tvStudentName.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // ------------------ BOTTOM NAVIGATION ------------------
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    // Already on Home
                    true
                }

                R.id.nav_profile -> {
                    // Go to Profile
                    val intent = Intent(this, ProfileActivity::class.java)
                    val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }

                else -> false
            }
        }
    }
}
