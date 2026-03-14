package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smartattendance.student.models.ChangePasswordRequest
import com.smartattendance.student.network.RetrofitClient

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // TextInputLayouts
        val tilOld = findViewById<TextInputLayout>(R.id.tilOldPassword)
        val tilNew = findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirm = findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        // EditTexts
        val etOld = findViewById<TextInputEditText>(R.id.etOldPassword)
        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirm = findViewById<TextInputEditText>(R.id.etConfirmPassword)

        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdatePassword)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Bottom nav (no tab selected)
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

        btnUpdate.setOnClickListener {

            // Clear old errors
            tilOld.error = null
            tilNew.error = null
            tilConfirm.error = null

            val oldPass = etOld.text.toString().trim()
            val newPass = etNew.text.toString().trim()
            val confirmPass = etConfirm.text.toString().trim()

            when {
                oldPass.isEmpty() -> tilOld.error = "Old password is required"
                newPass.isEmpty() -> tilNew.error = "New password is required"
                newPass.length < 6 -> tilNew.error = "Minimum 6 characters required"
                confirmPass.isEmpty() -> tilConfirm.error = "Please confirm password"
                newPass != confirmPass -> tilConfirm.error = "Passwords do not match"
                else -> {

                    btnUpdate.isEnabled = false
                    btnUpdate.text = "Updating..."

                    val request = ChangePasswordRequest(
                        password = oldPass,
                        newPassword = newPass,
                        confirmPassword = confirmPass
                    )

                    RetrofitClient.create(this)
                        .changePassword(request)
                        .enqueue(object : retrofit2.Callback<Map<String, String>> {

                            override fun onResponse(
                                call: retrofit2.Call<Map<String, String>>,
                                response: retrofit2.Response<Map<String, String>>
                            ) {

                                btnUpdate.isEnabled = true
                                btnUpdate.text = "Update Password"

                                if (response.isSuccessful) {

                                    MaterialAlertDialogBuilder(this@ChangePasswordActivity)
                                        .setTitle("Password Changed")
                                        .setMessage("Your password has been updated successfully.\nPlease login again.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK") { _, _ ->

                                            // Clear session
                                            getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                                .edit().clear().apply()

                                            getSharedPreferences("profile_data", MODE_PRIVATE)
                                                .edit().clear().apply()

                                            // Navigate to Login (clear back stack)
                                            val intent = Intent(this@ChangePasswordActivity, LoginActivity::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                        .show()

                                } else {

                                    val errorBody = response.errorBody()?.string()

                                    when {
                                        errorBody?.contains("WRONG_PASSWORD") == true -> {
                                            tilOld.error = "Incorrect old password"
                                        }

                                        errorBody?.contains("BOTH_PASSWORD_SHOULD_BE_SAME") == true -> {
                                            tilConfirm.error = "Passwords do not match"
                                        }

                                        errorBody?.contains("ALL_FIELD_REQUIRED") == true -> {
                                            com.google.android.material.snackbar.Snackbar
                                                .make(btnUpdate,
                                                    "All fields are required",
                                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                                ).show()
                                        }

                                        else -> {
                                            com.google.android.material.snackbar.Snackbar
                                                .make(btnUpdate,
                                                    "Unable to change password",
                                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                                ).show()
                                        }
                                    }
                                }
                            }

                            override fun onFailure(
                                call: retrofit2.Call<Map<String, String>>,
                                t: Throwable
                            ) {

                                btnUpdate.isEnabled = true
                                btnUpdate.text = "Update Password"

                                val msg = when {
                                    t is java.net.SocketTimeoutException ->
                                        "Server timeout"
                                    t is java.net.UnknownHostException ->
                                        "Server unreachable"
                                    else ->
                                        "Something went wrong"
                                }

                                com.google.android.material.snackbar.Snackbar
                                    .make(btnUpdate, msg,
                                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                    ).show()
                            }
                        })
                }
            }
        }
    }
}
