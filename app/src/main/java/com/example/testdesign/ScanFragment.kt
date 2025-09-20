package com.example.testdesign

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScanFragment : Fragment() {

    private lateinit var btnScanIngredient: AppCompatButton
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvIngredients: RecyclerView
    private lateinit var btnClearAll: TextView
    private lateinit var btnFindRecipes: AppCompatButton
    private lateinit var btnAddProtein: AppCompatButton

    private lateinit var scannedIngredientsAdapter: ScannedIngredientsAdapter
    private val scannedIngredients = mutableListOf<ScannedIngredientsAdapter.ScannedIngredient>()

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gson: Gson

    companion object {
        private const val PREF_NAME = "scan_ingredients_prefs"
        private const val KEY_INGREDIENTS = "scanned_ingredients"
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val ingredientName = data?.getStringExtra(CameraActivity.EXTRA_SCANNED_INGREDIENT)
            val confidence = data?.getFloatExtra(CameraActivity.EXTRA_CONFIDENCE, 0f) ?: 0f
            if (!ingredientName.isNullOrBlank()) addScannedIngredient(ingredientName, confidence)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_scan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        gson = Gson()

        btnScanIngredient = view.findViewById(R.id.btn_scan_ingredient)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        rvIngredients = view.findViewById(R.id.rv_ingredients)
        btnClearAll = view.findViewById(R.id.btn_clear_all)
        btnFindRecipes = view.findViewById(R.id.btn_find_recipes)
        btnAddProtein = view.findViewById(R.id.btn_add_protein)

        setupRecycler()
        setupClicks()
        loadSavedIngredients()
        updateUIState()
    }

    private fun setupRecycler() {
        scannedIngredientsAdapter = ScannedIngredientsAdapter(
            ingredients = scannedIngredients,
            onRemoveClick = { _, position -> removeIngredient(position) }
        )
        rvIngredients.apply {
            adapter = scannedIngredientsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClicks() {
        btnScanIngredient.setOnClickListener { openCamera() }

        btnClearAll.setOnClickListener { clearAllIngredients() }

        btnFindRecipes.setOnClickListener {
            // Build the names list (includes proteins you added via dialog)
            val names = ArrayList(scannedIngredients.map { it.name })
            val act = activity as? MainActivity
            if (names.isEmpty()) {
                // No toast, just open Recipes tab normally
                act?.openRecipesTab()
            } else {
                act?.openRecipesWithScannedFilter(names)
            }
        }

        btnAddProtein.setOnClickListener { showAddProteinDialog() }
    }

    private fun openCamera() {
        cameraLauncher.launch(Intent(requireContext(), CameraActivity::class.java))
    }

    private fun addScannedIngredient(name: String, confidence: Float) {
        val existingIndex = scannedIngredients.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        val item = ScannedIngredientsAdapter.ScannedIngredient(name = name, confidence = confidence)
        if (existingIndex >= 0) {
            if (confidence > scannedIngredients[existingIndex].confidence) {
                scannedIngredients[existingIndex] = item
                scannedIngredientsAdapter.notifyItemChanged(existingIndex)
            }
        } else {
            scannedIngredients.add(item)
            scannedIngredientsAdapter.notifyItemInserted(scannedIngredients.size - 1)
        }
        updateUIState()
        saveIngredients()
    }

    private fun addManualIngredient(name: String) {
        val existingIndex = scannedIngredients.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        val item = ScannedIngredientsAdapter.ScannedIngredient(name = name, confidence = 1.0f)
        if (existingIndex >= 0) {
            if (1.0f > scannedIngredients[existingIndex].confidence) {
                scannedIngredients[existingIndex] = item
                scannedIngredientsAdapter.notifyItemChanged(existingIndex)
            }
        } else {
            scannedIngredients.add(item)
            scannedIngredientsAdapter.notifyItemInserted(scannedIngredients.size - 1)
        }
        updateUIState()
        saveIngredients()
    }

    private fun removeIngredient(position: Int) {
        if (position in scannedIngredients.indices) {
            scannedIngredients.removeAt(position)
            scannedIngredientsAdapter.notifyItemRemoved(position)
            scannedIngredientsAdapter.notifyItemRangeChanged(position, scannedIngredients.size)
            updateUIState()
            saveIngredients()
        }
    }

    private fun clearAllIngredients() {
        val size = scannedIngredients.size
        scannedIngredients.clear()
        if (size > 0) scannedIngredientsAdapter.notifyItemRangeRemoved(0, size)
        updateUIState()
        saveIngredients()
    }

    private fun updateUIState() {
        val hasItems = scannedIngredients.isNotEmpty()
        layoutEmptyState.visibility = if (hasItems) View.GONE else View.VISIBLE
        rvIngredients.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    private fun saveIngredients() {
        try {
            val json = gson.toJson(scannedIngredients)
            sharedPreferences.edit().putString(KEY_INGREDIENTS, json).apply()
        } catch (_: Exception) {}
    }

    private fun loadSavedIngredients() {
        try {
            val json = sharedPreferences.getString(KEY_INGREDIENTS, null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<MutableList<ScannedIngredientsAdapter.ScannedIngredient>>() {}.type
                val saved: MutableList<ScannedIngredientsAdapter.ScannedIngredient> =
                    gson.fromJson(json, type) ?: mutableListOf()
                scannedIngredients.clear()
                scannedIngredients.addAll(saved)
                scannedIngredientsAdapter.notifyDataSetChanged()
            }
        } catch (_: Exception) {
            scannedIngredients.clear()
        }
    }

    private fun showAddProteinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_protein, null)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chipGroup)
        val btnCancel = dialogView.findViewById<AppCompatButton>(R.id.btnCancel)
        val btnAdd = dialogView.findViewById<AppCompatButton>(R.id.btnAdd)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnAdd.setOnClickListener {
            val selected = mutableListOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                if (chip.isChecked) selected.add(chip.text.toString())
            }
            selected.forEach { addManualIngredient(it) }
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        saveIngredients()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveIngredients()
    }
}
