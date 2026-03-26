package com.smartattendance.student

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.smartattendance.student.adapters.ImageUrlHelper

class HomeActivity : AppCompatActivity() {

    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnMarkAttendance = findViewById<MaterialButton>(R.id.btnMarkAttendance)
        val btnViewAttendance = findViewById<MaterialButton>(R.id.btnViewAttendance)
        val imgProfile        = findViewById<ImageView>(R.id.imgProfile)
        val tvStudentName     = findViewById<TextView>(R.id.tvStudentName)
        val bottomNavigation  = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Profile data is already saved by LoginActivity before this screen opens
        // So real name and photo show immediately — no flicker
        loadProfileData(imgProfile, tvStudentName)

        btnMarkAttendance.setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
        }
        btnViewAttendance.setOnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }
        imgProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }
        tvStudentName.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        bottomNavigation.selectedItemId = R.id.nav_home
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> true
                R.id.nav_profile -> {
                    val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(Intent(this, ProfileActivity::class.java), options.toBundle())
                    true
                }
                else -> false
            }
        }

        // Double-tap back to exit
        onBackPressedDispatcher.addCallback(this) {
            if (System.currentTimeMillis() - backPressedTime < 2000) {
                finish()
            } else {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(this@HomeActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload in case student updated photo in ProfileActivity
        loadProfileData(
            findViewById(R.id.imgProfile),
            findViewById(R.id.tvStudentName)
        )
    }

    private fun loadProfileData(imgProfile: ImageView, tvStudentName: TextView) {
        val pref = getSharedPreferences("profile_data", MODE_PRIVATE)

        tvStudentName.text = pref.getString("name", "Student")

        val imageUrl = ImageUrlHelper.resolve(pref.getString("image", null))
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.profile_temp)
                .error(R.drawable.profile_temp)
                .into(imgProfile)
        } else {
            imgProfile.setImageResource(R.drawable.profile_temp)
        }
    }
}