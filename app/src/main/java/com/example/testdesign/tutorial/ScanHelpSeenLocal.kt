package com.example.testdesign.tutorial

import android.content.Context
import androidx.core.content.edit

/**
 * Simple "seen once" flag for the scan tutorial.
 */
object ScanHelpSeenLocal {
    private const val PREFS = "scan_tutorial_prefs"
    private const val KEY   = "scan_tutorial_seen_v1"

    fun isSeen(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    fun markSeen(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY, true) }
    }

    // handy for testing
    fun reset(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(KEY) }
    }
}
