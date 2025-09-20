package com.example.testdesign.onboarding

import android.content.Context
import androidx.core.content.edit

class FirstRunPolicyLocal(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("policy_local", Context.MODE_PRIVATE)

    fun accepted(): Boolean = prefs.getBoolean("accepted", false)
    fun isAccepted(): Boolean = accepted()                // alias for convenience
    fun setAccepted(value: Boolean) = prefs.edit { putBoolean("accepted", value) }
    fun markAccepted() = setAccepted(true)                // optional helper
}
