package com.example.progfront.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.progfront.R
import com.example.progfront.data.model.HabitResponse

class ProfileHabitsAdapter : RecyclerView.Adapter<ProfileHabitsAdapter.VH>() {

    private val items = mutableListOf<HabitResponse>()
    // Map of habitId to progress percent (0..100)
    private val progressPercents = mutableMapOf<Int, Int>()

    fun submit(list: List<HabitResponse>) {
        items.clear()
        items.addAll(list)
        // Reset progress when list changes
        progressPercents.clear()
        notifyDataSetChanged()
    }

    fun updateProgress(map: Map<Int, Int>) {
        progressPercents.clear()
        progressPercents.putAll(map)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_habit, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], progressPercents[items[position].id] ?: 0)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.textHabitName)
        private val goal: TextView = itemView.findViewById(R.id.textHabitGoal)
        private val desc: TextView = itemView.findViewById(R.id.textHabitDescription)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressHabit)
        private val percentText: TextView = itemView.findViewById(R.id.textHabitProgressPercent)

        fun bind(item: HabitResponse, percent: Int) {
            name.text = item.name
            goal.text = item.goal
            desc.text = item.description.orEmpty()
            desc.visibility = if (item.description.isNullOrBlank()) View.GONE else View.VISIBLE

            val safePercent = percent.coerceIn(0, 100)
            progressBar.max = 100
            progressBar.progress = safePercent
            percentText.text = itemView.context.getString(R.string.schedule_progress_percent, safePercent)
        }
    }
}
