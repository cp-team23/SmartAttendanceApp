package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.smartattendance.student.models.StudentProfileResponse
import com.smartattendance.student.models.StudentData
import com.smartattendance.student.network.RetrofitClient
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgProfile: ShapeableImageView
    private lateinit var imgCameraIcon: ImageView
    private lateinit var tvPendingBadge: ImageView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentId: TextView

    private lateinit var tvCollege: TextView
    private lateinit var tvEnrollment: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBranch: TextView
    private lateinit var tvSemester: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvClass: TextView
    private lateinit var tvBatch: TextView
    private val BASE_URL = "https://mdj4kwmp-8080.inc1.devtunnels.ms"
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var profileImageLoader: View
    private var shouldRefresh = false
    private lateinit var viewRejectDot: View


    private var pendingPhotoUri: Uri? = null

    // ================= IMAGE PICK =================

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let { uri -> startCrop(uri) }
        }

    private val cropImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = UCrop.getOutput(it.data ?: return@registerForActivityResult)
            uri?.let {

                val jpgUri = convertToJpg(it) ?: it

                pendingPhotoUri = jpgUri
                savePhotoUri(jpgUri)
                savePhotoPending(true)
                showPendingUI()

                uploadProfileImage(jpgUri)   // send JPG to backend
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()

        loadSavedProfile()
        restoreProfileImage()

        loadStudentProfile()



        setupBottomNavigation()
        setupButtons()

        val rejected = getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .getBoolean("photo_rejected", false)

        if (rejected) {
            viewRejectDot.visibility = View.VISIBLE
        }

        if (isPhotoPending()) showPendingUI()
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefresh) {
            loadStudentProfile()
            shouldRefresh = false
        }
    }

    private fun initViews() {
        imgProfile = findViewById(R.id.imgProfile)
        imgCameraIcon = findViewById(R.id.imgCameraIcon)
        tvPendingBadge = findViewById(R.id.tvPendingBadge)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentId = findViewById(R.id.tvStudentId)

        tvCollege = findViewById(R.id.tvCollege)
        tvEnrollment = findViewById(R.id.tvEnrollment)
        tvEmail = findViewById(R.id.tvEmail)
        tvBranch = findViewById(R.id.tvBranch)
        tvSemester = findViewById(R.id.tvSemester)
        tvYear = findViewById(R.id.tvYear)
        tvClass = findViewById(R.id.tvClass)
        tvBatch = findViewById(R.id.tvBatch)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        profileImageLoader = findViewById(R.id.profileImageLoader)
        viewRejectDot = findViewById(R.id.viewRejectDot)

        swipeRefresh.setOnRefreshListener {
            loadStudentProfile()
        }
        imgProfile.setImageResource(R.drawable.profile_temp)
        imgProfile.setOnClickListener { handleProfileClick() }
        imgCameraIcon.setOnClickListener { handleProfileClick() }
        tvPendingBadge.setOnClickListener {
            if (isPhotoPending()) {
                showPendingBottomSheet()
            }
        }

    }

    // ================= PROFILE API =================

    private fun loadStudentProfile(showLoader: Boolean = false) {

        if (showLoader) {
            swipeRefresh.isRefreshing = true
        }

        RetrofitClient.create(this).getStudentProfile()
            .enqueue(object : Callback<StudentProfileResponse> {

                override fun onResponse(
                    call: Call<StudentProfileResponse>,
                    response: Response<StudentProfileResponse>
                ) {

                    swipeRefresh.isRefreshing = false

                    if (!response.isSuccessful || response.body() == null) return

                    val p = response.body()!!.response

                    setProfileData(p)
                    saveProfile(p)
                    markProfileLoaded()
                }

                override fun onFailure(call: Call<StudentProfileResponse>, t: Throwable) {

                    swipeRefresh.isRefreshing = false

                    val msg = when {
                        t is java.net.SocketTimeoutException ->
                            "Server taking too long to respond"

                        t is java.net.UnknownHostException ->
                            "Server unreachable"

                        t is java.io.IOException ->
                            "Check your internet connection"

                        else ->
                            "Something went wrong"
                    }

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        msg,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun setProfileData(p: StudentData) {

        tvStudentName.text = p.name
        tvStudentId.text = "Student ID : ${p.userId}"

        tvCollege.text = p.collegeName
        tvEnrollment.text = p.enrollmentNo
        tvEmail.text = p.email
        tvBranch.text = p.branch
        tvSemester.text = p.semester
        tvYear.text = p.year
        tvClass.text = p.className
        tvBatch.text = p.batch

        val prefs = getSharedPreferences("profile_data", MODE_PRIVATE)

        // ================= IMAGE =================
        // ALWAYS show approved image in profile circle

        if (!p.curImage.isNullOrEmpty() && p.curImage != "/uploads/null") {

            val imageUrl = BASE_URL + p.curImage

            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(imgProfile.drawable)
                .error(R.drawable.profile_temp)
                .dontAnimate()
                .into(imgProfile)

        } else {
            imgProfile.setImageResource(R.drawable.profile_temp)
        }

        // Reset camera icon color
        imgCameraIcon.clearColorFilter()

        // ================= PENDING STATUS =================

        val isPending =
            !p.newImage.isNullOrEmpty() &&
                    p.newImage != "/uploads/null"

        if (isPending) {

            showPendingUI()
            savePhotoPending(true)

            prefs.edit()
                .putBoolean("wasPending", true)
                .putString("lastApprovedImage", p.curImage)
                .apply()

        } else {

            hidePendingUI()
            savePhotoPending(false)

            // clear pending uri after approval or cancel
            clearSavedPhotoUri()
            pendingPhotoUri = null
        }

        // ================= REJECTION DETECTION =================

        val oldPending = prefs.getBoolean("wasPending", false)
        val oldApprovedImage = prefs.getString("lastApprovedImage", null)

        val nowPending =
            !p.newImage.isNullOrEmpty() &&
                    p.newImage != "/uploads/null"

        val approvedChanged = oldApprovedImage != p.curImage

        // REJECTED if pending was there but now removed and approved image didn't change
        if (oldPending && !nowPending && !approvedChanged) {

            getSharedPreferences("profile_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("photo_rejected", true)
                .apply()

            viewRejectDot.visibility = View.VISIBLE

            Snackbar.make(
                findViewById(android.R.id.content),
                "Photo rejected. Tap camera to reupload.",
                Snackbar.LENGTH_LONG
            ).show()
        }

        prefs.edit()
            .putBoolean("wasPending", nowPending)
            .putString("lastApprovedImage", p.curImage)
            .apply()
    }
    // ================= LOCAL STORAGE =================

    private fun saveProfile(p: StudentData) {
        val pref = getSharedPreferences("profile_data", MODE_PRIVATE).edit()

        pref.putString("name", p.name)
        pref.putString("id", p.userId)
        pref.putString("college", p.collegeName)
        pref.putString("enrollment", p.enrollmentNo)
        pref.putString("email", p.email)
        pref.putString("branch", p.branch)
        pref.putString("semester", p.semester)
        pref.putString("year", p.year)
        pref.putString("class", p.className)
        pref.putString("batch", p.batch)
        pref.putString("image", p.curImage)
        pref.putString("newImage", p.newImage)
        pref.putString("lastApprovedImage", p.curImage)


        pref.apply()
    }

    private fun loadSavedProfile() {
        val pref = getSharedPreferences("profile_data", MODE_PRIVATE)

        tvStudentName.text = pref.getString("name", "Student Name")
        tvStudentId.text = "Student ID : " + pref.getString("id", "")

        tvCollege.text = pref.getString("college", "")
        tvEnrollment.text = pref.getString("enrollment", "")
        tvEmail.text = pref.getString("email", "")
        tvBranch.text = pref.getString("branch", "")
        tvSemester.text = pref.getString("semester", "")
        tvYear.text = pref.getString("year", "")
        tvClass.text = pref.getString("class", "")
        tvBatch.text = pref.getString("batch", "")

        // load cached profile image
        val imageUrl = pref.getString("image", null)
        if (!imageUrl.isNullOrEmpty() && imageUrl != "/uploads/null") {
            Glide.with(this)
                .load(BASE_URL + imageUrl)
                .dontAnimate()
                .error(R.drawable.profile_temp)
                .into(imgProfile)
        }
        else {
            imgProfile.setImageResource(R.drawable.profile_temp)
        }

    }


    private fun isProfileNotLoaded(): Boolean {
        return !getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getBoolean("profile_loaded", false)
    }

    private fun markProfileLoaded() {
        getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("profile_loaded", true)
            .apply()
    }

    private fun convertToJpg(uri: Uri): Uri? {
        return try {

            val bitmap = contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it)
            }

            val file = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")

            file.outputStream().use { out ->
                bitmap?.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    90,   // quality
                    out
                )
            }

            Uri.fromFile(file)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uploadProfileImage(uri: Uri) {

        profileImageLoader.visibility = View.VISIBLE
        imgProfile.alpha = 0.5f
        imgProfile.isEnabled = false

        val file = File(uri.path!!)
        val requestFile =
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())

        val body =
            MultipartBody.Part.createFormData("image", "profile.jpg", requestFile)

        RetrofitClient.create(this)
            .uploadProfileImage(body)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {

                    profileImageLoader.visibility = View.GONE
                    imgProfile.alpha = 1f
                    imgProfile.isEnabled = true

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Photo sent for approval",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                    profileImageLoader.visibility = View.GONE
                    imgProfile.alpha = 1f
                    imgProfile.isEnabled = true

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Upload failed. Check connection.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            })
    }

    // ================= NAVIGATION =================

    private fun setupBottomNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.selectedItemId = R.id.nav_profile

        nav.setOnItemSelectedListener {
            if (it.itemId == R.id.nav_home) {
                startActivity(
                    Intent(this, HomeActivity::class.java),
                    ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
                )
                finish()
            }
            true
        }
    }

    // ================= BUTTONS =================

    private fun setupButtons() {

        findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        btnLogout.setOnClickListener {

            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->

                    // 🔒 Disable button AFTER confirmation
                    btnLogout.isEnabled = false
                    btnLogout.text = "Logging out..."

                    logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ================= LOGOUT =================

    private fun logout() {
        RetrofitClient.create(this).logout()
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    clearSessionAndExit()
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    clearSessionAndExit()
                }
            })
    }

    private fun clearSessionAndExit() {
        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("profile_data", MODE_PRIVATE).edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ================= IMAGE HANDLING =================

    private fun handleProfileClick() {

        val rejected = getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .getBoolean("photo_rejected", false)

        if (rejected) {
            showRejectionBottomSheet()
            return
        }

        if (isPhotoPending()) {
            showPendingBottomSheet()
        } else {
            pickImage.launch("image/*")
        }
    }

    private fun startCrop(uri: Uri) {
        val dest = Uri.fromFile(File(cacheDir, "crop.jpg"))

        val options = UCrop.Options().apply {

            setCircleDimmedLayer(true)
            setShowCropFrame(false)
            setShowCropGrid(false)
            setToolbarTitle("Edit Photo")
            setToolbarColor(Color.parseColor("#1B263B"))
            setStatusBarColor(Color.parseColor("#1B263B"))
            setToolbarWidgetColor(Color.WHITE)
            setActiveControlsWidgetColor(Color.parseColor("#1B263B"))
            setRootViewBackgroundColor(Color.parseColor("#121212"))
        }

        val intent = UCrop.of(uri, dest)
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .getIntent(this)

        cropImage.launch(intent)
    }

    private fun showPendingUI() {

        imgCameraIcon.visibility = View.GONE

        tvPendingBadge.scaleX = 0f
        tvPendingBadge.scaleY = 0f
        tvPendingBadge.visibility = View.VISIBLE

        tvPendingBadge.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }

    private fun hidePendingUI() {
        imgCameraIcon.visibility = View.VISIBLE
        tvPendingBadge.visibility = View.GONE
    }

    private fun showRejectionBottomSheet() {

        val sheet = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.bottomsheet_pending_photo, null)
        sheet.setContentView(v)

        val btnReupload = v.findViewById<MaterialButton>(R.id.btnReupload)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancelChange)

        btnReupload.text = "Reupload Photo"
        btnCancel.text = "Dismiss"

        btnReupload.setOnClickListener {
            sheet.dismiss()

            // clear rejection state
            getSharedPreferences("profile_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("photo_rejected", false)
                .apply()

            viewRejectDot.visibility = View.GONE

            pickImage.launch("image/*")
        }

        btnCancel.setOnClickListener {
            sheet.dismiss()

            // clear rejection state
            getSharedPreferences("profile_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("photo_rejected", false)
                .apply()

            viewRejectDot.visibility = View.GONE
        }

        sheet.show()
    }
    private fun showPendingBottomSheet() {

        val sheet = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.bottomsheet_pending_photo, null)
        sheet.setContentView(v)

        val preview = v.findViewById<ImageView>(R.id.imgPreview)
        val btnReupload = v.findViewById<MaterialButton>(R.id.btnReupload)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancelChange)
        val btnClose = v.findViewById<TextView>(R.id.btnClose)

        if (pendingPhotoUri != null) {
            preview.setImageURI(pendingPhotoUri)
        } else {

            val pref = getSharedPreferences("profile_data", MODE_PRIVATE)
            val pendingServerImage = pref.getString("newImage", null)

            if (!pendingServerImage.isNullOrEmpty() && pendingServerImage != "/uploads/null") {
                val url = "https://mdj4kwmp-8080.inc1.devtunnels.ms" + pendingServerImage
                Glide.with(this).load(url).into(preview)
            }
        }


        // reupload
        btnReupload.setOnClickListener {
            sheet.dismiss()
            pickImage.launch("image/*")
        }

        // ✅ CANCEL CHANGE
        btnCancel.setOnClickListener {

            RetrofitClient.create(this).deleteImageRequest()
                .enqueue(object : Callback<Map<String, String>> {

                    override fun onResponse(
                        call: Call<Map<String, String>>,
                        response: Response<Map<String, String>>
                    ) {

                        savePhotoPending(false)
                        clearSavedPhotoUri()
                        pendingPhotoUri = null

                        // reload profile to restore old approved image
                        loadStudentProfile()

                        sheet.dismiss()
                    }

                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {

                        savePhotoPending(false)
                        clearSavedPhotoUri()
                        pendingPhotoUri = null

                        loadStudentProfile()

                        sheet.dismiss()
                    }
                })
        }

        btnClose.setOnClickListener { sheet.dismiss() }

        sheet.show()
    }


    private fun savePhotoPending(b: Boolean) {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .edit().putBoolean("photo_pending", b).apply()
    }

    private fun isPhotoPending() =
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .getBoolean("photo_pending", false)

    private fun savePhotoUri(uri: Uri) {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .edit().putString("photo_uri", uri.toString()).apply()
    }

    private fun clearSavedPhotoUri() {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .edit()
            .remove("photo_uri")
            .apply()
    }

    private fun restoreProfileImage() {
        val s = getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .getString("photo_uri", null)

        if (s != null) {
            pendingPhotoUri = Uri.parse(s)
        }
    }
}