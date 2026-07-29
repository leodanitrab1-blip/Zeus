package com.zeus.suite.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zeus.suite.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.zeusLogo)
        val bolt = findViewById<ImageView>(R.id.zeusBolt)
        val title = findViewById<TextView>(R.id.titleText)

        // Animacion del logo con escalado
        val scaleAnim = ScaleAnimation(
            0.3f, 1.0f,
            0.3f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnim.duration = 1200
        scaleAnim.fillAfter = true
        logo.startAnimation(scaleAnim)

        // Animacion del rayo con parpadeo
        val boltAnim = AlphaAnimation(0.2f, 1.0f)
        boltAnim.duration = 600
        boltAnim.repeatMode = Animation.REVERSE
        boltAnim.repeatCount = Animation.INFINITE
        bolt.startAnimation(boltAnim)

        // Animacion del titulo desde abajo
        val slideAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        slideAnim.duration = 1000
        title.startAnimation(slideAnim)

        // Navegar al main
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3000)
    }
}