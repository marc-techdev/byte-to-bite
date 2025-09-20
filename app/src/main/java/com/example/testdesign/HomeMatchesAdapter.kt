package com.example.testdesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeMatchesAdapter(
    private var items: MutableList<MatchedRecipe>,
    private val onClick: (Recipe) -> Unit
) : RecyclerView.Adapter<HomeMatchesAdapter.VH>() {

    data class MatchedRecipe(
        val recipe: Recipe,
        val matchCount: Int,
        val totalScanned: Int
    )

    fun submitList(newItems: List<MatchedRecipe>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_match, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvMatch: TextView = itemView.findViewById(R.id.tvMatch)

        fun bind(item: MatchedRecipe, onClick: (Recipe) -> Unit) {
            val ctx = itemView.context
            val r = item.recipe

            tvTitle.text = r.title
            tvMeta.text = "${r.cookingTime} • ${r.servings} • ${r.difficulty}"
            tvMatch.text = if (item.totalScanned > 0)
                "Matches: ${item.matchCount} of ${item.totalScanned}"
            else
                "Matches: 0"

            // Load image from drawable name
            r.imageUrl?.let { name ->
                val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
                if (resId != 0) ivThumb.setImageResource(resId)
                else ivThumb.setImageResource(R.drawable.placeholder_recipe)
            } ?: ivThumb.setImageResource(R.drawable.placeholder_recipe)

            itemView.setOnClickListener { onClick(r) }
        }
    }
}
