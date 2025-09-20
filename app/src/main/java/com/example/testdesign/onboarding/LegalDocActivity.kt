package com.example.testdesign.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.testdesign.R
import com.google.android.material.appbar.MaterialToolbar

class LegalDocActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal_doc)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val web     = findViewById<WebView>(R.id.webView)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.tldr_title)
        val asset = intent.getStringExtra(EXTRA_ASSET_PATH)
        val html  = intent.getStringExtra(EXTRA_BODY) // optional fallback

        toolbar.title = title
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Basic safe WebView defaults for local content
        web.webViewClient = WebViewClient()
        web.settings.javaScriptEnabled = false
        web.settings.domStorageEnabled = false
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false

        when {
            !asset.isNullOrBlank() ->
                web.loadUrl("file:///android_asset/$asset")
            !html.isNullOrBlank()  ->
                web.loadDataWithBaseURL(
                    "file:///android_asset/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            else -> {
                // As a last resort, show a tiny stub page
                web.loadData("<html><body><p>No document found.</p></body></html>",
                    "text/html", "UTF-8")
            }
        }

        onBackPressedDispatcher.addCallback(this) { finish() }
    }

    companion object {
        private const val EXTRA_TITLE      = "extra_title"
        private const val EXTRA_BODY       = "extra_body"
        private const val EXTRA_ASSET_PATH = "extra_asset"

        fun startWithAsset(context: Context, title: String, asset: String) {
            context.startActivity(
                Intent(context, LegalDocActivity::class.java)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_ASSET_PATH, asset)
            )
        }

        // Optional fallback if you ever want to pass raw HTML:
        fun startWithHtml(context: Context, title: String, html: String) {
            context.startActivity(
                Intent(context, LegalDocActivity::class.java)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_BODY, html)
            )
        }
    }
}
