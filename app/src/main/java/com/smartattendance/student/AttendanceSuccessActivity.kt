package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_success)

        val tvDetails = findViewById<TextView>(R.id.tvAttendanceDetails)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val subject      = intent.getStringExtra("subject") ?: "Attendance"
        val dateFromIntent = intent.getStringExtra("date")
        val displayDate  = dateFromIntent ?: getCurrentDate()

        tvDetails.text = "$subject • $displayDate"

        bottomNav.menu.setGroupCheckable(0, false, true)

        bottomNav.setOnItemSelectedListener { item ->
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            when (item.itemId) {
                R.id.nav_home -> {
                    goToHome()
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

        // FIX: Back button goes to Home, not back to FaceVerification (which has no camera)
        onBackPressedDispatcher.addCallback(this) {
            goToHome()
        }

        // FIX: Auto-navigate to Home after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) goToHome()
        }, 3000)
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}