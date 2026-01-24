package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.yalantis.ucrop.UCrop
import java.io.File

class ProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var imgProfile: ShapeableImageView
    private lateinit var imgCameraIcon: ImageView
    private lateinit var tvPendingBadge: ImageView

    // State
    private var pendingPhotoUri: Uri? = null

    // ================= IMAGE PICK =================

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { startCrop(it) }
        }

    private val cropImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    imgProfile.setImageURI(it)
                    pendingPhotoUri = it
                    savePhotoPending(true)
                    showPendingUI()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Views
        imgProfile = findViewById(R.id.imgProfile)
        imgCameraIcon = findViewById(R.id.imgCameraIcon)
        tvPendingBadge = findViewById(R.id.tvPendingBadge)

        val btnChangePassword = findViewById<MaterialButton>(R.id.btnChangePassword)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Restore pending state
        if (isPhotoPending()) showPendingUI()

        // Bottom navigation
        bottomNavigation.selectedItemId = R.id.nav_profile
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    finish()
                    true
                }
                else -> false
            }
        }

        // Profile image click
        imgProfile.setOnClickListener {
            handleProfileImageClick()
        }

        // Camera icon click
        imgCameraIcon.setOnClickListener {
            handleProfileImageClick()
        }

        // Pending badge click
        tvPendingBadge.setOnClickListener {
            showPendingBottomSheet()
        }

        // Change password
        btnChangePassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(intent, options.toBundle())
        }

        // Logout
        btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    getSharedPreferences("student_session", MODE_PRIVATE)
                        .edit().clear().apply()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ================= CLICK HANDLER =================

    private fun handleProfileImageClick() {
        if (isPhotoPending()) {
            showPendingBottomSheet()
        } else {
            pickImage.launch("image/*")
        }
    }

    // ================= UCROP =================

    private fun startCrop(sourceUri: Uri) {
        val destinationUri =
            Uri.fromFile(File(cacheDir, "crop_${System.currentTimeMillis()}.jpg"))

        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropFrame(false)
            setShowCropGrid(false)
            setToolbarTitle("Edit Photo")
            setToolbarColor(Color.parseColor("#1B263B"))
            setStatusBarColor(Color.parseColor("#1B263B"))
            setRootViewBackgroundColor(Color.BLACK)
        }

        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .getIntent(this)

        cropImage.launch(intent)
    }

    // ================= UI STATES =================

    private fun showPendingUI() {
        imgCameraIcon.visibility = View.GONE
        tvPendingBadge.visibility = View.VISIBLE
    }

    private fun resetToCameraState() {
        imgProfile.setImageResource(R.drawable.profile_temp)
        imgCameraIcon.visibility = View.VISIBLE
        tvPendingBadge.visibility = View.GONE
        savePhotoPending(false)
        pendingPhotoUri = null
    }

    // ================= BOTTOM SHEET =================

    private fun showPendingBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_pending_photo, null)
        bottomSheet.setContentView(view)

        val imgPreview = view.findViewById<ImageView>(R.id.imgPreview)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelChange)
        val btnReupload = view.findViewById<MaterialButton>(R.id.btnReupload)
        val btnClose = view.findViewById<TextView>(R.id.btnClose)

        pendingPhotoUri?.let { imgPreview.setImageURI(it) }

        btnCancel.setOnClickListener {
            resetToCameraState()
            bottomSheet.dismiss()
        }

        btnReupload.setOnClickListener {
            bottomSheet.dismiss()
            pickImage.launch("image/*")
        }

        btnClose.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    // ================= STATE STORAGE =================

    private fun savePhotoPending(pending: Boolean) {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("photo_pending", pending)
            .apply()
    }

    private fun isPhotoPending(): Boolean {
        return getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .getBoolean("photo_pending", false)
    }
}
