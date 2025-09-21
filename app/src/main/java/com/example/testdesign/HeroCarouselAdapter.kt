package com.example.testdesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class HeroCarouselAdapter(
    private val drawables: List<Int>
) : RecyclerView.Adapter<HeroCarouselAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.iv_slide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hero_slide, parent, false)
        return VH(v)
    }

    // Fake-infinite list
    override fun getItemCount(): Int = if (drawables.isEmpty()) 0 else Int.MAX_VALUE

    override fun onBindViewHolder(holder: VH, position: Int) {
        val resId = drawables[position % drawables.size]
        holder.image.setImageResource(resId)
    }
}
