package com.smartattendance.student

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imgLogo   = findViewById<ImageView>(R.id.imgLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)

        imgLogo.alpha   = 0f
        tvAppName.alpha = 0f

        imgLogo.animate().alpha(1f).setDuration(1000)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        tvAppName.animate().alpha(1f).setDuration(1000)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        imgLogo.postDelayed({
            val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            val token = prefs.getString("jwt_token", null)

            if (!token.isNullOrEmpty()) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }

            overridePendingTransition(android.R.anim.fade_in, 0)
            finish()
        }, 2000)
    }
}
