package com.example.tracker

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AchievementAdapter : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    companion object {
        private const val TAG = "AchievementAdapter"
    }

    // ИСПРАВЛЕНО: Используем один список в адаптере
    private val achievements = mutableListOf<Achievement>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        Log.d(TAG, "🔧 onCreateViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        Log.d(TAG, "🔹 Отображаем достижение [$position]: ${achievement.title} (получено: ${achievement.isEarned})")
        holder.bind(achievement)
    }

    override fun getItemCount(): Int {
        val count = achievements.size
        Log.d(TAG, "📊 getItemCount: $count")
        return count
    }

    fun updateAchievements(newAchievements: List<Achievement>) {
        Log.d(TAG, "🔄 Обновление списка достижений: ${newAchievements.size}")
        Log.d(TAG, "📊 Полученных: ${newAchievements.count { it.isEarned }}")
        Log.d(TAG, "🔒 Заблокированных: ${newAchievements.count { !it.isEarned }}")

        achievements.clear()
        achievements.addAll(newAchievements)
        notifyDataSetChanged()
        Log.d(TAG, "✅ Адаптер обновлен, теперь содержит ${achievements.size} элементов")

        // Проверим содержимое
        achievements.forEachIndexed { index, achievement ->
            Log.d(TAG, "📋 [$index] ${achievement.title} - ${achievement.isEarned}")
        }
    }

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAchievementIcon: TextView = itemView.findViewById(R.id.tvAchievementIcon)
        private val tvAchievementTitle: TextView = itemView.findViewById(R.id.tvAchievementTitle)
        private val tvAchievementDescription: TextView = itemView.findViewById(R.id.tvAchievementDescription)
        private val tvAchievementStatus: TextView = itemView.findViewById(R.id.tvAchievementStatus)

        fun bind(achievement: Achievement) {
            Log.d(TAG, "🎯 Привязываем: ${achievement.title}")

            tvAchievementIcon.text = achievement.icon
            tvAchievementTitle.text = achievement.title
            tvAchievementDescription.text = achievement.description

            if (achievement.isEarned) {
                tvAchievementStatus.text = "✅ Получено"
                tvAchievementStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                itemView.setBackgroundColor(Color.parseColor("#2A2B36"))
                tvAchievementTitle.setTextColor(Color.WHITE)
                tvAchievementDescription.setTextColor(Color.parseColor("#CCCCCC"))
                Log.d(TAG, "✅ Отображаем как полученное: ${achievement.title}")
            } else {
                tvAchievementStatus.text = "🔒 Заблокировано"
                tvAchievementStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                itemView.setBackgroundColor(Color.parseColor("#1A1A1A"))
                tvAchievementTitle.setTextColor(Color.parseColor("#888888"))
                tvAchievementDescription.setTextColor(Color.parseColor("#666666"))
                Log.d(TAG, "🔒 Отображаем как заблокированное: ${achievement.title}")
            }
        }
    }
}