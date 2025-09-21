package com.example.testdesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

/**
 * Sectioned adapter that shows alphabet headers (A, B, C, ...) before recipe groups.
 * Works for both RecipesFragment and FavoritesFragment.
 */
class RecipesAdapter(
    recipes: MutableList<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // --- Sectioned rows (Header or Recipe row) ---
    private sealed class Row {
        data class Header(val letter: String) : Row()
        data class RecipeRow(val recipe: Recipe) : Row()
    }

    private val rows: MutableList<Row> = mutableListOf()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_RECIPE = 1
    }

    init {
        updateRecipes(recipes) // build initial sectioned rows
    }

    /** Public API used by both RecipesFragment and FavoritesFragment */
    fun updateRecipes(newRecipes: List<Recipe>) {
        rows.clear()

        // Sort alphabetically by title for stable sectioning
        val sorted = newRecipes.sortedBy { it.title.trim().lowercase() }

        var lastLetter: String? = null
        for (r in sorted) {
            val letter = r.title.firstOrNull { it.isLetter() }
                ?.uppercaseChar()?.toString() ?: "#"
            if (letter != lastLetter) {
                rows.add(Row.Header(letter))
                lastLetter = letter
            }
            rows.add(Row.RecipeRow(r))
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> TYPE_HEADER
        is Row.RecipeRow -> TYPE_RECIPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val v = inflater.inflate(R.layout.item_section_header, parent, false)
            HeaderVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_recipe_card, parent, false)
            RecipeVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderVH).bind(row.letter)
            is Row.RecipeRow -> (holder as RecipeVH).bind(row.recipe)
        }
    }

    override fun getItemCount(): Int = rows.size

    // ================== ViewHolders ==================

    private class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSection: TextView = itemView.findViewById(R.id.tvSection)
        fun bind(letter: String) {
            tvSection.text = letter
        }
    }

    private inner class RecipeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_recipe)
        private val imageView: ImageView = itemView.findViewById(R.id.iv_recipe_image)
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_recipe_title)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.tv_recipe_subtitle)
        private val cookingTimeTextView: TextView = itemView.findViewById(R.id.tv_cooking_time)
        private val servingsTextView: TextView = itemView.findViewById(R.id.tv_servings)
        private val difficultyTextView: TextView = itemView.findViewById(R.id.tv_difficulty)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.iv_favorite)
        // Optional region chip in list card
        private val regionTextView: TextView? = itemView.findViewById(R.id.tv_region)

        fun bind(recipe: Recipe) {
            titleTextView.text = recipe.title
            subtitleTextView.text = recipe.subtitle
            cookingTimeTextView.text = recipe.cookingTime
            servingsTextView.text = recipe.servings

            setDifficultyBackground(difficultyTextView, recipe.difficulty)

            // image (with placeholder fallback)
            val resId = recipe.imageUrl?.let {
                itemView.context.resources.getIdentifier(it, "drawable", itemView.context.packageName)
            } ?: 0
            if (resId != 0) imageView.setImageResource(resId)
            else imageView.setImageResource(R.drawable.placeholder_recipe)

            // favorite icon
            favoriteImageView.setImageResource(
                if (recipe.isFavorite) R.drawable.ic_favorite_recipes
                else R.drawable.ic_favorite_border_recipes
            )

            // region chip
            if (!recipe.region.isNullOrBlank()) {
                regionTextView?.text = recipe.region
                regionTextView?.visibility = View.VISIBLE
            } else {
                regionTextView?.visibility = View.GONE
            }

            // clicks
            cardView.setOnClickListener {
                val intent = RecipeDetailActivity.newIntent(itemView.context, recipe)
                itemView.context.startActivity(intent)
                onRecipeClick(recipe)
            }

            favoriteImageView.setOnClickListener {
                recipe.isFavorite = !recipe.isFavorite
                onFavoriteClick(recipe)
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    // ================== helpers ==================

    private fun setDifficultyBackground(tv: TextView, difficulty: String) {
        when (difficulty.lowercase()) {
            "easy" -> {
                tv.setBackgroundResource(R.drawable.bg_difficulty_easy)
                tv.text = "Easy"
            }
            "medium" -> {
                tv.setBackgroundResource(R.drawable.bg_difficulty_medium)
                tv.text = "Medium"
            }
            "hard" -> {
                tv.setBackgroundResource(R.drawable.bg_difficulty_hard)
                tv.text = "Hard"
            }
            else -> {
                tv.setBackgroundResource(R.drawable.bg_difficulty_easy)
                tv.text = difficulty
            }
        }
    }
}
