package com.example.testdesign.onboarding

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.testdesign.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TermsPrivacyDialogFragment : DialogFragment() {

    interface Listener {
        fun onPolicyAccepted()
        fun onPolicyDeclined()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // HARD lock: cannot dismiss with back or outside
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_terms_privacy, null, false)

        val cbAgree       = v.findViewById<MaterialCheckBox>(R.id.cbAgree)
        val cbUnderstand  = v.findViewById<MaterialCheckBox>(R.id.cbUnderstand)
        val btnContinue   = v.findViewById<MaterialButton>(R.id.btnContinue)
        val btnCancel     = v.findViewById<MaterialButton>(R.id.btnCancel)
        val btnViewLegal  = v.findViewById<MaterialButton>(R.id.btnViewLegal)

        fun updateContinue() {
            btnContinue.isEnabled = cbAgree.isChecked && cbUnderstand.isChecked
        }
        cbAgree.setOnCheckedChangeListener { _, _ -> updateContinue() }
        cbUnderstand.setOnCheckedChangeListener { _, _ -> updateContinue() }
        updateContinue()

        btnViewLegal.setOnClickListener {
            // Open the in-app HTML (assets/terms_privacy.html)
            LegalDocActivity.startWithAsset(
                context = requireContext(),
                title   = getString(R.string.tldr_title),
                asset   = "terms_privacy.html"
            )
        }

        btnCancel.setOnClickListener {
            (activity as? Listener)?.onPolicyDeclined()
            // No fall-through: activity will finish
            dismissAllowingStateLoss()
        }

        btnContinue.setOnClickListener {
            (activity as? Listener)?.onPolicyAccepted()
            dismissAllowingStateLoss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(v)
            .create()
            .apply {
                // Block outside-tap and BACK key (including back gesture)
                setCanceledOnTouchOutside(false)
                setOnKeyListener { _, keyCode, _ ->
                    keyCode == KeyEvent.KEYCODE_BACK // consume BACK always
                }
            }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Safety: if user somehow dismissed without accepting, force re-show via activity
        val act = activity as? OnboardingActivity ?: return
        if (!act.policyAcceptedHard()) {
            act.forceShowPolicyGate()
        }
    }

    companion object {
        const val TAG = "policy"
        fun newInstance() = TermsPrivacyDialogFragment()
    }
}
