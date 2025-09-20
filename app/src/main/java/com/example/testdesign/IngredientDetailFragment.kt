package com.example.testdesign

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class IngredientDetailFragment : Fragment() {

    companion object {
        private const val ARG_CORE = "arg_core" // canonical name, e.g., "red onion"

        fun newInstance(coreName: String) = IngredientDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_CORE, coreName) }
        }
    }

    private lateinit var ivHero: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvUses: TextView
    private lateinit var tvSubs: TextView
    private lateinit var tvStorage: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_ingredient_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header back arrow
        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Bind views
        ivHero = view.findViewById(R.id.ivHero)
        tvName = view.findViewById(R.id.tvName)
        tvAlt = view.findViewById(R.id.tvAlt)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvUses = view.findViewById(R.id.tvUses)
        tvSubs = view.findViewById(R.id.tvSubs)
        tvStorage = view.findViewById(R.id.tvStorage)

        // Load content
        val core = arguments?.getString(ARG_CORE)
        if (core.isNullOrBlank()) {
            // No argument passed; just go back gracefully
            requireActivity().onBackPressedDispatcher.onBackPressed()
        } else {
            val info = IngredientInfoRepository.getInfo(requireContext(), core)

            tvName.text = info.displayName
            tvAlt.text = info.altNames
            tvDescription.text = info.description
            tvSubs.text = info.substitutes.joinToString(", ")
            tvStorage.text = info.storageTip
            setOrangeBullets(tvUses, info.uses)

            // Resolve ig_<canonical> drawable (e.g., ig_red_onion), else placeholder
            val resName = "ig_${info.canonical.replace(" ", "_")}"
            val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
            ivHero.setImageResource(if (resId != 0) resId else R.drawable.placeholder_ingredient)
        }
    }

    /** Renders a list with orange bullets to match the card accent. */
    private fun setOrangeBullets(tv: TextView, items: List<String>) {
        val orange = Color.parseColor("#FF6B35")
        val sb = SpannableStringBuilder()
        items.forEachIndexed { i, s ->
            val start = sb.length
            sb.append("• ")
            sb.setSpan(
                ForegroundColorSpan(orange),
                start,
                start + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sb.append(s)
            if (i != items.lastIndex) sb.append("\n")
        }
        tv.text = sb
    }
}
