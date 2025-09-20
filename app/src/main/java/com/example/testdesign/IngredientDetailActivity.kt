package com.example.testdesign

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class IngredientDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CORE = "extra_core"

        fun newIntent(ctx: Context, coreName: String) =
            Intent(ctx, IngredientDetailActivity::class.java).apply {
                putExtra(EXTRA_CORE, coreName)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // reuse a generic container layout
        setContentView(R.layout.activity_fragment_container)

        if (savedInstanceState == null) {
            val core = intent.getStringExtra(EXTRA_CORE) ?: return
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, IngredientDetailFragment.newInstance(core))
                .commit()
        }
    }
}
