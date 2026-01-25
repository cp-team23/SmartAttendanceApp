package com.smartattendance.student

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== AUTO LOGIN CHECK =====
        val prefs = getSharedPreferences("student_session", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // ===== Views =====
        val tilStudentId = findViewById<TextInputLayout>(R.id.tilStudentId)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        val etStudentId = findViewById<TextInputEditText>(R.id.etStudentId)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvContactAdmin = findViewById<TextView>(R.id.tvContactAdmin)

        // ===== LOGIN =====
        btnLogin.setOnClickListener {

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

                // Dummy credentials (API later)
                studentId != "12345" || password != "123456" -> {
                    tilPassword.error = "Invalid Student ID or Password"
                }

                else -> {
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

        // ===== FORGOT PASSWORD =====
        tvForgotPassword.setOnClickListener {
            showForgotPasswordBottomSheet()
        }

        // ===== CONTACT ADMIN =====
        tvContactAdmin.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:admin@college.edu")
                putExtra(Intent.EXTRA_SUBJECT, "Smart Attendance - Login Help")
            }
            startActivity(intent)
        }
    }

    // ================= FORGOT PASSWORD =================

    private fun showForgotPasswordBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(
            R.layout.bottomsheet_forgot_password,
            null
        )
        dialog.setContentView(view)

        val etInput = view.findViewById<EditText>(R.id.etResetInput)
        val btnSend = view.findViewById<MaterialButton>(R.id.btnSendLink)
        val tvResult = view.findViewById<TextView>(R.id.tvResultMessage)

        btnSend.setOnClickListener {

            val input = etInput.text.toString().trim()

            etInput.error = null
            tvResult.visibility = View.GONE

            if (input.isEmpty()) {
                etInput.error = "Student ID or Email required"
                return@setOnClickListener
            }

            btnSend.isEnabled = false

            // 🔁 Dummy backend call
            val success = fakeForgotPasswordApi(input)

            tvResult.visibility = View.VISIBLE

            if (success) {
                tvResult.text = "Reset link sent to registered email"
                tvResult.setTextColor(Color.parseColor("#4CAF50"))
                android.os.Handler(mainLooper).postDelayed({
                    dialog.dismiss()
                }, 1000)
            } else {
                tvResult.text = "Student ID or Email not found"
                tvResult.setTextColor(Color.parseColor("#F44336"))
            }

            btnSend.isEnabled = true
        }

        dialog.show()
    }

    // ===== DUMMY BACKEND (BOOLEAN, BACKEND READY) =====
    private fun fakeForgotPasswordApi(identifier: String): Boolean {

        // Replace with API later
        val validUsers = listOf(
            "12345",
            "student@college.edu"
        )

        return validUsers.contains(identifier.lowercase())
    }
}
