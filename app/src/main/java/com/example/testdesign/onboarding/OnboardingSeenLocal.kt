package com.example.testdesign.onboarding

import android.content.Context
import android.content.SharedPreferences

class OnboardingSeenLocal(ctx: Context) {

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("bb_prefs", Context.MODE_PRIVATE)

    fun shouldShow(): Boolean = !prefs.getBoolean(KEY, false)

    fun markSeen() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        private const val KEY = "onb_seen_v1"
    }
}
