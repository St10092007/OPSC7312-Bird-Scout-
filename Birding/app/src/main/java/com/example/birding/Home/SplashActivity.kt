package com.example.birding.Home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.birding.R


val SPLASH_DELAY: Long = 5000 // 5 seconds

class SplashActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImageView = findViewById<ImageView>(R.id.logoImageView)


        Handler().postDelayed({
            logoImageView.visibility = View.GONE
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, SPLASH_DELAY)
    }
}