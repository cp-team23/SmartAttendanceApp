package com.smartattendance.student

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.smartattendance.student.models.ErrorResponse
import com.smartattendance.student.models.LoginRequest
import com.smartattendance.student.models.LoginResponse
import com.smartattendance.student.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== AUTO LOGIN CHECK (JWT BASED) =====
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        if (!token.isNullOrEmpty()) {
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

                else -> {
                    btnLogin.isEnabled = false
                    loginWithBackend(studentId, password, btnLogin)
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

    // ================= BACKEND LOGIN =================

    private fun loginWithBackend(
        studentId: String,
        password: String,
        btnLogin: MaterialButton
    ) {

        val request = LoginRequest(
            userId = studentId,
            password = password
        )

        RetrofitClient.create(this).login(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    btnLogin.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {

                        val token = response.body()!!.token

                        getSharedPreferences("auth_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("jwt_token", token)
                            .putString("student_id", studentId)
                            .putString("student_name", "Alex Harrison")
                            .apply()

                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()

                    } else {
                        handleLoginError(response)
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    Toast.makeText(
                        this@LoginActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ================= ERROR HANDLING =================

    private fun handleLoginError(response: Response<*>) {
        try {
            val errorJson = response.errorBody()?.string()
            if (errorJson != null) {
                val errorResponse = gson.fromJson(errorJson, ErrorResponse::class.java)

                val message = when (errorResponse.error) {
                    "USER_NOT_FOUND" ->
                        "Student ID not found"

                    "WRONG_PASSWORD" ->
                        "Incorrect password"

                    "TEMPORARY_BLOCKED" ->
                        "Your account is temporarily blocked. Please try later."

                    else ->
                        "Login failed. Please try again."
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show()
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

    // ===== DUMMY FORGOT PASSWORD BACKEND =====
    private fun fakeForgotPasswordApi(identifier: String): Boolean {
        val validUsers = listOf(
            "12345",
            "student@college.edu"
        )
        return validUsers.contains(identifier.lowercase())
    }
}
