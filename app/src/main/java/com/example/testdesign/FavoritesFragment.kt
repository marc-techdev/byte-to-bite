package com.example.testdesign

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class FavoritesFragment : Fragment() {

    private lateinit var favoritesAdapter: RecipesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var favoriteCountTextView: TextView
    private lateinit var noFavoritesLayout: LinearLayout

    private var allRecipes: List<Recipe> = emptyList()
    private var favoriteRecipes: MutableList<Recipe> = mutableListOf()
    private var filteredRecipes: MutableList<Recipe> = mutableListOf()
    private lateinit var sharedPref: SharedPreferences

    companion object {
        private const val FAVORITES_PREF = "recipe_favorites"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_favorites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_favorites)
        searchEditText = view.findViewById(R.id.et_search)
        favoriteCountTextView = view.findViewById(R.id.tv_favorite_count)
        noFavoritesLayout = view.findViewById(R.id.ll_no_favorites)

        sharedPref = requireContext().getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)

        setupRecyclerView()
        loadAllRecipes()
        loadFavoriteRecipes()
        setupSearch()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = RecipesAdapter(
            recipes = mutableListOf(),
            onRecipeClick = { /* Handle click */ },
            onFavoriteClick = { recipe -> toggleFavorite(recipe) }
        )

        recyclerView.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterFavorites(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterFavorites(query: String) {
        filteredRecipes = if (query.isEmpty()) {
            favoriteRecipes.toMutableList()
        } else {
            favoriteRecipes.filter {
                it.title.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        favoritesAdapter.updateRecipes(filteredRecipes)
        updateUIVisibility(filteredRecipes.isNotEmpty())
        updateFavoriteCount(filteredRecipes.size)
    }

    private fun loadAllRecipes() {
        try {
            val inputStream = requireContext().assets.open("recipes.json")
            val reader = BufferedReader(inputStream.reader())
            val jsonString = reader.use { it.readText() }

            val recipeListType = object : TypeToken<List<Recipe>>() {}.type
            allRecipes = Gson().fromJson(jsonString, recipeListType)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFavoriteRecipes() {
        favoriteRecipes.clear()

        val favoriteIds = mutableSetOf<String>()
        sharedPref.all.forEach { (key, value) ->
            if (value == true) favoriteIds.add(key)
        }

        allRecipes.forEach { recipe ->
            if (favoriteIds.contains(recipe.id.toString())) {
                recipe.isFavorite = true
                favoriteRecipes.add(recipe)
            }
        }

        filterFavorites(searchEditText.text.toString())
    }

    private fun updateFavoriteCount(count: Int) {
        favoriteCountTextView.text = "My favorite recipes ($count)"
    }

    private fun updateUIVisibility(hasFavorites: Boolean) {
        recyclerView.visibility = if (hasFavorites) View.VISIBLE else View.GONE
        noFavoritesLayout.visibility = if (hasFavorites) View.GONE else View.VISIBLE
    }

    private fun toggleFavorite(recipe: Recipe) {
        recipe.isFavorite = false
        with(sharedPref.edit()) {
            remove(recipe.id.toString())
            apply()
        }

        favoriteRecipes.removeAll { it.id == recipe.id }
        filterFavorites(searchEditText.text.toString())
    }
}
