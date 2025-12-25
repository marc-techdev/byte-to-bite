package com.example.testdesign

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.util.Locale
import java.util.regex.Pattern

class RecipesFragment : Fragment() {

    private lateinit var recipesAdapter: RecipesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeCountTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var noResultsLayout: LinearLayout
    private lateinit var filterIcon: ImageView
    private lateinit var ddProtein: MaterialAutoCompleteTextView

    private var fullRecipeList: MutableList<Recipe> = mutableListOf()
    private var scannedIngredients: MutableList<String> = mutableListOf()
    private var isFilterActive: Boolean = false

    companion object {
        private const val FAVORITES_PREF = "recipe_favorites"
        private const val ARG_SCANNED_INGREDIENTS = "arg_scanned_ingredients"

        private const val SCAN_PREF = "scan_ingredients_prefs"
        private const val KEY_SAVED_ING = "scanned_ingredients"
        private const val KEY_SAVED_PROTEINS = "selected_proteins"

        fun newInstance(scanned: ArrayList<String>): RecipesFragment {
            return RecipesFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_SCANNED_INGREDIENTS, scanned)
                }
            }
        }
    }

    // ---------- protein logic & helpers (unchanged) ----------

    private val proteinKeys = listOf("chicken", "pork", "beef", "fish", "shrimp")
    private val proteinDisplayToKey = mapOf(
        "Any protein" to null,
        "Chicken" to "chicken",
        "Pork" to "pork",
        "Beef" to "beef",
        "Fish" to "fish",
        "Shrimp" to "shrimp"
    )
    private val proteinKeyToDisplay = mapOf(
        null to "Any protein",
        "chicken" to "Chicken",
        "pork" to "Pork",
        "beef" to "Beef",
        "fish" to "Fish",
        "shrimp" to "Shrimp"
    )
    private val proteinSynonyms: Map<String, List<String>> = mapOf(
        "chicken" to listOf("chicken", "manok"),
        "pork"    to listOf("pork", "baboy", "liempo"),
        "beef"    to listOf("beef", "baka", "bulalo", "beef shank"),
        "fish"    to listOf(
            "fish", "isda",
            "bangus", "tilapia", "galunggong", "gg", "tulingan",
            "tanigue", "tuna", "salmon", "mackerel", "sardines"
        ),
        "shrimp"  to listOf("shrimp", "hipon", "prawn", "prawns", "suahe")
    )
    private val disallowedContext = listOf(
        "sauce", "broth", "stock", "powder", "bouillon", "cube",
        "paste", "seasoning", "flavoring", "extract", "granules"
    )
    private fun makeProteinPatternFor(syn: String): Pattern {
        val escaped = Pattern.quote(syn)
        val ctx = disallowedContext.joinToString("|") { Pattern.quote(it) }
        val regex = "\\b$escaped\\b(?!\\s*(?:$ctx)\\b)"
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
    }
    private fun containsProtein(text: String, proteinKey: String): Boolean {
        val syns = proteinSynonyms[proteinKey] ?: return false
        val hay = text.lowercase(Locale.getDefault())
        return syns.any { syn -> makeProteinPatternFor(syn).matcher(hay).find() }
    }

    private val repl = listOf(
        "bok choy" to "pechay", "bokchoy" to "pechay",
        "talong" to "eggplant", "kamatis" to "tomato",
        "sibuyas pula" to "red onion", "sibuyas puti" to "white onion",
        "sibuyas" to "onion", "bawang" to "garlic", "luya" to "ginger",
        "patatas" to "potato", "labanos" to "radish", "repolyo" to "cabbage",
        "sitaw" to "string beans", "ampalaya" to "bitter gourd",
        "upo" to "bottle gourd", "malunggay" to "moringa",
        "kangkong" to "water spinach",
        "manok" to "chicken", "baboy" to "pork", "baka" to "beef",
        "isda" to "fish", "hipon" to "shrimp"
    )
    private fun canonicalize(text: String): String {
        var t = text.lowercase(Locale.getDefault())
        repl.sortedByDescending { it.first.length }.forEach { (from, to) -> t = t.replace(from, to) }
        t = t.replace(Regex("\\b([a-z]+)s\\b"), "$1")
        t = t.replace(Regex("\\begg\\s*plant\\b"), "eggplant")
        return t
    }
    private fun recipeHaystacks(recipe: Recipe): Pair<String, String> {
        val raw = buildString {
            append(recipe.title).append(' ')
            append(recipe.subtitle).append(' ')
            recipe.ingredients.forEach { append(it).append(' ') }
        }
        val canon = canonicalize(raw)
        return canon to raw.lowercase(Locale.getDefault())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_recipes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_recipes)
        recipeCountTextView = view.findViewById(R.id.tv_recipe_count)
        searchEditText = view.findViewById(R.id.et_search)
        noResultsLayout = view.findViewById(R.id.ll_no_results)
        filterIcon = view.findViewById(R.id.ic_filter_recipes)
        ddProtein = view.findViewById(R.id.dd_protein_filter)

        setupRecyclerView()
        loadRecipesFromAssets()
        setupSearchListener()
        setupFilterIconListener()
        setupProteinDropdown()

        val passed = arguments?.getStringArrayList(ARG_SCANNED_INGREDIENTS)
        if (!passed.isNullOrEmpty()) {
            scannedIngredients.clear()
            scannedIngredients.addAll(passed.map { it.lowercase(Locale.getDefault()).trim() })
            isFilterActive = true
        }

        if (scannedIngredients.isEmpty()) {
            val sp = requireContext().getSharedPreferences(SCAN_PREF, Context.MODE_PRIVATE)
            val json = sp.getString(KEY_SAVED_ING, null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<MutableList<ScannedIngredientsAdapter.ScannedIngredient>>() {}.type
                val saved: MutableList<ScannedIngredientsAdapter.ScannedIngredient> =
                    Gson().fromJson(json, type) ?: mutableListOf()
                scannedIngredients.addAll(saved.map { it.name.lowercase(Locale.getDefault()).trim() })
            }
            val proteins = sp.getStringSet(KEY_SAVED_PROTEINS, emptySet()) ?: emptySet()
            if (proteins.isNotEmpty()) scannedIngredients.addAll(proteins.map { it.lowercase(Locale.getDefault()).trim() })
            if (scannedIngredients.isNotEmpty()) isFilterActive = true
        }

        syncProteinDropdownFromState()
        applyFilters(searchEditText.text.toString().trim())
    }

    override fun onResume() {
        super.onResume()
        loadFavoriteStates()
        recipesAdapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        recipesAdapter = RecipesAdapter(
            recipes = mutableListOf(),
            onRecipeClick = { /* handled in adapter */ },
            onFavoriteClick = { recipe -> toggleFavorite(recipe) }
        )
        recyclerView.apply {
            adapter = recipesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilters(s.toString().trim()) }
        })
    }
    private fun setupFilterIconListener() {
        filterIcon.setOnClickListener {
            isFilterActive = !isFilterActive
            applyFilters(searchEditText.text.toString().trim())
        }
    }
    private fun setupProteinDropdown() {
        val items = proteinDisplayToKey.keys.toList()
        ddProtein.setSimpleItems(items.toTypedArray())
        ddProtein.setOnItemClickListener { parent, _, position, _ ->
            val label = parent.getItemAtPosition(position) as String
            val key = proteinDisplayToKey[label]
            applyProteinSelection(key)
        }
    }
    private fun syncProteinDropdownFromState() {
        val current = scannedIngredients.firstOrNull { it in proteinKeys }
        val label = proteinKeyToDisplay[current] ?: "Any protein"
        ddProtein.setText(label, false)
    }
    private fun applyProteinSelection(selectedKey: String?) {
        scannedIngredients.removeAll { it in proteinKeys }
        if (selectedKey != null) scannedIngredients.add(selectedKey)
        val sp = requireContext().getSharedPreferences(SCAN_PREF, Context.MODE_PRIVATE)
        with(sp.edit()) {
            if (selectedKey == null) putStringSet(KEY_SAVED_PROTEINS, emptySet())
            else putStringSet(KEY_SAVED_PROTEINS, setOf(selectedKey))
            apply()
        }
        isFilterActive = scannedIngredients.isNotEmpty()
        applyFilters(searchEditText.text.toString().trim())
    }

    // ---------- Data & favorites ----------

    private fun loadRecipesFromAssets() {
        try {
            val inputStream = requireContext().assets.open("recipes.json")
            val reader = BufferedReader(inputStream.reader())
            val jsonString = reader.use { it.readText() }
            val recipeListType = object : TypeToken<List<Recipe>>() {}.type
            fullRecipeList = Gson().fromJson<List<Recipe>>(jsonString, recipeListType).toMutableList()
            loadFavoriteStates()
        } catch (e: Exception) {
            e.printStackTrace()
            fullRecipeList = mutableListOf()
        }
    }

    private fun loadFavoriteStates() {
        val sp = requireContext().getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        fullRecipeList.forEach { it.isFavorite = sp.getBoolean(it.id.toString(), false) }
    }

    // IMPORTANT: use commit() to avoid first-write race after Clear Storage
    private fun saveFavoriteState(recipeId: Int, isFavorite: Boolean) {
        val sp = requireContext().getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        if (isFavorite) {
            sp.edit().putBoolean(recipeId.toString(), true).commit()
        } else {
            sp.edit().remove(recipeId.toString()).commit()
        }
    }

    private fun applyFilters(searchQuery: String) {
        var filtered = fullRecipeList.toList()
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase(Locale.getDefault())
            filtered = filtered.filter { r ->
                r.title.lowercase(Locale.getDefault()).contains(q) ||
                        r.subtitle.lowercase(Locale.getDefault()).contains(q)
            }
        }

        if (isFilterActive) {
            if (scannedIngredients.isEmpty()) {
                filtered = emptyList()
            } else {
                val scansCanon = scannedIngredients.map { canonicalize(it) }.filter { it.isNotBlank() }
                val scannedProteinKeys = scansCanon.filter { proteinSynonyms.containsKey(it) }.distinct()
                val scannedProduce = scansCanon.filter { !proteinSynonyms.containsKey(it) }.distinct()

                val withHay = filtered.map { r ->
                    val (canon, raw) = recipeHaystacks(r)
                    Triple(r, canon, raw)
                }

                var matches = withHay.filter { (_, canon, raw) ->
                    val produceOk = if (scannedProduce.isEmpty()) true else scannedProduce.all { term -> canon.contains(term) }
                    val proteinOk = if (scannedProteinKeys.isEmpty()) true else scannedProteinKeys.any { key -> containsProtein(raw, key) }
                    produceOk && proteinOk
                }.map { it.first }

                if (matches.isEmpty()) {
                    matches = withHay.filter { (_, canon, raw) ->
                        val produceOk = if (scannedProduce.isEmpty()) true else scannedProduce.any { term -> canon.contains(term) }
                        val proteinOk = if (scannedProteinKeys.isEmpty()) true else scannedProteinKeys.any { key -> containsProtein(raw, key) }
                        produceOk && proteinOk
                    }.map { it.first }
                }
                filtered = matches
            }
        }

        recipesAdapter.updateRecipes(filtered)
        updateRecipeCount(filtered.size)
        updateUIVisibility(filtered.isNotEmpty())
    }

    private fun updateRecipeCount(count: Int) {
        recipeCountTextView.text = if (isFilterActive) "Showing recipes with your ingredients ($count)"
        else "All Filipino cuisine recipes ($count)"
    }
    private fun updateUIVisibility(hasResults: Boolean) {
        recyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE
        noResultsLayout.visibility = if (hasResults) View.GONE else View.VISIBLE
    }

    private fun toggleFavorite(recipe: Recipe) {
        fullRecipeList.find { it.id == recipe.id }?.let {
            it.isFavorite = recipe.isFavorite
            saveFavoriteState(recipe.id, recipe.isFavorite) // now synchronous
        }
    }

    fun addScannedIngredient(ingredient: String) {
        val norm = ingredient.lowercase(Locale.getDefault())
        if (!scannedIngredients.contains(norm)) {
            scannedIngredients.add(norm)
            if (isFilterActive) applyFilters(searchEditText.text.toString().trim())
        }
    }
    fun clearScannedIngredients() {
        scannedIngredients.clear()
        if (isFilterActive) applyFilters(searchEditText.text.toString().trim())
        syncProteinDropdownFromState()
    }
    fun getScannedIngredients(): List<String> = scannedIngredients.toList()
}
