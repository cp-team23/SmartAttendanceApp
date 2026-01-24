package com.smartattendance.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login check
        val prefs = getSharedPreferences("student_session", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // TextInputLayouts
        val tilStudentId = findViewById<TextInputLayout>(R.id.tilStudentId)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        // EditTexts
        val etStudentId = findViewById<TextInputEditText>(R.id.etStudentId)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvContactAdmin = findViewById<TextView>(R.id.tvContactAdmin)

        btnLogin.setOnClickListener {

            // Clear previous errors
            tilStudentId.error = null
            tilPassword.error = null

            val studentId = etStudentId.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                studentId.isEmpty() -> {
                    tilStudentId.error = "Student ID is required"
                }

                password.isEmpty() -> {
                    tilPassword.error = "Password is required"
                }

                // Dummy invalid credentials (replace with API later)
                studentId != "12345" || password != "123456" -> {
                    tilPassword.error = "Invalid Student ID or Password"
                }

                else -> {
                    // Save session (API values later)
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("student_id", studentId)
                        .putString("student_name", "Alex Harrison")
                        .apply()

                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
        }

        // Contact Admin
        tvContactAdmin.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:admin@college.edu")
                putExtra(Intent.EXTRA_SUBJECT, "Smart Attendance - Login Help")
            }
            startActivity(intent)
        }
    }
}
