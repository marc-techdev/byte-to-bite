package com.example.testdesign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.testdesign.onboarding.LegalDocActivity

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_about, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // External learn-more link
        view.findViewById<Button>(R.id.btnLearnMore)?.setOnClickListener {
            val url = "https://en.wikipedia.org/wiki/Filipino_cuisine"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Open local HTML (assets/terms_privacy.html) in our in-app WebView screen
        view.findViewById<View>(R.id.btnTermsPrivacy)?.setOnClickListener {
            LegalDocActivity.startWithAsset(
                context = requireContext(),
                title   = getString(R.string.tldr_title),   // e.g., "Terms, Privacy & Disclaimers"
                asset   = "terms_privacy.html"
            )
        }
    }
}
