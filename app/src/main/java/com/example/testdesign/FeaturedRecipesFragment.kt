package com.example.testdesign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class FeaturedRecipesFragment : Fragment() {

    private val gson = Gson()
    private val FAVORITES_PREF = "recipe_favorites"

    // Card 1
    private var card1: CardView? = null
    private var iv1: ImageView? = null
    private var title1: TextView? = null
    private var subtitle1: TextView? = null
    private var time1: TextView? = null
    private var servings1: TextView? = null
    private var diff1: TextView? = null
    private var fav1: ImageView? = null

    // Card 2
    private var card2: CardView? = null
    private var iv2: ImageView? = null
    private var title2: TextView? = null
    private var subtitle2: TextView? = null
    private var time2: TextView? = null
    private var servings2: TextView? = null
    private var diff2: TextView? = null
    private var fav2: ImageView? = null

    // Card 3
    private var card3: CardView? = null
    private var iv3: ImageView? = null
    private var title3: TextView? = null
    private var subtitle3: TextView? = null
    private var time3: TextView? = null
    private var servings3: TextView? = null
    private var diff3: TextView? = null
    private var fav3: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_featured_recipes_container, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        // “Want more recipes?” row -> Recipes tab
        view.findViewById<View>(R.id.layout_want_more_recipes)?.setOnClickListener {
            (activity as? MainActivity)?.openRecipesTab()
        }

        val all = loadRecipesFromAssets()
        val featured = all.shuffled().take(3)   // random 3 every time Home opens

        if (featured.isNotEmpty()) bindCard(1, featured[0])
        if (featured.size > 1) bindCard(2, featured[1])
        if (featured.size > 2) bindCard(3, featured[2])
    }

    private fun bindViews(v: View) {
        // card 1 refs
        card1 = v.findViewById(R.id.card_featured_recipe_1)
        iv1 = v.findViewById(R.id.iv_featured_recipe_1)
        title1 = v.findViewById(R.id.tv_featured_recipe_title_1)
        subtitle1 = v.findViewById(R.id.tv_featured_recipe_subtitle_1)
        time1 = v.findViewById(R.id.tv_featured_cooking_time_1)
        servings1 = v.findViewById(R.id.tv_featured_servings_1)
        diff1 = v.findViewById(R.id.tv_featured_difficulty_1)
        fav1 = v.findViewById(R.id.iv_featured_favorite_1)

        // card 2 refs
        card2 = v.findViewById(R.id.card_featured_recipe_2)
        iv2 = v.findViewById(R.id.iv_featured_recipe_2)
        title2 = v.findViewById(R.id.tv_featured_recipe_title_2)
        subtitle2 = v.findViewById(R.id.tv_featured_recipe_subtitle_2)
        time2 = v.findViewById(R.id.tv_featured_cooking_time_2)
        servings2 = v.findViewById(R.id.tv_featured_servings_2)
        diff2 = v.findViewById(R.id.tv_featured_difficulty_2)
        fav2 = v.findViewById(R.id.iv_featured_favorite_2)

        // card 3 refs
        card3 = v.findViewById(R.id.card_featured_recipe_3)
        iv3 = v.findViewById(R.id.iv_featured_recipe_3)
        title3 = v.findViewById(R.id.tv_featured_recipe_title_3)
        subtitle3 = v.findViewById(R.id.tv_featured_recipe_subtitle_3)
        time3 = v.findViewById(R.id.tv_featured_cooking_time_3)
        servings3 = v.findViewById(R.id.tv_featured_servings_3)
        diff3 = v.findViewById(R.id.tv_featured_difficulty_3)
        fav3 = v.findViewById(R.id.iv_featured_favorite_3)
    }

    private fun loadRecipesFromAssets(): List<Recipe> {
        return try {
            val json = requireContext().assets.open("recipes.json")
                .use { input -> BufferedReader(input.reader()).use { it.readText() } }
            val type = object : TypeToken<List<Recipe>>() {}.type
            val list: List<Recipe> = gson.fromJson(json, type)

            // apply saved favorite state
            val sp = requireContext().getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
            list.onEach { it.isFavorite = sp.getBoolean(it.id.toString(), false) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun bindCard(position: Int, recipe: Recipe) {
        // pick the correct views set
        val card: CardView?
        val image: ImageView?
        val title: TextView?
        val subtitle: TextView?
        val time: TextView?
        val servings: TextView?
        val diff: TextView?
        val fav: ImageView?

        when (position) {
            1 -> {
                card = card1; image = iv1; title = title1; subtitle = subtitle1
                time = time1; servings = servings1; diff = diff1; fav = fav1
            }
            2 -> {
                card = card2; image = iv2; title = title2; subtitle = subtitle2
                time = time2; servings = servings2; diff = diff2; fav = fav2
            }
            else -> {
                card = card3; image = iv3; title = title3; subtitle = subtitle3
                time = time3; servings = servings3; diff = diff3; fav = fav3
            }
        }

        // text/meta
        title?.text = recipe.title
        subtitle?.text = recipe.subtitle
        time?.text = recipe.cookingTime
        servings?.text = recipe.servings
        diff?.text = recipe.difficulty

        // image
        val resId = recipe.imageUrl?.let {
            resources.getIdentifier(it, "drawable", requireContext().packageName)
        } ?: 0
        if (resId != 0) image?.setImageResource(resId)
        else image?.setImageResource(R.drawable.placeholder_recipe)

        // open detail
        card?.setOnClickListener {
            startActivity(RecipeDetailActivity.newIntent(requireContext(), recipe))
        }

        // favorite toggle
        updateFavIcon(fav, recipe.isFavorite)
        fav?.setOnClickListener {
            recipe.isFavorite = !recipe.isFavorite
            saveFavoriteState(recipe.id, recipe.isFavorite)
            updateFavIcon(fav, recipe.isFavorite)
        }
    }

    private fun updateFavIcon(iv: ImageView?, isFav: Boolean) {
        // Prefer featured icons if you have them; fall back to general ones
        val filled = resources.getIdentifier(
            "ic_favorite_featured", "drawable", requireContext().packageName
        ).takeIf { it != 0 } ?: R.drawable.ic_favorite_recipes

        val border = resources.getIdentifier(
            "ic_favorite_border_featured", "drawable", requireContext().packageName
        ).takeIf { it != 0 } ?: R.drawable.ic_favorite_border_recipes

        iv?.setImageResource(if (isFav) filled else border)
    }

    private fun saveFavoriteState(id: Int, isFav: Boolean) {
        val sp = requireContext().getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        with(sp.edit()) {
            if (isFav) putBoolean(id.toString(), true) else remove(id.toString())
            apply()
        }
    }
}
