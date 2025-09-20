package com.example.testdesign

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScannedIngredientsAdapter(
    private val ingredients: MutableList<ScannedIngredient>,
    private val onRemoveClick: (ScannedIngredient, Int) -> Unit
) : RecyclerView.Adapter<ScannedIngredientsAdapter.ScannedIngredientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedIngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scanned_ingredient, parent, false)
        return ScannedIngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScannedIngredientViewHolder, position: Int) {
        holder.bind(ingredients[position], position)


    }

    override fun getItemCount(): Int = ingredients.size

    inner class ScannedIngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIngredientImage: ImageView = itemView.findViewById(R.id.iv_ingredient_image)
        private val tvIngredientIcon: TextView = itemView.findViewById(R.id.tv_ingredient_icon)
        private val tvIngredientName: TextView = itemView.findViewById(R.id.tv_ingredient_name)
        private val tvConfidenceLabel: TextView = itemView.findViewById(R.id.tv_confidence_label)
        private val ivRemoveIngredient: ImageView = itemView.findViewById(R.id.iv_remove_ingredient)

        fun bind(ingredient: ScannedIngredient, position: Int) {
            tvIngredientName.text = ingredient.name

            val confidencePercent = (ingredient.confidence * 100).toInt()
            tvConfidenceLabel.text = "Confidence: $confidencePercent%"

            if (isProtein(ingredient.name)) {
                // proteins -> show emoji TextView, hide ImageView
                ivIngredientImage.visibility = View.GONE
                tvIngredientIcon.visibility = View.VISIBLE
                tvIngredientIcon.text = proteinEmoji(ingredient.name)
            } else {
                // veggies -> try to show your drawable; fallback to emoji if not found
                val resId = getIngredientDrawableRes(itemView.context, ingredient.name)
                if (resId != 0) {
                    ivIngredientImage.visibility = View.VISIBLE
                    tvIngredientIcon.visibility = View.GONE
                    ivIngredientImage.setImageResource(resId)
                } else {
                    ivIngredientImage.visibility = View.GONE
                    tvIngredientIcon.visibility = View.VISIBLE
                    tvIngredientIcon.text = "🥬" // fallback, no crash
                }
            }

            ivRemoveIngredient.setOnClickListener {
                onRemoveClick(ingredient, position)
            }
        }

        private fun isProtein(name: String): Boolean {
            val t = name.trim().lowercase()
            return t in setOf("chicken", "pork", "beef", "fish", "shrimp")
                    || t in setOf("manok", "baboy", "baka", "isda", "hipon")
        }

        private fun proteinEmoji(name: String): String {
            return when (name.trim().lowercase()) {
                "chicken", "manok" -> "🐔"
                "pork", "baboy" -> "🐖"
                "beef", "baka" -> "🐄"
                "fish", "isda" -> "🐟"
                "shrimp", "hipon" -> "🍤"
                else -> "🍗"
            }
        }
    }

    // Maps a scanned veggie name (Fil/Eng/synonyms) to your drawable resource id (ig_*)
    private fun getIngredientDrawableRes(context: Context, rawName: String): Int {
        val key = canonicalVegKey(rawName) ?: return 0
        val drawableName = "ig_$key" // e.g., ig_red_onion
        return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    }

    // Return one of: calamansi, carrot, chayote, egg, eggplant, garlic, ginger,
    // potato, radish, red_onion, tomato, white_onion — or null if not recognized.
    private fun canonicalVegKey(raw: String): String? {
        var t = raw.trim().lowercase()

        // Quick contains checks for multi-word onions
        if (t.contains("red onion") || t.contains("sibuyas pula")) return "red_onion"
        if (t.contains("white onion") || t.contains("sibuyas puti")) return "white_onion"

        // Common Filipino/English synonyms
        val map = mapOf(
            "kalamansi" to "calamansi",
            "calamansi" to "calamansi",

            "carrot" to "carrot",
            "carrots" to "carrot",

            "sayote" to "chayote",
            "chayote" to "chayote",

            "egg" to "egg",

            "talong" to "eggplant",
            "eggplant" to "eggplant",

            "bawang" to "garlic",
            "garlic" to "garlic",

            "luya" to "ginger",
            "ginger" to "ginger",

            "patatas" to "potato",
            "potato" to "potato",
            "potatoes" to "potato",

            "labanos" to "radish",
            "radish" to "radish",

            "sibuyas pula" to "red_onion",

            "kamatis" to "tomato",
            "tomato" to "tomato",
            "tomatoes" to "tomato",

            "sibuyas puti" to "white_onion"
        )

        // Normalize plurals (basic)
        t = t.replace(Regex("\\b([a-z]+)s\\b"), "$1")

        // Exact or fallback contains-based checks
        map[t]?.let { return it }

        // Contains fallbacks (for phrases like "fresh ginger root", "diced red onion", etc.)
        return when {
            t.contains("calamansi") || t.contains("kalamansi") -> "calamansi"
            t.contains("carrot") -> "carrot"
            t.contains("sayote") || t.contains("chayote") -> "chayote"
            t.contains("eggplant") || t.contains("talong") -> "eggplant"
            t.contains("garlic") || t.contains("bawang") -> "garlic"
            t.contains("ginger") || t.contains("luya") -> "ginger"
            t.contains("potato") || t.contains("patatas") -> "potato"
            t.contains("radish") || t.contains("labanos") -> "radish"
            t.contains("tomato") || t.contains("kamatis") -> "tomato"
            else -> null
        }
    }

    data class ScannedIngredient(
        val name: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    )
}
