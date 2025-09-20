package com.example.testdesign

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE = "extra_recipe"
        private const val FAVORITES_PREF = "recipe_favorites"

        fun newIntent(context: Context, recipe: Recipe): Intent {
            return Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra(EXTRA_RECIPE, Gson().toJson(recipe))
            }
        }
    }

    private lateinit var recipe: Recipe

    // header + meta
    private lateinit var btnBack: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var ivRecipeImage: ImageView
    private lateinit var tvRecipeTitle: TextView
    private lateinit var tvRecipeSubtitle: TextView
    private lateinit var tvCookingTime: TextView
    private lateinit var tvServings: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRegionDetail: TextView

    // lists
    private lateinit var rvIngredients: RecyclerView
    private lateinit var rvInstructions: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_recipe_detail)

        recipe = Gson().fromJson(intent.getStringExtra(EXTRA_RECIPE), Recipe::class.java)

        initViews()
        bindRecipe()
        setupLists()
        setupClicks()
        loadFavoriteState()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnFavorite = findViewById(R.id.btnFavorite)
        ivRecipeImage = findViewById(R.id.ivRecipeImage)
        tvRecipeTitle = findViewById(R.id.tvRecipeTitle)
        tvRecipeSubtitle = findViewById(R.id.tvRecipeSubtitle)
        tvCookingTime = findViewById(R.id.tvCookingTime)
        tvServings = findViewById(R.id.tvServings)
        tvDifficulty = findViewById(R.id.tvDifficulty)
        tvDescription = findViewById(R.id.tvDescription)
        tvRegionDetail = findViewById(R.id.tvRegionDetail)
        rvIngredients = findViewById(R.id.rvIngredients)
        rvInstructions = findViewById(R.id.rvInstructions)
    }

    private fun bindRecipe() {
        val resId = recipe.imageUrl?.let {
            resources.getIdentifier(it, "drawable", packageName)
        } ?: 0
        if (resId != 0) ivRecipeImage.setImageResource(resId)
        else ivRecipeImage.setImageResource(R.drawable.placeholder_recipe)

        tvRecipeTitle.text = recipe.title
        tvRecipeSubtitle.text = recipe.subtitle
        tvCookingTime.text = recipe.cookingTime
        tvServings.text = recipe.servings
        tvDescription.text = recipe.description

        val bg = when (recipe.difficulty.lowercase()) {
            "easy" -> R.drawable.bg_difficulty_easy
            "medium" -> R.drawable.bg_difficulty_medium
            "hard" -> R.drawable.bg_difficulty_hard
            else -> R.drawable.bg_difficulty_easy
        }
        tvDifficulty.setBackgroundResource(bg)
        tvDifficulty.text = recipe.difficulty

        if (!recipe.region.isNullOrBlank()) {
            tvRegionDetail.text = recipe.region
            tvRegionDetail.visibility = View.VISIBLE
        } else {
            tvRegionDetail.visibility = View.GONE
        }
    }

    private fun setupLists() {
        // INGREDIENTS — use the adapter with the click callback for core items
        rvIngredients.layoutManager = LinearLayoutManager(this)
        rvIngredients.adapter = IngredientsAdapter(recipe.ingredients) { coreName ->
            // opens the Ingredient Detail screen
            startActivity(IngredientDetailActivity.newIntent(this, coreName))
        }

        // INSTRUCTIONS
        rvInstructions.layoutManager = LinearLayoutManager(this)
        rvInstructions.adapter = InstructionsAdapter(recipe.instructions)
    }

    private fun setupClicks() {
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnFavorite.setOnClickListener { toggleFavorite() }
    }

    private fun loadFavoriteState() {
        val sp = getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        recipe.isFavorite = sp.getBoolean(recipe.id.toString(), false)
        updateFavoriteIcon()
    }

    private fun toggleFavorite() {
        recipe.isFavorite = !recipe.isFavorite
        val sp = getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        with(sp.edit()) {
            if (recipe.isFavorite) putBoolean(recipe.id.toString(), true)
            else remove(recipe.id.toString())
            apply()
        }
        updateFavoriteIcon()
    }

    private fun updateFavoriteIcon() {
        btnFavorite.setImageResource(
            if (recipe.isFavorite) R.drawable.ic_favorite_recipes
            else R.drawable.ic_favorite_border
        )
    }
}
