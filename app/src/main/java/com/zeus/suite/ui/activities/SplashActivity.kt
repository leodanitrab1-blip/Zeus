package com.zeus.suite.ui.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.zeus.suite.R
import com.zeus.suite.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeAnimations()
        navigateToMain()
    }

    private fun initializeAnimations() {
        // Animacion de entrada del logo
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        fadeInAnimation.duration = 1500
        fadeInAnimation.interpolator = AccelerateDecelerateInterpolator()
        binding.zeusLogo.startAnimation(fadeInAnimation)

        // Animacion del rayo de Zeus
        ObjectAnimator.ofFloat(binding.zeusBolt, View.ALPHA, 0f, 1f).apply {
            duration = 2000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Animacion del texto del titulo
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        slideUpAnimation.duration = 1200
        slideUpAnimation.startOffset = 300
        binding.titleText.startAnimation(slideUpAnimation)

        // Animacion del subtitulo
        val fadeInSubtitle = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeInSubtitle.duration = 1000
        fadeInSubtitle.startOffset = 800
        binding.subtitleText.startAnimation(fadeInSubtitle)
    }

    private fun navigateToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            
            startActivity(intent)
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            finish()
        }, 3000) // 3 segundos de splash
    }

    override fun onBackPressed() {
        // Bloquear el boton de retroceso durante el splash
    }
}