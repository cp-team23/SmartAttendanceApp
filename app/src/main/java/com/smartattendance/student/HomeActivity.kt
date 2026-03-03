package com.smartattendance.student

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {
    private val BASE_URL = "https://mdj4kwmp-8080.inc1.devtunnels.ms"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Views
        val btnMarkAttendance = findViewById<MaterialButton>(R.id.btnMarkAttendance)
        val btnViewAttendance = findViewById<MaterialButton>(R.id.btnViewAttendance)
        val imgProfile = findViewById<ImageView>(R.id.imgProfile)
        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        loadProfileData(imgProfile, tvStudentName)


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

    override fun onResume() {
        super.onResume()

        val imgProfile = findViewById<ImageView>(R.id.imgProfile)
        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)

        loadProfileData(imgProfile, tvStudentName)
    }

    private fun loadProfileData(imgProfile: ImageView, tvStudentName: TextView) {

        val pref = getSharedPreferences("profile_data", MODE_PRIVATE)

        // ✅ Name
        tvStudentName.text = pref.getString("name", "Student")

        // ✅ Image
        val imagePath = pref.getString("image", null)

        if (!imagePath.isNullOrEmpty() && imagePath != "/uploads/null") {

            Glide.with(this)
                .load(BASE_URL + imagePath)
                .placeholder(R.drawable.profile_temp)
                .error(R.drawable.profile_temp)
                .into(imgProfile)

        } else {
            imgProfile.setImageResource(R.drawable.profile_temp)
        }
    }

}
