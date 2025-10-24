package com.example.tracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class AchievementActivity : AppCompatActivity() {

    private lateinit var rvAchievements: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var achievementAdapter: AchievementAdapter

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    companion object {
        private const val TAG = "AchievementActivity"

        // Полный список всех возможных достижений
        val ALL_ACHIEVEMENTS = listOf(
            // Достижения по созданию привычек
            Achievement(
                achievementId = "first_habit",
                title = "Первые шаги",
                description = "Создайте свою первую привычку",
                pointsRequired = 1,
                icon = "🎯",
                type = "habit_creation"
            ),
            Achievement(
                achievementId = "five_habits",
                title = "Собиратель привычек",
                description = "Создайте 5 различных привычек",
                pointsRequired = 5,
                icon = "📝",
                type = "habit_creation"
            ),
            Achievement(
                achievementId = "ten_habits",
                title = "Мастер привычек",
                description = "Создайте 10 различных привычек",
                pointsRequired = 10,
                icon = "💪",
                type = "habit_creation"
            ),

            // Достижения по выполнению привычек
            Achievement(
                achievementId = "first_completion",
                title = "Первый успех",
                description = "Впервые выполните любую привычку",
                pointsRequired = 1,
                icon = "✅",
                type = "habit_completion"
            ),
            Achievement(
                achievementId = "ten_completions",
                title = "Десять побед",
                description = "Выполните привычки 10 раз",
                pointsRequired = 10,
                icon = "🔥",
                type = "habit_completion"
            ),
            Achievement(
                achievementId = "fifty_completions",
                title = "Полтинник",
                description = "Выполните привычки 50 раз",
                pointsRequired = 50,
                icon = "⭐",
                type = "habit_completion"
            ),

            // Достижения по стрикам
            Achievement(
                achievementId = "streak_3",
                title = "Трехдневный стрик",
                description = "Выполняйте привычки 3 дня подряд",
                pointsRequired = 3,
                icon = "📅",
                type = "streak"
            ),
            Achievement(
                achievementId = "streak_7",
                title = "Недельный чемпион",
                description = "Выполняйте привычки 7 дней подряд",
                pointsRequired = 7,
                icon = "🏆",
                type = "streak"
            ),
            Achievement(
                achievementId = "streak_30",
                title = "Месяц дисциплины",
                description = "Выполняйте привычки 30 дней подряд",
                pointsRequired = 30,
                icon = "👑",
                type = "streak"
            ),

            // Достижения по очкам
            Achievement(
                achievementId = "hundred_points",
                title = "Сотня очков",
                description = "Заработайте 100 очков",
                pointsRequired = 100,
                icon = "💯",
                type = "points"
            ),
            Achievement(
                achievementId = "five_hundred_points",
                title = "Пятьсот очков",
                description = "Заработайте 500 очков",
                pointsRequired = 500,
                icon = "💰",
                type = "points"
            ),
            Achievement(
                achievementId = "thousand_points",
                title = "Тысяча очков",
                description = "Заработайте 1000 очков",
                pointsRequired = 1000,
                icon = "🎖️",
                type = "points"
            ),

            // Специальные достижения
            Achievement(
                achievementId = "early_bird",
                title = "Ранняя пташка",
                description = "Выполните привычку до 8 утра",
                pointsRequired = 1,
                icon = "🌅",
                type = "special"
            ),
            Achievement(
                achievementId = "night_owl",
                title = "Ночная сова",
                description = "Выполните привычку после 10 вечера",
                pointsRequired = 1,
                icon = "🌙",
                type = "special"
            ),
            Achievement(
                achievementId = "perfect_week",
                title = "Идеальная неделя",
                description = "Выполните все привычки каждый день недели",
                pointsRequired = 7,
                icon = "🌟",
                type = "special"
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        supportActionBar?.hide()

        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadUserAchievements()
    }

    private fun initViews() {
        rvAchievements = findViewById(R.id.rvAchievements)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "🔧 Настройка RecyclerView")

        achievementAdapter = AchievementAdapter()
        rvAchievements.layoutManager = LinearLayoutManager(this)
        rvAchievements.adapter = achievementAdapter

        Log.d(TAG, "✅ RecyclerView настроен")
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_achievements

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_timer -> {
                    startActivity(Intent(this, TimerActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_achievements -> true
                R.id.nav_profile -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserAchievements() {
        if (currentUserId.isEmpty()) {
            showDefaultAchievements()
            return
        }

        Log.d(TAG, "🔄 Загружаем достижения для пользователя: $currentUserId")

        db.collection("achievements")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { achievementsSnapshot ->
                Log.d(TAG, "✅ Найдено документов достижений: ${achievementsSnapshot.documents.size}")

                val earnedAchievementIds = mutableSetOf<String>()
                achievementsSnapshot.documents.forEach { document ->
                    val achievementId = document.getString("achievementId")
                    if (achievementId != null) {
                        earnedAchievementIds.add(achievementId)
                        Log.d(TAG, "📄 Найдено достижение: $achievementId")
                    } else {
                        Log.w(TAG, "⚠️ Документ без achievementId: ${document.id}")
                    }
                }

                Log.d(TAG, "✅ Уникальных полученных достижений: ${earnedAchievementIds.size}")
                Log.d(TAG, "📋 Список полученных: $earnedAchievementIds")

                // Создаем полный список достижений с информацией о получении
                val allAchievementsWithStatus = ALL_ACHIEVEMENTS.map { baseAchievement ->
                    val isEarned = earnedAchievementIds.contains(baseAchievement.achievementId)
                    baseAchievement.copy(
                        userId = if (isEarned) currentUserId else "",
                        dateEarned = if (isEarned) Date() else Date(0),
                        isEarned = isEarned
                    )
                }

                // Сортируем: сначала полученные, потом заблокированные
                val sortedAchievements = allAchievementsWithStatus.sortedByDescending { it.isEarned }

                // Логируем итоговый список
                sortedAchievements.forEachIndexed { index, achievement ->
                    Log.d(TAG, "📊 [$index] ${achievement.title} - ${if (achievement.isEarned) "✅" else "🔒"}")
                }

                runOnUiThread {
                    achievementAdapter.updateAchievements(sortedAchievements)

                    Log.d(TAG, "🎯 Передали в адаптер ${sortedAchievements.size} достижений")
                    Log.d(TAG, "📊 Полученных: ${sortedAchievements.count { it.isEarned }}")
                    Log.d(TAG, "🔒 Заблокированных: ${sortedAchievements.count { !it.isEarned }}")

                    // Показываем тост с информацией
                    val earnedCount = sortedAchievements.count { it.isEarned }
                    val totalCount = sortedAchievements.size
                    Toast.makeText(
                        this,
                        "Достижения: $earnedCount/$totalCount получено",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Ошибка загрузки достижений: ${exception.message}")
                showDefaultAchievements()
                Toast.makeText(this, "Ошибка загрузки достижений", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDefaultAchievements() {
        Log.d(TAG, "📋 Показываем достижения по умолчанию")

        val defaultAchievements = ALL_ACHIEVEMENTS.map { it.copy(isEarned = false) }

        runOnUiThread {
            achievementAdapter.updateAchievements(defaultAchievements)
            Log.d(TAG, "✅ Загружены достижения по умолчанию: ${defaultAchievements.size}")
        }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
    }

    // Метод для обновления достижений из других активностей
    fun refreshAchievements() {
        loadUserAchievements()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 AchievementActivity onResume - обновляем данные")
        // При возвращении на экран обновляем достижения
        loadUserAchievements()
    }
}