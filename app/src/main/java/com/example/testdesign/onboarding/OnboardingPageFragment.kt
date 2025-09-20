package com.example.testdesign.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.testdesign.R
import com.example.testdesign.databinding.FragmentOnboardingPageBinding
import java.io.Serializable

import androidx.core.text.HtmlCompat
import com.google.android.flexbox.FlexboxLayout

class OnboardingPageFragment : Fragment() {

    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var page: OnboardingPage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        page = requireArguments().getSerializable(ARG_PAGE) as OnboardingPage
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Header
        binding.tvTitle.text = page.title
        binding.tvBody.text  = page.body
        binding.tvBody.isVisible = page.body.isNotEmpty()

        // Hero
        binding.ivHero.isVisible = page.imageRes != 0
        if (page.imageRes != 0) binding.ivHero.setImageResource(page.imageRes)

        hideAllDynamicSections()

        when (page.type) {
            OnbType.WELCOME -> {
                binding.tvBody.isVisible = page.body.isNotEmpty()
                binding.llBullets.isVisible = true
                binding.llBullets.removeAllViews()
                page.bullets.forEach { b ->
                    val row = layoutInflater.inflate(
                        R.layout.item_onb_bullet_row, binding.llBullets, false
                    )
                    row.findViewById<ImageView>(R.id.ivIcon).setImageResource(b.iconRes)
                    row.findViewById<TextView>(R.id.tvPrimary).text = b.text
                    binding.llBullets.addView(row)
                }
            }

            OnbType.HOW -> {
                // Remove the big page title and gray subtitle (the first “How it works”)
                binding.tvTitle.isVisible = false
                binding.tvBody.isVisible  = false

                // Keep the small section header above the cards
                binding.tvStepsCaption.isVisible = true
                binding.llSteps.isVisible = true
                binding.tvHowFoot.isVisible = true

                binding.llSteps.removeAllViews()
                binding.llSteps.addView(makeStepCard(
                    1, getString(R.string.onb_step1_title), getString(R.string.onb_step1_sub)))
                binding.llSteps.addView(makeStepCard(
                    2, getString(R.string.onb_step2_title), getString(R.string.onb_step2_sub)))
                binding.llSteps.addView(makeStepCard(
                    3, getString(R.string.onb_step3_title), getString(R.string.onb_step3_sub)))
            }


            OnbType.TIPS -> {
                binding.tvBody.isVisible = page.body.isNotEmpty()
                binding.llChecks.isVisible = true
                binding.btnSeeTips.isVisible = page.showTipsCta

                binding.llChecks.removeAllViews()
                listOf(
                    Triple(R.drawable.ic_eye_24, getString(R.string.onb_check1_title), ""),
                    Triple(R.drawable.ic_sun_24, getString(R.string.onb_check2_title), ""),
                    Triple(R.drawable.ic_square_24, getString(R.string.onb_check3_title), "")
                ).forEach { (icon, title, sub) ->
                    val row = layoutInflater.inflate(R.layout.item_onb_check_row, binding.llChecks, false)
                    row.findViewById<ImageView>(R.id.ivLead).setImageResource(icon)
                    row.findViewById<TextView>(R.id.tvPrimary).text = title
                    val subTv = row.findViewById<TextView>(R.id.tvSecondary)
                    if (sub.isBlank()) subTv.visibility = View.GONE else subTv.text = sub
                    binding.llChecks.addView(row)
                }

                binding.btnSeeTips.setOnClickListener {
                    try {
                        val clazz = Class.forName("com.example.testdesign.SnapTipsBottomSheet")
                        val sheet = clazz.getDeclaredConstructor().newInstance() as DialogFragment
                        sheet.show(parentFragmentManager, "snapTips")
                    } catch (_: Throwable) { }
                }
            }

            // COMPACT INGREDIENTS PAGE (uses sectionIngredientsCompact)
            OnbType.INGREDIENTS -> {
                // Hide the big header & hero for the compact design
                binding.ivHero.isVisible = false
                binding.tvTitle.isVisible = false
                binding.tvBody.isVisible  = false

                // Show the compact section
                binding.sectionIngredientsCompact.isVisible = true

                val items = listOf(
                    "Calamansi", "Carrot", "Chayote", "Egg",
                    "Eggplant", "Garlic", "Ginger", "Potato",
                    "Radish", "Red Onion", "Tomato", "White Onion"
                )
                // Subtitle with bold count
                binding.ingSubtitle.text = HtmlCompat.fromHtml(
                    getString(R.string.onb_coverage_compact, items.size),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                // Build chips
                val flex = binding.flexChips
                flex.removeAllViews()
                items.forEach { label ->
                    val chip = layoutInflater.inflate(
                        R.layout.item_chip_simple, flex, false
                    ) as TextView
                    chip.text = label
                    flex.addView(chip)
                }
            }

            OnbType.PRIVACY -> {
                binding.ivHero.isVisible = page.imageRes != 0
                binding.tvTitle.isVisible = true
                binding.tvBody.isVisible  = page.body.isNotEmpty()

                binding.llChecks.isVisible = true
                binding.llChecks.removeAllViews()

                val points = listOf(
                    getString(R.string.onb_privacy_point1),
                    getString(R.string.onb_privacy_point2)
                )

                points.forEach { pointText ->
                    val row = layoutInflater.inflate(
                        R.layout.item_onb_check_row,
                        binding.llChecks,
                        false
                    )

                    row.findViewById<ImageView>(R.id.ivLead).setImageResource(
                        R.drawable.ic_check_filled_24
                    )

                    row.findViewById<TextView>(R.id.tvPrimary).apply {
                        this.text = pointText          // <-- key change
                        textSize = 18f
                    }

                    row.findViewById<TextView>(R.id.tvSecondary).visibility = View.GONE

                    binding.llChecks.addView(row)
                }
            }

        }
    }

    private fun hideAllDynamicSections() {
        binding.llBullets.isVisible       = false
        binding.tvStepsCaption.isVisible  = false
        binding.llSteps.isVisible         = false
        binding.tvHowFoot.isVisible       = false
        binding.tvChecksCaption.isVisible = false
        binding.llChecks.isVisible        = false
        binding.btnSeeTips.isVisible      = false
        binding.tvCoverageTitle.isVisible = false
        binding.tvCoverageSub.isVisible   = false
        binding.gridChips.isVisible       = false
        binding.tvMoreSoon.isVisible      = false
        binding.sectionIngredientsCompact.isVisible = false
    }

    private fun makeStepCard(num: Int, title: String, subtitle: String): View =
        layoutInflater.inflate(R.layout.item_onb_step_card, binding.llSteps, false).apply {
            findViewById<TextView>(R.id.tvStepNum).text = num.toString()
            findViewById<TextView>(R.id.tvPrimary).text = title
            findViewById<TextView>(R.id.tvSecondary).text = subtitle
        }

    private fun makeTwoLineRow(title: String, subtitle: String): View =
        layoutInflater.inflate(R.layout.item_onb_check_row, binding.llChecks, false).apply {
            findViewById<TextView>(R.id.tvPrimary).text = title
            findViewById<TextView>(R.id.tvSecondary).text = subtitle
        }

    private fun populateChips(@Suppress("UNUSED_PARAMETER") grid: GridLayout) { /* kept for legacy path */ }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PAGE = "arg_onb_page"
        fun newInstance(page: OnboardingPage) = OnboardingPageFragment().apply {
            arguments = bundleOf(ARG_PAGE to page as Serializable)
        }
    }
}
