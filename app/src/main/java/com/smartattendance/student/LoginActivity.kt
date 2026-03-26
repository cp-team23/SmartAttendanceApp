package com.smartattendance.student

import android.content.Intent
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
import com.smartattendance.student.models.StudentProfileResponse
import com.smartattendance.student.network.RetrofitClient
import com.smartattendance.student.adapters.AppConstants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login: token already exists → go to Home directly
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        if (!token.isNullOrEmpty()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val tilStudentId     = findViewById<TextInputLayout>(R.id.tilStudentId)
        val tilPassword      = findViewById<TextInputLayout>(R.id.tilPassword)
        val etStudentId      = findViewById<TextInputEditText>(R.id.etStudentId)
        val etPassword       = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin         = findViewById<MaterialButton>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvContactAdmin   = findViewById<TextView>(R.id.tvContactAdmin)

        // IME: "Next" moves focus to password field
        etStudentId.setOnEditorActionListener { _, _, _ ->
            etPassword.requestFocus(); true
        }
        // IME: "Done" on password triggers login
        etPassword.setOnEditorActionListener { _, _, _ ->
            btnLogin.performClick(); true
        }

        btnLogin.setOnClickListener {
            tilStudentId.error = null
            tilPassword.error  = null

            val studentId = etStudentId.text.toString().trim()
            val password  = etPassword.text.toString().trim()

            when {
                studentId.isEmpty() -> tilStudentId.error = "Student ID is required"
                password.isEmpty()  -> tilPassword.error  = "Password is required"
                else -> {
                    btnLogin.isEnabled = false
                    btnLogin.text      = "Logging in..."
                    loginWithBackend(studentId, password, btnLogin)
                }
            }
        }

        tvForgotPassword.setOnClickListener { showForgotPasswordBottomSheet() }

        tvContactAdmin.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${AppConstants.ADMIN_EMAIL}")
                putExtra(Intent.EXTRA_SUBJECT, "Smart Attendance - Login Help")
            })
        }
    }

    // ── Step 1: Login API ─────────────────────────────────────────────
    private fun loginWithBackend(studentId: String, password: String, btnLogin: MaterialButton) {

        RetrofitClient.create(this)
            .login(LoginRequest(userId = studentId, password = password))
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val token = response.body()!!.token

                        // Save JWT token immediately
                        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
                            .putString("jwt_token", token)
                            .putString("student_id", studentId)
                            .apply()

                        // FIX: Fetch profile BEFORE opening HomeActivity
                        // so Home always shows real name + photo on first load
                        btnLogin.text = "Loading profile..."
                        fetchProfileThenGoHome(btnLogin)

                    } else {
                        btnLogin.isEnabled = true
                        btnLogin.text      = "LOGIN"
                        handleLoginError(response)
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    btnLogin.text      = "LOGIN"
                    val msg = when (t) {
                        is java.net.SocketTimeoutException -> "Server timeout. Try again."
                        is java.net.UnknownHostException   -> "No internet connection."
                        else -> "Network error: ${t.message}"
                    }
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Step 2: Fetch profile, save it, then open Home ────────────────
    private fun fetchProfileThenGoHome(btnLogin: MaterialButton) {

        RetrofitClient.create(this)
            .getStudentProfile()
            .enqueue(object : Callback<StudentProfileResponse> {

                override fun onResponse(call: Call<StudentProfileResponse>, response: Response<StudentProfileResponse>) {

                    if (response.isSuccessful && response.body() != null) {
                        val p = response.body()!!.response

                        // Save all profile data to SharedPreferences
                        // HomeActivity reads from here — will have real data immediately
                        getSharedPreferences("profile_data", MODE_PRIVATE).edit().apply {
                            putString("name",       p.name)
                            putString("id",         p.userId)
                            putString("college",    p.collegeName)
                            putString("enrollment", p.enrollmentNo)
                            putString("email",      p.email)
                            putString("branch",     p.branch)
                            putString("semester",   p.semester)
                            putString("year",       p.year)
                            putString("class",      p.className)
                            putString("batch",      p.batch)
                            putString("image",      p.curImage)   // Full Cloudinary URL
                            putString("newImage",   p.newImage)
                            apply()
                        }

                        // Mark profile as loaded so ProfileActivity knows data exists
                        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
                            .putBoolean("profile_loaded", true)
                            .apply()
                    }
                    // Whether profile fetch succeeded or failed, go to Home
                    // Home will show real data if saved, or placeholder if fetch failed
                    goToHome()
                }

                override fun onFailure(call: Call<StudentProfileResponse>, t: Throwable) {
                    // Profile fetch failed (no internet etc.) — still go to Home
                    // Home will show placeholder, student can go to Profile to refresh
                    goToHome()
                }
            })
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    // ── Error handling ────────────────────────────────────────────────
    private fun handleLoginError(response: Response<*>) {
        try {
            val errorJson = response.errorBody()?.string()
            if (errorJson != null) {
                val err = gson.fromJson(errorJson, ErrorResponse::class.java)
                val message = when (err.error) {
                    "USER_NOT_FOUND"    -> "Student ID not found"
                    "WRONG_PASSWORD"    -> "Incorrect password"
                    "TEMPORARY_BLOCKED" -> "Account temporarily blocked. Try later."
                    else                -> "Login failed. Please try again."
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Forgot password ───────────────────────────────────────────────
    private fun showForgotPasswordBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view   = layoutInflater.inflate(R.layout.bottomsheet_forgot_password, null)
        dialog.setContentView(view)

        val etInput  = view.findViewById<EditText>(R.id.etResetInput)
        val btnSend  = view.findViewById<MaterialButton>(R.id.btnSendLink)
        val tvResult = view.findViewById<TextView>(R.id.tvResultMessage)

        btnSend.setOnClickListener {
            val input = etInput.text.toString().trim()
            etInput.error      = null
            tvResult.visibility = View.GONE

            if (input.isEmpty()) {
                etInput.error = "Student ID or Email required"
                return@setOnClickListener
            }

            // TODO: replace with real backend API call
            tvResult.visibility = View.VISIBLE
            tvResult.text = "If this ID/email is registered, a reset link will be sent."
            tvResult.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            android.os.Handler(mainLooper).postDelayed({ dialog.dismiss() }, 2000)
        }

        dialog.show()
    }
}