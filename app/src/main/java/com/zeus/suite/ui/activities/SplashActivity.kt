package com.zeus.suite.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
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
        val subtitle = findViewById<TextView>(R.id.subtitleText)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        fadeIn.duration = 1500
        logo.startAnimation(fadeIn)

        val boltAnim = AnimationUtils.loadAnimation(this, R.anim.bolt_flash)
        boltAnim.duration = 2000
        bolt.startAnimation(boltAnim)

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        slideUp.duration = 1200
        slideUp.startOffset = 300
        title.startAnimation(slideUp)

        val fadeInSub = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeInSub.duration = 1000
        fadeInSub.startOffset = 800
        subtitle.startAnimation(fadeInSub)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3000)
    }
}