package com.example.testdesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.util.Locale
import java.util.regex.Pattern

class IngredientsAdapter(
    private val lines: List<String>,
    private val onCoreIngredientClick: (String /*canonical core name*/) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.VH>() {

    // Your 12 canonical cores
    private val cores = listOf(
        "calamansi","carrot","chayote","egg","eggplant","garlic",
        "ginger","potato","radish","red onion","tomato","white onion"
    )

    // permissive regex per core (plural, spacing, hyphens)
    private val patterns: Map<String, Pattern> = mapOf(
        "calamansi"   to "\\bcalamans?i\\b",
        "carrot"      to "\\bcarrots?\\b",
        "chayote"     to "\\bchayotes?\\b|\\bsayote\\b",
        "egg"         to "\\beggs?\\b",
        "eggplant"    to "\\begg\\s*plants?\\b|\\btalong\\b",
        "garlic"      to "\\bgarlic\\b|\\bcloves?\\s+garlic\\b",
        "ginger"      to "\\bginger\\b|\\bluya\\b",
        "potato"      to "\\bpotatoes?\\b",
        "radish"      to "\\bradishes?\\b|\\blabanos\\b",
        "red onion"   to "\\bred\\s+onions?\\b",
        "tomato"      to "\\btomatoes?\\b|\\bkamatis\\b",
        "white onion" to "\\bwhite\\s+onions?\\b"
    ).mapValues { Pattern.compile(it.value, Pattern.CASE_INSENSITIVE) }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.cardRoot)
        val title: TextView        = v.findViewById(R.id.tvIngredient)
        val chevron: ImageView     = v.findViewById(R.id.ivChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ingredient, parent, false)
        return VH(v)
    }

    override fun getItemCount() = lines.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val line = lines[pos]
        h.title.text = line

        // Try to extract which core (if any) appears in this line.
        val matchedCore = extractCore(line)

        // Arrow + click only when we matched one of the 12
        val isCore = matchedCore != null
        h.chevron.visibility = if (isCore) View.VISIBLE else View.GONE

        if (isCore) {
            val coreName = matchedCore!!
            h.card.isClickable = true
            h.card.setOnClickListener { onCoreIngredientClick(coreName) }
            h.chevron.setOnClickListener { onCoreIngredientClick(coreName) }
        } else {
            h.card.setOnClickListener(null)
            h.chevron.setOnClickListener(null)
        }
    }

    private fun extractCore(text: String): String? {
        val t = text.lowercase(Locale.getDefault())
        patterns.forEach { (core, pat) ->
            if (pat.matcher(t).find()) return core
        }
        return null
    }
}
