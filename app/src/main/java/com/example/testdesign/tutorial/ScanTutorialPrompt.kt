package com.example.testdesign.tutorial

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * One-time prompt asking whether the user wants to watch a tutorial.
 * - "Watch" -> opens the bottom sheet (forced).
 * - "No thanks" -> mark seen (never ask again).
 * - "Maybe later" -> don't mark; will ask again next launch.
 */
@UnstableApi
object ScanTutorialPrompt {
    private const val TAG = "scan_tutorial_prompt"

    fun maybeShow(host: FragmentActivity, source: VideoSource) {
        if (ScanHelpSeenLocal.isSeen(host)) return
        val fm = host.supportFragmentManager
        // Avoid duplicates if prompt or sheet already visible
        if (fm.findFragmentByTag(TAG) != null) return
        if (fm.findFragmentByTag(ScanTutorialBottomSheet.TAG) != null) return
        PromptDialog.new(source).show(fm, TAG)
    }

    @UnstableApi
    class PromptDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val a = requireArguments()
            val type = a.getString("type") ?: "none"
            val vRes = a.getInt("videoRes")
            val sRes = a.getInt("subRes")
            val url  = a.getString("url")
            val sUrl = a.getString("subUrl")
            val host = requireActivity()

            return MaterialAlertDialogBuilder(host)
                .setTitle("Watch a quick tutorial?")
                .setMessage("Learn how to scan ingredients and find recipes in under 30 seconds.")
                .setPositiveButton("Watch") { _, _ ->
                    when (type) {
                        "raw" -> ScanTutorialBottomSheet.showRaw(host, vRes, sRes, startMuted = true, force = true)
                        "url" -> ScanTutorialBottomSheet.showUrl(host, url.orEmpty(), sUrl, startMuted = true, force = true)
                        else  -> ScanTutorialBottomSheet.show(host, force = true)
                    }
                }
                .setNegativeButton("No thanks") { _, _ ->
                    ScanHelpSeenLocal.markSeen(host)
                }
                .setNeutralButton("Maybe later", null)
                .create()
        }

        companion object {
            fun new(source: VideoSource): PromptDialog {
                val args = Bundle().apply {
                    when (source) {
                        is VideoSource.RawResSource -> {
                            putString("type", "raw")
                            putInt("videoRes", source.videoResId)
                            putInt("subRes", source.subtitleResId ?: 0)
                        }
                        is VideoSource.UrlSource -> {
                            putString("type", "url")
                            putString("url", source.url)
                            putString("subUrl", source.subtitleUrl)
                        }
                        VideoSource.None -> putString("type", "none")
                    }
                }
                return PromptDialog().also { it.arguments = args }
            }
        }
    }
}
