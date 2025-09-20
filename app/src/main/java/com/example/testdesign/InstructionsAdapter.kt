package com.example.testdesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InstructionsAdapter(private val instructions: List<String>) :
    RecyclerView.Adapter<InstructionsAdapter.InstructionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_instruction, parent, false)
        return InstructionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstructionViewHolder, position: Int) {
        holder.bind(instructions[position], position + 1)
    }

    override fun getItemCount(): Int = instructions.size

    class InstructionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStepNumber: TextView = itemView.findViewById(R.id.tvStepNumber)
        private val tvInstruction: TextView = itemView.findViewById(R.id.tvInstruction)

        fun bind(instruction: String, stepNumber: Int) {
            tvStepNumber.text = stepNumber.toString()
            tvInstruction.text = instruction
        }
    }
}