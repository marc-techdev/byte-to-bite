package com.example.testdesign

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class MainActivity : AppCompatActivity() {

    private lateinit var tabHome: LinearLayout
    private lateinit var tabScan: LinearLayout
    private lateinit var tabRecipes: LinearLayout
    private lateinit var tabFavorites: LinearLayout
    private lateinit var tabAbout: LinearLayout

    // Colors
    private val activeColor by lazy { ContextCompat.getColor(this, android.R.color.holo_orange_dark) }
    private val inactiveColor by lazy { ContextCompat.getColor(this, android.R.color.darker_gray) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBottomNavigation()

        // Default: Home
        loadFragment(HomeFragment())
        updateTabSelection(tabHome)
    }

    private fun initViews() {
        tabHome = findViewById(R.id.tab_home)
        tabScan = findViewById(R.id.tab_scan)
        tabRecipes = findViewById(R.id.tab_recipes)
        tabFavorites = findViewById(R.id.tab_favorites)
        tabAbout = findViewById(R.id.tab_about)
    }

    private fun setupBottomNavigation() {
        tabHome.setOnClickListener {
            loadFragment(HomeFragment())
            updateTabSelection(tabHome)
        }

        tabScan.setOnClickListener {
            loadFragment(ScanFragment())
            updateTabSelection(tabScan)
        }

        tabRecipes.setOnClickListener {
            loadFragment(RecipesFragment())
            updateTabSelection(tabRecipes)
        }

        tabFavorites.setOnClickListener {
            loadFragment(FavoritesFragment())
            updateTabSelection(tabFavorites)
        }

        tabAbout.setOnClickListener {
            loadFragment(AboutFragment())
            updateTabSelection(tabAbout)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    private fun updateTabSelection(selectedTab: LinearLayout) {
        // Reset
        setTabInactive(tabHome)
        setTabInactive(tabScan)
        setTabInactive(tabRecipes)
        setTabInactive(tabFavorites)
        setTabInactive(tabAbout)
        // Activate
        setTabActive(selectedTab)
    }

    private fun setTabActive(tab: LinearLayout) {
        val imageView = tab.getChildAt(0) as ImageView
        imageView.setColorFilter(activeColor)

        val textView = tab.getChildAt(1) as TextView
        textView.setTextColor(activeColor)

        if (tab.childCount == 2) {
            val activeIndicator = android.view.View(this)
            val params = LinearLayout.LayoutParams(dpToPx(20), dpToPx(2))
            params.topMargin = dpToPx(4)
            activeIndicator.layoutParams = params
            activeIndicator.setBackgroundColor(activeColor)
            tab.addView(activeIndicator)
        }
    }

    private fun setTabInactive(tab: LinearLayout) {
        val imageView = tab.getChildAt(0) as ImageView
        imageView.setColorFilter(inactiveColor)

        val textView = tab.getChildAt(1) as TextView
        textView.setTextColor(inactiveColor)

        if (tab.childCount > 2) {
            tab.removeViewAt(2)
        }
    }

    // MainActivity.kt
    fun openRecipesTab() {
        loadFragment(RecipesFragment())
        updateTabSelection(tabRecipes)
    }


    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    /** ✅ Public helper for ScanFragment: open Recipes with scanned-ingredients filter applied */
    fun openRecipesWithScannedFilter(scanned: ArrayList<String>) {
        val fragment = RecipesFragment.newInstance(scanned)
        loadFragment(fragment)
        updateTabSelection(tabRecipes)
    }
}
