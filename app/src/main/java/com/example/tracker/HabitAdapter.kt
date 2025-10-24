package com.example.tracker

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private val onHabitChecked: (Habit, Boolean) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    companion object {
        private const val TAG = "HabitAdapter"
    }

    private val habits = mutableListOf<Habit>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        Log.d(TAG, "🔧 onCreateViewHolder вызван")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        Log.d(TAG, "🔹 Отображаем привычку [$position]: ${habit.title}")
        holder.bind(habit)
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "📊 getItemCount: ${habits.size}")
        return habits.size
    }

    fun updateHabits(newHabits: List<Habit>) {
        Log.d(TAG, "🔄 Обновление адаптера: ${newHabits.size} привычек")
        habits.clear()
        habits.addAll(newHabits)
        Log.d(TAG, "✅ Адаптер обновлен, теперь содержит ${habits.size} элементов")
        notifyDataSetChanged()
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val tvHabitDescription: TextView = itemView.findViewById(R.id.tvHabitDescription)
        private val tvHabitPoints: TextView = itemView.findViewById(R.id.tvHabitPoints)
        private val cbHabitCompleted: CheckBox = itemView.findViewById(R.id.cbHabitCompleted)

        fun bind(habit: Habit) {
            tvHabitName.text = habit.title
            tvHabitDescription.text = habit.description
            tvHabitPoints.text = "+${habit.points} монет"

            cbHabitCompleted.setOnCheckedChangeListener(null)
            cbHabitCompleted.isChecked = habit.isCompleted
            cbHabitCompleted.setOnCheckedChangeListener { _, isChecked ->
                onHabitChecked(habit, isChecked)
            }
        }
    }
}