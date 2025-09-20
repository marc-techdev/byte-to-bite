package com.example.testdesign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.text.Normalizer
import kotlin.random.Random

class HomeFragment : Fragment() {

    // Views
    private lateinit var heroImage: ImageView
    private lateinit var scanCard: View
    private lateinit var seeAll: TextView

    // Matching section
    private lateinit var tvMatchingHeader: TextView
    private lateinit var rvMatches: RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var matchesAdapter: HomeMatchesAdapter

    // Data
    private val gson = Gson()
    private var allRecipes: List<Recipe> = emptyList()

    // SharedPref keys used by ScanFragment
    private val PREF_NAME = "scan_ingredients_prefs"
    private val KEY_INGREDIENTS = "scanned_ingredients"

    data class SavedIngredient(
        val name: String,
        val confidence: Float,
        val timestamp: Long
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- hero
        heroImage = view.findViewById(R.id.iv_cuisine_background)

        // --- nav bits
        scanCard = view.findViewById(R.id.layout_scan_vegetables)
        seeAll = view.findViewById(R.id.tv_see_all)

        // --- matching UI
        tvMatchingHeader = view.findViewById(R.id.tv_matching_recipes)
        rvMatches = view.findViewById(R.id.rv_matching_recipes)
        emptyLayout = view.findViewById(R.id.ll_matching_empty)

        rvMatches.layoutManager = LinearLayoutManager(requireContext())
        matchesAdapter = HomeMatchesAdapter(mutableListOf()) { recipe ->
            startActivity(RecipeDetailActivity.newIntent(requireContext(), recipe))
        }
        rvMatches.adapter = matchesAdapter

        // Featured fragment (keep your previous behavior)
        if (savedInstanceState == null) {
            val featured = FeaturedRecipesFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_featured_recipes_container, featured)
                .commit()
        }

        // Clicks
        scanCard.setOnClickListener { openScanTab() }
        seeAll.setOnClickListener { openRecipesTab() }

        // Load data + populate
        loadRecipesFromAssets()
        setRandomHeroImage()
        populateMatchingSection()
    }

    private fun openScanTab() {
        // Ask MainActivity to switch to Scan tab
        (activity as? MainActivity)?.let { main ->
            // Simulate clicking the bottom nav's Scan tab
            main.findViewById<View>(R.id.tab_scan)?.performClick()
        }
    }

    private fun openRecipesTab() {
        (activity as? MainActivity)?.let { main ->
            main.findViewById<View>(R.id.tab_recipes)?.performClick()
        }
    }

    private fun loadRecipesFromAssets() {
        try {
            val inputStream = requireContext().assets.open("recipes.json")
            val reader = BufferedReader(inputStream.reader())
            val jsonString = reader.use { it.readText() }
            val type = object : TypeToken<List<Recipe>>() {}.type
            allRecipes = gson.fromJson(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            allRecipes = emptyList()
        }
    }

    private fun setRandomHeroImage() {
        if (allRecipes.isEmpty()) return
        val pick = allRecipes[Random.nextInt(allRecipes.size)]
        pick.imageUrl?.let { name ->
            val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
            if (resId != 0) heroImage.setImageResource(resId)
        }
    }

    private fun populateMatchingSection() {
        val scanned = readSavedScannedIngredients()
        val scannedNames = scanned.map { normalize(it.name) }.filter { it.isNotBlank() }
        val totalScanned = scannedNames.size

        val matches = if (scannedNames.isEmpty() || allRecipes.isEmpty()) {
            emptyList<HomeMatchesAdapter.MatchedRecipe>()
        } else {
            allRecipes.mapNotNull { recipe ->
                val recipeText = (recipe.ingredients + listOf(recipe.title, recipe.subtitle))
                    .joinToString("\n") { normalize(it) }

                val hit = scannedNames.count { kw -> recipeText.contains(kw) }
                if (hit > 0) {
                    HomeMatchesAdapter.MatchedRecipe(recipe, hit, totalScanned)
                } else null
            }
                .sortedWith(
                    compareByDescending<HomeMatchesAdapter.MatchedRecipe> { it.matchCount }
                        .thenBy { it.recipe.title }
                )
                .take(3) // show top 3
        }

        if (matches.isEmpty()) {
            rvMatches.visibility = View.GONE
            emptyLayout.visibility = View.VISIBLE
            tvMatchingHeader.text = "Matching Recipes"
        } else {
            matchesAdapter.submitList(matches)
            rvMatches.visibility = View.VISIBLE
            emptyLayout.visibility = View.GONE
            tvMatchingHeader.text = "Matching Recipes (${matches.size})"
        }
    }

    private fun readSavedScannedIngredients(): List<SavedIngredient> {
        val sp = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_INGREDIENTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedIngredient>>() {}.type
            gson.fromJson<List<SavedIngredient>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccent.trim()
    }
}
