package com.example.testdesign

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

class MainActivity : AppCompatActivity() {

    // Tabs
    private lateinit var tabHome: LinearLayout
    private lateinit var tabScan: LinearLayout
    private lateinit var tabRecipes: LinearLayout
    private lateinit var tabFavorites: LinearLayout
    private lateinit var tabAbout: LinearLayout

    // Colors
    private val activeColor by lazy { ContextCompat.getColor(this, android.R.color.holo_orange_dark) }
    private val inactiveColor by lazy { ContextCompat.getColor(this, android.R.color.darker_gray) }

    // Nav model
    private enum class Tab(val id: Int, val tag: String) {
        HOME(R.id.tab_home, "tab_home"),
        SCAN(R.id.tab_scan, "tab_scan"),
        RECIPES(R.id.tab_recipes, "tab_recipes"),
        FAVORITES(R.id.tab_favorites, "tab_favorites"),
        ABOUT(R.id.tab_about, "tab_about")
    }

    private var activeTab = Tab.HOME
    private var lastNavClickMs = 0L
    private val navThrottleMs = 150L

    // Keep references so we don't recreate
    private val fragments = mutableMapOf<Tab, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system UI (status bar) BEFORE setContentView
        try {
            hideSystemUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContentView(R.layout.activity_main)

        initViews()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            // Pre-create all fragments once
            fragments[Tab.HOME] = HomeFragment()
            fragments[Tab.SCAN] = ScanFragment()
            fragments[Tab.RECIPES] = RecipesFragment()
            fragments[Tab.FAVORITES] = FavoritesFragment()
            fragments[Tab.ABOUT] = AboutFragment()

            val tx = supportFragmentManager.beginTransaction().setReorderingAllowed(true)

            // Add HOME visible + RESUMED
            tx.add(R.id.fragment_container, fragments[Tab.HOME]!!, Tab.HOME.tag)
            tx.setMaxLifecycle(fragments[Tab.HOME]!!, Lifecycle.State.RESUMED)

            // Add the rest hidden + STARTED
            for (tab in Tab.values()) {
                if (tab != Tab.HOME) {
                    val f = fragments[tab]!!
                    tx.add(R.id.fragment_container, f, tab.tag)
                    tx.hide(f)
                    tx.setMaxLifecycle(f, Lifecycle.State.STARTED)
                }
            }
            tx.commitNow()

            activeTab = Tab.HOME
            updateTabSelection(tabHome)
        } else {
            // Re-link existing fragments by tag after process death
            for (tab in Tab.values()) {
                supportFragmentManager.findFragmentByTag(tab.tag)?.let { fragments[tab] = it }
            }
            activeTab = detectVisibleTab() ?: Tab.HOME

            // Normalize lifecycle: active = RESUMED, others = STARTED
            val tx = supportFragmentManager.beginTransaction().setReorderingAllowed(true)
            for (tab in Tab.values()) {
                val f = fragments[tab] ?: continue
                if (tab == activeTab) {
                    if (f.isHidden) tx.show(f)
                    tx.setMaxLifecycle(f, Lifecycle.State.RESUMED)
                } else {
                    if (f.isVisible) tx.hide(f)
                    tx.setMaxLifecycle(f, Lifecycle.State.STARTED)
                }
            }
            tx.commitNow()

            updateTabSelection(getTabView(activeTab))
        }
    }

    // --- System UI Hiding ---

    private fun hideSystemUI() {
        // Make the activity full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            try {
                hideSystemUI()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Setup ---

    private fun initViews() {
        tabHome = findViewById(R.id.tab_home)
        tabScan = findViewById(R.id.tab_scan)
        tabRecipes = findViewById(R.id.tab_recipes)
        tabFavorites = findViewById(R.id.tab_favorites)
        tabAbout = findViewById(R.id.tab_about)
    }

    private fun setupBottomNavigation() {
        tabHome.setOnClickListener { navigate(Tab.HOME) }
        tabScan.setOnClickListener { navigate(Tab.SCAN) }
        tabRecipes.setOnClickListener { navigate(Tab.RECIPES) }
        tabFavorites.setOnClickListener { navigate(Tab.FAVORITES) }
        tabAbout.setOnClickListener { navigate(Tab.ABOUT) }
    }

    // --- Nav core (instant switching) ---

    private fun navigate(target: Tab) {
        val now = SystemClock.uptimeMillis()
        if (now - lastNavClickMs < navThrottleMs) return
        lastNavClickMs = now
        if (target == activeTab) return

        val current = fragments[activeTab]
        val next = fragments[target] ?: createAndStore(target)

        val tx = supportFragmentManager.beginTransaction().setReorderingAllowed(true)
        if (current != null) {
            tx.hide(current)
            tx.setMaxLifecycle(current, Lifecycle.State.STARTED)
        }
        tx.show(next)
        tx.setMaxLifecycle(next, Lifecycle.State.RESUMED)
        tx.commitNow()

        activeTab = target
        updateTabSelection(getTabView(target))
    }

    private fun createAndStore(tab: Tab): Fragment {
        val f = when (tab) {
            Tab.HOME -> HomeFragment()
            Tab.SCAN -> ScanFragment()
            Tab.RECIPES -> RecipesFragment()
            Tab.FAVORITES -> FavoritesFragment()
            Tab.ABOUT -> AboutFragment()
        }
        fragments[tab] = f
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.fragment_container, f, tab.tag)
            .hide(f)
            .setMaxLifecycle(f, Lifecycle.State.STARTED)
            .commitNow()
        return f
    }

    private fun detectVisibleTab(): Tab? {
        val fm = supportFragmentManager
        return Tab.values().firstOrNull { tab ->
            fm.findFragmentByTag(tab.tag)?.isVisible == true
        }
    }

    private fun getTabView(tab: Tab): LinearLayout = when (tab) {
        Tab.HOME -> tabHome
        Tab.SCAN -> tabScan
        Tab.RECIPES -> tabRecipes
        Tab.FAVORITES -> tabFavorites
        Tab.ABOUT -> tabAbout
    }

    // --- Tab visuals ---

    private fun updateTabSelection(selectedTab: LinearLayout) {
        setTabInactive(tabHome)
        setTabInactive(tabScan)
        setTabInactive(tabRecipes)
        setTabInactive(tabFavorites)
        setTabInactive(tabAbout)
        setTabActive(selectedTab)
    }

    private fun setTabActive(tab: LinearLayout) {
        val imageView = tab.getChildAt(0) as? ImageView
        val textView = tab.getChildAt(1) as? TextView
        imageView?.setColorFilter(activeColor)
        textView?.setTextColor(activeColor)

        if (tab.childCount == 2) {
            val indicator = View(this)
            val params = LinearLayout.LayoutParams(dpToPx(20), dpToPx(2))
            params.topMargin = dpToPx(4)
            indicator.layoutParams = params
            indicator.setBackgroundColor(activeColor)
            tab.addView(indicator)
        }
    }

    private fun setTabInactive(tab: LinearLayout) {
        val imageView = tab.getChildAt(0) as? ImageView
        val textView = tab.getChildAt(1) as? TextView
        imageView?.setColorFilter(inactiveColor)
        textView?.setTextColor(inactiveColor)
        if (tab.childCount > 2) tab.removeViewAt(2)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // --- Public helpers for other fragments ---

    fun openRecipesTab() {
        navigate(Tab.RECIPES)
    }

    fun openRecipesWithScannedFilter(scanned: ArrayList<String>) {
        val current = fragments[activeTab]
        val newRecipes = RecipesFragment.newInstance(scanned)
        fragments[Tab.RECIPES] = newRecipes

        val tx = supportFragmentManager.beginTransaction().setReorderingAllowed(true)
        supportFragmentManager.findFragmentByTag(Tab.RECIPES.tag)?.let { tx.remove(it) }
        tx.add(R.id.fragment_container, newRecipes, Tab.RECIPES.tag)
        if (current != null && current != newRecipes) {
            tx.hide(current)
            tx.setMaxLifecycle(current, Lifecycle.State.STARTED)
        }
        tx.show(newRecipes)
        tx.setMaxLifecycle(newRecipes, Lifecycle.State.RESUMED)
        tx.commitNow()

        activeTab = Tab.RECIPES
        updateTabSelection(tabRecipes)
    }
}