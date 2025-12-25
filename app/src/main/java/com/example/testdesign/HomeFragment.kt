package com.example.testdesign

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.testdesign.tutorial.ScanHelpSeenLocal
import com.example.testdesign.tutorial.ScanTutorialBottomSheet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.text.Normalizer
import kotlin.math.abs
import kotlin.random.Random

class HomeFragment : Fragment() {

    // HERO carousel
    private lateinit var heroPager: ViewPager2
    private var heroAdapter: HeroCarouselAdapter? = null
    private val heroHandler = Handler(Looper.getMainLooper())
    private val autoSlideInterval = 4800L
    private val autoSlide = object : Runnable {
        override fun run() {
            if (::heroPager.isInitialized && (heroAdapter?.itemCount ?: 0) > 1) {
                heroPager.setCurrentItem(heroPager.currentItem + 1, true)
                heroHandler.postDelayed(this, autoSlideInterval)
            }
        }
    }

    // Nav
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
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // HERO
        heroPager = view.findViewById(R.id.vp_hero)
        setupHeroPagerTransform()

        // Nav
        scanCard = view.findViewById(R.id.layout_scan_vegetables)
        seeAll = view.findViewById(R.id.tv_see_all)

        // Matching UI
        tvMatchingHeader = view.findViewById(R.id.tv_matching_recipes)
        rvMatches = view.findViewById(R.id.rv_matching_recipes)
        emptyLayout = view.findViewById(R.id.ll_matching_empty)

        rvMatches.layoutManager = LinearLayoutManager(requireContext())
        matchesAdapter = HomeMatchesAdapter(mutableListOf()) { recipe ->
            startActivity(RecipeDetailActivity.newIntent(requireContext(), recipe))
        }
        rvMatches.adapter = matchesAdapter

        // Featured fragment
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_featured_recipes_container, FeaturedRecipesFragment())
                .commit()
        }

        // Clicks
        scanCard.setOnClickListener { openScanTab() }
        seeAll.setOnClickListener { openRecipesTab() }

        // Load data + populate
        loadRecipesFromAssets()
        setupHeroCarousel()
        populateMatchingSection()
    }

    // -------- HERO helpers --------

    private fun setupHeroPagerTransform() {
        heroPager.setPageTransformer { page, position ->
            page.translationX = -page.width * position
            page.alpha = 1f - kotlin.math.min(1f, abs(position))
        }
        heroPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { restartAutoSlide() }
        })
    }

    @SuppressLint("DiscouragedApi")
    private fun buildHeroImages(): List<Int> {
        val ids = mutableListOf<Int>()
        for (r in allRecipes.shuffled(Random(System.currentTimeMillis()))) {
            val name = r.imageUrl ?: continue
            val id = resources.getIdentifier(name, "drawable", requireContext().packageName)
            if (id != 0 && !ids.contains(id)) ids += id
            if (ids.size >= 8) break
        }
        if (ids.isEmpty()) ids += android.R.drawable.ic_menu_gallery
        return ids
    }

    private fun setupHeroCarousel() {
        val images = buildHeroImages()
        if (images.isEmpty()) return
        heroAdapter = HeroCarouselAdapter(images)
        heroPager.adapter = heroAdapter

        val start = if (images.size > 1) {
            val mid = Int.MAX_VALUE / 2
            mid - (mid % images.size)
        } else 0
        heroPager.setCurrentItem(start, false)
    }

    private fun restartAutoSlide() {
        heroHandler.removeCallbacks(autoSlide)
        heroHandler.postDelayed(autoSlide, autoSlideInterval)
    }

    override fun onResume() {
        super.onResume()
        restartAutoSlide()
        populateMatchingSection()
        view?.postDelayed({ maybeShowScanTutorial() }, 650)
    }

    override fun onPause() {
        heroHandler.removeCallbacks(autoSlide)
        super.onPause()
    }

    // -------- Navigation --------
    private fun openScanTab() {
        (activity as? MainActivity)?.findViewById<View>(R.id.tab_scan)?.performClick()
    }

    private fun openRecipesTab() {
        (activity as? MainActivity)?.findViewById<View>(R.id.tab_recipes)?.performClick()
    }

    // -------- Data --------
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

    // -------- Matching section --------
    @SuppressLint("SetTextI18n")
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
                if (hit > 0) HomeMatchesAdapter.MatchedRecipe(recipe, hit, totalScanned) else null
            }
                .sortedWith(
                    compareByDescending<HomeMatchesAdapter.MatchedRecipe> { it.matchCount }
                        .thenBy { it.recipe.title }
                )
                .take(3)
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
            Gson().fromJson<List<SavedIngredient>>(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccent.trim()
    }

    // -------- Tutorial helper --------

    private fun maybeShowScanTutorial() {
        val act = requireActivity()
        val shouldShow = !ScanHelpSeenLocal.isSeen(act)
        val noDialogShowing = parentFragmentManager.findFragmentByTag("scan_tutorial") == null

        if (shouldShow && noDialogShowing) {
            ScanTutorialBottomSheet.showRaw(
                host = act,
                videoResId = R.raw.scan_tutorial,
                subtitleResId = 0,
                startMuted = false   // 🔊 sound ON
            )
        }
    }


}
