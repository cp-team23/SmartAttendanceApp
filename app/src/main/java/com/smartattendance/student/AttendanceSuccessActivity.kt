package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AttendanceSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_success)

        // Views
        val tvDetails = findViewById<TextView>(R.id.tvAttendanceDetails)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Optional data from intent (future backend use)
        val subject = intent.getStringExtra("subject") ?: "Attendance Recorded"
        val time = intent.getStringExtra("time")

        tvDetails.text = if (time != null) {
            "$subject • $time"
        } else {
            subject
        }

        // Bottom Navigation handling
        bottomNav.menu.setGroupCheckable(0, false, true)

        bottomNav.setOnItemSelectedListener { item ->
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java), options.toBundle())
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
