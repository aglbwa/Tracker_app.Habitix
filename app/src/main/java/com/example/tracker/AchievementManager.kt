package com.example.tracker

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class AchievementManager {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    companion object {
        private const val TAG = "AchievementManager"
    }

    // Проверка достижений при создании привычки
    fun checkHabitCreationAchievements(totalHabitsCount: Int) {
        if (currentUserId.isEmpty()) return

        Log.d(TAG, "🔍 Проверка достижений по созданию привычек: $totalHabitsCount")

        // Первая привычка
        if (totalHabitsCount >= 1) {
            grantAchievement("first_habit")
        }

        // 5 привычек
        if (totalHabitsCount >= 5) {
            grantAchievement("five_habits")
        }

        // 10 привычек
        if (totalHabitsCount >= 10) {
            grantAchievement("ten_habits")
        }
    }

    // Проверка достижений при выполнении привычки
    fun checkHabitCompletionAchievements(totalCompletions: Int, points: Int) {
        if (currentUserId.isEmpty()) return

        Log.d(TAG, "🔍 Проверка достижений по выполнению: $totalCompletions выполнений, $points очков")

        // Первое выполнение
        if (totalCompletions >= 1) {
            grantAchievement("first_completion")
        }

        // 10 выполнений
        if (totalCompletions >= 10) {
            grantAchievement("ten_completions")
        }

        // 50 выполнений
        if (totalCompletions >= 50) {
            grantAchievement("fifty_completions")
        }

        // Проверка достижений по очкам
        checkPointsAchievements(points)
    }

    // Проверка достижений по очкам - ИЗМЕНИЛОСЬ: теперь public
    fun checkPointsAchievements(totalPoints: Int) {
        if (currentUserId.isEmpty()) return

        Log.d(TAG, "🔍 Проверка достижений по очкам: $totalPoints очков")

        if (totalPoints >= 100) {
            grantAchievement("hundred_points")
        }
        if (totalPoints >= 500) {
            grantAchievement("five_hundred_points")
        }
        if (totalPoints >= 1000) {
            grantAchievement("thousand_points")
        }
    }

    // Проверка достижений по стрикам
    fun checkStreakAchievements(currentStreak: Int) {
        if (currentUserId.isEmpty()) return

        Log.d(TAG, "🔍 Проверка достижений по стрику: $currentStreak дней")

        if (currentStreak >= 3) {
            grantAchievement("streak_3")
        }
        if (currentStreak >= 7) {
            grantAchievement("streak_7")
        }
        if (currentStreak >= 30) {
            grantAchievement("streak_30")
        }
    }

    // Проверка специальных достижений
    fun checkSpecialAchievements(isEarlyMorning: Boolean = false, isLateNight: Boolean = false) {
        if (currentUserId.isEmpty()) return

        Log.d(TAG, "🔍 Проверка специальных достижений: ранняя пташка=$isEarlyMorning, ночная сова=$isLateNight")

        if (isEarlyMorning) {
            grantAchievement("early_bird")
        }
        if (isLateNight) {
            grantAchievement("night_owl")
        }
    }

    // Выдача достижения пользователю - оставляем private
    private fun grantAchievement(achievementId: String) {
        val achievementRef = db.collection("achievements")
            .document("${currentUserId}_$achievementId")

        // Проверяем, не получено ли уже это достижение
        achievementRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Достижение еще не получено - выдаем его
                    val achievementData = hashMapOf(
                        "achievementId" to achievementId,
                        "userId" to currentUserId,
                        "dateEarned" to Date(),
                        "title" to getAchievementTitle(achievementId),
                        "description" to getAchievementDescription(achievementId)
                    )

                    achievementRef.set(achievementData)
                        .addOnSuccessListener {
                            Log.d(TAG, "🎉 Достижение получено: $achievementId")
                            // Здесь можно добавить уведомление или анимацию
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Ошибка выдачи достижения: ${e.message}")
                        }
                } else {
                    Log.d(TAG, "ℹ️ Достижение уже получено: $achievementId")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка проверки достижения: ${e.message}")
            }
    }

    // Вспомогательные методы для получения информации о достижениях
    private fun getAchievementTitle(achievementId: String): String {
        return AchievementActivity.ALL_ACHIEVEMENTS
            .find { it.achievementId == achievementId }
            ?.title ?: "Достижение"
    }

    private fun getAchievementDescription(achievementId: String): String {
        return AchievementActivity.ALL_ACHIEVEMENTS
            .find { it.achievementId == achievementId }
            ?.description ?: "Описание достижения"
    }
}