package com.example.testdesign.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.example.testdesign.MainActivity
import com.example.testdesign.R
import com.example.testdesign.databinding.ActivityOnboardingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

class OnboardingActivity : AppCompatActivity(), TermsPrivacyDialogFragment.Listener {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pages: List<OnboardingPage>
    private lateinit var policyLocal: FirstRunPolicyLocal

    // ==== CAMERA PERMISSION GATE (added) ====
    private val CAMERA_PERMISSION = Manifest.permission.CAMERA

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // User allowed camera → finish onboarding as usual
                finishOnboarding()
            } else {
                // Denied. Decide whether to show rationale or send to Settings.
                if (shouldShowRequestPermissionRationale(CAMERA_PERMISSION)) {
                    showCameraRationale()
                } else {
                    showGoToSettingsDialog()
                }
            }
        }
    // ========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        policyLocal = FirstRunPolicyLocal(this)

        // Ensure first layout pass measures the pill at full width
        binding.root.post { binding.btnNext.requestLayout() }

        pages = buildPages()

        val host = supportFragmentManager.findFragmentById(R.id.onboardingHost)
            ?: OnboardingHostFragment().also {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.onboardingHost, it)
                    .commitNow()
            }

        binding.viewPager.adapter = OnboardingPagerAdapter(host, pages)

        createDots(binding.dotStrip, pages.size)
        updateDots(binding.dotStrip, 0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val last = position == pages.lastIndex
                binding.btnNext.text = getString(
                    if (last) R.string.onb_get_started else R.string.onb_next
                )
                binding.btnPrev.isVisible = position > 0
                updateDots(binding.dotStrip, position)
            }
        })

        binding.btnPrev.setOnClickListener {
            val vp = binding.viewPager
            if (vp.currentItem > 0) vp.currentItem -= 1
        }

        // 🔁 Modified: last-page tap now requests CAMERA before proceeding
        binding.btnNext.setOnClickListener {
            val vp = binding.viewPager
            if (vp.currentItem == pages.lastIndex) {
                requestCameraThenProceed()
            } else {
                vp.currentItem += 1
            }
        }

        // Back key / gesture: NEVER finish onboarding, only go to previous page
        onBackPressedDispatcher.addCallback(this) {
            val gateVisible =
                supportFragmentManager.findFragmentByTag(TermsPrivacyDialogFragment.TAG) != null
            if (!policyLocal.accepted() || gateVisible) return@addCallback

            val vp = binding.viewPager
            if (vp.currentItem > 0) {
                vp.currentItem -= 1
            } // else: ignore back on page 0
        }

        // Show first-run policy gate (and freeze UI) if needed
        forceShowPolicyGate()
    }

    override fun onResume() {
        super.onResume()
        // If the dialog was killed by config change or something, keep enforcing
        if (!policyLocal.accepted()) forceShowPolicyGate()
    }

    /* ==== Terms dialog callbacks ==== */
    override fun onPolicyAccepted() {
        policyLocal.setAccepted(true)
        disableControls(false)
    }

    override fun onPolicyDeclined() {
        // User explicitly declined -> close app/flow
        finish()
    }

    /* ==== Helpers (called by fragment too) ==== */
    fun policyAcceptedHard(): Boolean = policyLocal.accepted()

    fun forceShowPolicyGate() {
        if (!policyLocal.accepted()) {
            disableControls(true)
            if (supportFragmentManager.findFragmentByTag(TermsPrivacyDialogFragment.TAG) == null) {
                TermsPrivacyDialogFragment.newInstance()
                    .show(supportFragmentManager, TermsPrivacyDialogFragment.TAG)
            }
        }
    }

    private fun disableControls(disabled: Boolean) {
        binding.viewPager.isUserInputEnabled = !disabled
        binding.btnNext.isEnabled = !disabled
        binding.btnPrev.isEnabled = !disabled
        val a = if (disabled) 0.5f else 1f
        binding.btnNext.alpha = a
        binding.btnPrev.alpha = a
    }

    // ====== Finish onboarding (unchanged) ======
    private fun finishOnboarding() {
        OnboardingSeenLocal(this).markSeen()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun buildPages(): List<OnboardingPage> = listOf(
        OnboardingPage(
            type = OnbType.WELCOME,
            imageRes = R.drawable.ig_calamansi,
            title = getString(R.string.onb_title_welcome),
            body  = getString(R.string.onb_body_welcome),
            bullets = listOf(
                OnboardingPage.Bullet(R.drawable.ic_camera,          getString(R.string.onb_bullet_w1)),
                OnboardingPage.Bullet(R.drawable.ic_group_recipes,   getString(R.string.onb_bullet_w2)),
                OnboardingPage.Bullet(R.drawable.ic_favorite_border, getString(R.string.onb_bullet_w3)),
            )
        ),
        OnboardingPage(
            type = OnbType.HOW,
            imageRes = 0,
            title = getString(R.string.onb_title_how),
            body  = "" // no gray subtitle
        ),
        OnboardingPage(
            type = OnbType.TIPS,
            imageRes = R.drawable.ic_onb_tips,
            title = getString(R.string.onb_title_tips),
            body  = getString(R.string.onb_body_tips),
            bullets = listOf(
                OnboardingPage.Bullet(R.drawable.ic_help_outline, getString(R.string.onb_bullet_t1)),
                OnboardingPage.Bullet(R.drawable.ic_info_home,    getString(R.string.onb_bullet_t2)),
                OnboardingPage.Bullet(R.drawable.ic_check_circle, getString(R.string.onb_bullet_t3)),
            ),
            showTipsCta = true
        ),
        OnboardingPage(
            type = OnbType.INGREDIENTS,
            imageRes = 0,
            title = getString(R.string.onb_title_supported_simple),
            body  = getString(R.string.onb_coverage_prefix),
            isIngredientList = true
        ),
        OnboardingPage(
            type = OnbType.PRIVACY,
            imageRes = R.drawable.ic_onb_privacy,
            title = getString(R.string.onb_title_privacy),
            body  = getString(R.string.onb_body_privacy),
            bullets = listOf(
                OnboardingPage.Bullet(R.drawable.ic_check_circle, getString(R.string.onb_bullet_p1)),
                OnboardingPage.Bullet(R.drawable.ic_check_circle, getString(R.string.onb_bullet_p2)),
            )
        )
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun createDots(container: LinearLayout, count: Int) {
        container.removeAllViews()
        repeat(count) {
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).also { lp ->
                    lp.marginStart = dp(6)
                    lp.marginEnd   = dp(6)
                }
                setImageResource(R.drawable.dot_unselected)
            }
            container.addView(iv)
        }
    }

    private fun updateDots(container: LinearLayout, activeIndex: Int) {
        for (i in 0 until container.childCount) {
            val iv = container.getChildAt(i) as ImageView
            iv.setImageResource(if (i == activeIndex) R.drawable.dot_selected else R.drawable.dot_unselected)
        }
    }

    // ========= CAMERA PERMISSION helpers (added) =========

    /** Called when “Get Started” is tapped on the last page. */
    private fun requestCameraThenProceed() {
        // If already granted, proceed immediately.
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            finishOnboarding()
            return
        }
        // Otherwise show the system permission dialog.
        cameraPermissionLauncher.launch(CAMERA_PERMISSION)
    }

    /** Explain why camera is needed; allow retry or skipping for now. */
    private fun showCameraRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Camera permission")
            .setMessage("We use the camera to scan your ingredients. You can still browse recipes without it.")
            .setPositiveButton("Allow") { _, _ -> cameraPermissionLauncher.launch(CAMERA_PERMISSION) }
            .setNegativeButton("Not now") { _, _ -> finishOnboarding() }
            .show()
    }

    /** User selected “Don’t ask again” or the prompt won’t show. Offer Settings. */
    private fun showGoToSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Enable camera in Settings")
            .setMessage("To scan ingredients, please enable the Camera permission in Settings. You can continue without it.")
            .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Continue without it") { _, _ -> finishOnboarding() }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        // Do not finish here; let the user return and scan later.
    }
}
