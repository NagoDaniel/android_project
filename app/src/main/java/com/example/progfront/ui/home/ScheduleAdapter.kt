package com.example.progfront.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.progfront.R
import com.example.progfront.data.model.ScheduleResponse
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ScheduleAdapter(
    private val onStatusToggle: (ScheduleResponse) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private val items = mutableListOf<ScheduleResponse>()
    private val updatingIds = mutableSetOf<Int>()

    fun submitList(list: List<ScheduleResponse>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun markUpdating(id: Int, updating: Boolean) {
        if (updating) updatingIds.add(id) else updatingIds.remove(id)
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view, ::isUpdating, onStatusToggle)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    private fun isUpdating(id: Int) = updatingIds.contains(id)

    class ScheduleViewHolder(
        itemView: View,
        private val isUpdating: (Int) -> Boolean,
        private val onStatusToggle: (ScheduleResponse) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textHabitName: TextView = itemView.findViewById(R.id.textHabitName)
        private val textNotes: TextView = itemView.findViewById(R.id.textNotes)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)

        private val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        private val outputTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val utcZone = TimeZone.getTimeZone("UTC")

        fun bind(item: ScheduleResponse) {
            textTime.text = formatTime(item.start_time)
            textHabitName.text = item.habit?.name ?: "Habit #${item.habitId ?: "?"}"
            textNotes.text = item.notes ?: ""
            textNotes.visibility = if (item.notes.isNullOrBlank()) View.GONE else View.VISIBLE
            textStatus.text = item.status

            val updating = isUpdating(item.id)
            textStatus.isEnabled = !updating
            textStatus.alpha = if (updating) 0.5f else 1f

            styleStatus(item.status)

            textStatus.setOnClickListener {
                if (!updating) onStatusToggle(item)
            }
        }

        private fun styleStatus(status: String) {
            when (status.lowercase(Locale.getDefault())) {
                "planned" -> {
                    textStatus.setBackgroundResource(R.drawable.spinner_background)
                    textStatus.setTextColor(Color.parseColor("#333333"))
                }
                "completed" -> {
                    textStatus.setBackgroundColor(Color.parseColor("#C8E6C9"))
                    textStatus.setTextColor(Color.parseColor("#1B5E20"))
                }
                "skipped" -> {
                    textStatus.setBackgroundColor(Color.parseColor("#FFCDD2"))
                    textStatus.setTextColor(Color.parseColor("#B71C1C"))
                }
                else -> {
                    textStatus.setBackgroundResource(R.drawable.spinner_background)
                    textStatus.setTextColor(Color.parseColor("#333333"))
                }
            }
        }

        private fun formatTime(raw: String): String {
            patterns.forEach { p ->
                try {
                    val sdf = SimpleDateFormat(p, Locale.getDefault())
                    if (p.contains("'Z'")) sdf.timeZone = utcZone
                    val date = sdf.parse(raw)
                    if (date != null) return outputTime.format(date)
                } catch (_: ParseException) {
                }
            }
            return raw.substringAfter('T').take(5)
        }
    }
}
