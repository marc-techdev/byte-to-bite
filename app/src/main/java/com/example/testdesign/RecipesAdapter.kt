package com.example.testdesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class RecipesAdapter(
    private var recipes: MutableList<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipesAdapter.RecipeViewHolder>() {

    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size

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
                // sane fallback
                tv.setBackgroundResource(R.drawable.bg_difficulty_easy)
                tv.text = difficulty
            }
        }
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_recipe)
        private val imageView: ImageView = itemView.findViewById(R.id.iv_recipe_image)
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_recipe_title)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.tv_recipe_subtitle)
        private val cookingTimeTextView: TextView = itemView.findViewById(R.id.tv_cooking_time)
        private val servingsTextView: TextView = itemView.findViewById(R.id.tv_servings)
        private val difficultyTextView: TextView = itemView.findViewById(R.id.tv_difficulty)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.iv_favorite)
        // NEW: region chip in list card
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

            // NEW: region chip (show/hide)
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
            }

            favoriteImageView.setOnClickListener {
                recipe.isFavorite = !recipe.isFavorite
                onFavoriteClick(recipe)
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }
}
