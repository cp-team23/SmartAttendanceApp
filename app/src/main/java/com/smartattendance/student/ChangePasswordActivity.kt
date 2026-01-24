package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
                    // Backend will be added later
                    finish()
                }
            }
        }
    }
}
