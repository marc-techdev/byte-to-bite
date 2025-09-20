package com.example.testdesign

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.testdesign.databinding.ActivityLoaderBinding
import com.example.testdesign.onboarding.OnboardingSeenLocal

/**
 * Small splash/loader that decides where to go.
 * Shows your existing Lottie, then routes to Onboarding or Main.
 */
class LoaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: give the animation a short moment, then navigate.
        binding.loaderAnimation.postDelayed({
            val gate = OnboardingSeenLocal(this)
            if (gate.shouldShow()) {
                startActivity(Intent(this, com.example.testdesign.onboarding.OnboardingActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 3000) // tweak delay if you want
    }
}
