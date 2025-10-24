package com.example.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // Элементы UI
    private lateinit var rvHabits: RecyclerView
    private lateinit var btnAddHabit: Button
    private lateinit var btnLogout: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var tvLevel: TextView
    private lateinit var tvLives: TextView
    private lateinit var tvCoins: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBarLevel: ProgressBar
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var tvUserName: TextView
    private lateinit var tvEmptyState: TextView

    // Элементы календаря
    private lateinit var tvCalendarTitle: TextView
    private lateinit var btnPrevWeek: Button
    private lateinit var btnNextWeek: Button
    private lateinit var daysContainer: LinearLayout

    // Данные и адаптер
    private val habitList = mutableListOf<Habit>()
    private lateinit var habitAdapter: HabitAdapter

    // Firebase
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    // Менеджер достижений
    private val achievementManager = AchievementManager()

    // Календарь переменные
    private var currentWeekStart: Calendar = Calendar.getInstance()
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("ru"))
    private val dayFormat = SimpleDateFormat("d", Locale("ru"))
    private val dayNameFormat = SimpleDateFormat("E", Locale("ru"))
    private val dateFormatForFirestore = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val dayViews = mutableListOf<LinearLayout>()
    private val dayCalendars = mutableListOf<Calendar>()

    // ActivityResultLauncher для запроса разрешения на уведомления
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // Статистика пользователя
    private var userStats = UserStats()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Проверка авторизации
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        supportActionBar?.hide()

        registerPermissionLauncher()

        initViews()
        setupRecyclerView()
        setupCalendar()
        setupClickListeners()
        setupBottomNavigation()

        showUserInfo()
        loadUserStats()
        loadHabitsForSelectedDate()

        requestNotificationPermission()
        scheduleDeadlineReminder()
    }

    private fun registerPermissionLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Разрешение на уведомления получено.")
            } else {
                Toast.makeText(this, "Уведомления отключены. Вы не получите напоминания.", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Разрешение на уведомления отклонено.")
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initViews() {
        rvHabits = findViewById(R.id.rvHabits)
        btnAddHabit = findViewById(R.id.btnAddHabit)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        tvLevel = findViewById(R.id.tvLevel)
        tvLives = findViewById(R.id.tvLives)
        tvCoins = findViewById(R.id.tvCoins)
        tvProgress = findViewById(R.id.tvProgress)
        progressBarLevel = findViewById(R.id.progressBarLevel)
        progressBarLoading = findViewById(R.id.progressBarLoading)
        tvUserName = findViewById(R.id.tvUserName)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        tvCalendarTitle = findViewById(R.id.tvCalendarTitle)
        btnPrevWeek = findViewById(R.id.btnPrevWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)
        daysContainer = findViewById(R.id.daysContainer)
    }

    private fun setupCalendar() {
        currentWeekStart.firstDayOfWeek = Calendar.MONDAY
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        selectedDate = Calendar.getInstance()

        updateCalendar()

        btnPrevWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            updateCalendar()
            loadHabitsForSelectedDate()
        }

        btnNextWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            updateCalendar()
            loadHabitsForSelectedDate()
        }
    }

    private fun updateCalendar() {
        tvCalendarTitle.text = dateFormat.format(currentWeekStart.time)
        daysContainer.removeAllViews()
        dayViews.clear()
        dayCalendars.clear()

        val tempCalendar = currentWeekStart.clone() as Calendar
        for (i in 0 until 7) {
            val dayCalendar = tempCalendar.clone() as Calendar
            dayCalendars.add(dayCalendar)
            val dayView = createDayView(dayCalendar, i)
            daysContainer.addView(dayView)
            dayViews.add(dayView)
            tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        updateCalendarStyles()
    }

    private fun createDayView(calendar: Calendar, index: Int): LinearLayout {
        val dayLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 2.dpToPx()
                marginEnd = 2.dpToPx()
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4.dpToPx(), 8.dpToPx(), 4.dpToPx(), 8.dpToPx())
        }

        val dayNameView = TextView(this).apply {
            text = dayNameFormat.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
            textSize = 10f
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
            gravity = Gravity.CENTER
        }

        val dayNumberView = TextView(this).apply {
            text = dayFormat.format(calendar.time)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
        }

        dayLayout.setOnClickListener {
            selectedDate = calendar.clone() as Calendar
            updateCalendarStyles()
            loadHabitsForSelectedDate()
        }

        dayLayout.addView(dayNameView)
        dayLayout.addView(dayNumberView)
        return dayLayout
    }

    private fun updateCalendarStyles() {
        val today = Calendar.getInstance()
        dayViews.forEachIndexed { index, dayLayout ->
            val calendar = dayCalendars[index]
            val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            val isSelected = calendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
            val dayNumberView = dayLayout.getChildAt(1) as TextView
            updateDayViewStyle(dayLayout, dayNumberView, isToday, isSelected)
        }
    }

    private fun updateDayViewStyle(dayLayout: LinearLayout, dayNumberView: TextView, isToday: Boolean, isSelected: Boolean) {
        when {
            isToday && isSelected -> {
                dayLayout.setBackgroundResource(R.drawable.calendar_day_selected_today)
                dayNumberView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                dayNumberView.setTypeface(null, Typeface.BOLD)
            }
            isToday -> {
                dayLayout.setBackgroundResource(R.drawable.calendar_day_today)
                dayNumberView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                dayNumberView.setTypeface(null, Typeface.BOLD)
            }
            isSelected -> {
                dayLayout.setBackgroundResource(R.drawable.calendar_day_selected)
                dayNumberView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                dayNumberView.setTypeface(null, Typeface.BOLD)
            }
            else -> {
                dayNumberView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                dayLayout.setBackgroundResource(R.drawable.calendar_day_normal)
                dayNumberView.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun loadHabitsForSelectedDate() {
        val selectedDateString = dateFormatForFirestore.format(selectedDate.time)
        Log.d(TAG, "🔄 Загрузка привычек для даты: $selectedDateString")

        progressBarLoading.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        rvHabits.visibility = View.GONE

        loadHabitsFromFirebase()
    }

    private fun loadHabitsFromFirebase() {
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "❌ currentUserId пустой!")
            return
        }

        val selectedDateString = dateFormatForFirestore.format(selectedDate.time)
        Log.d(TAG, "🔄 Загружаем привычки для пользователя: $currentUserId")

        progressBarLoading.visibility = View.VISIBLE

        db.collection("habits")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { habitsSnapshot ->
                Log.d(TAG, "✅ Получено привычек из Firestore: ${habitsSnapshot.size()}")

                val allHabits = habitsSnapshot.mapNotNull { document ->
                    try {
                        Log.d(TAG, "📄 Документ привычки: ${document.data}")
                        Habit(
                            habitId = document.id,
                            userId = document.getString("userId") ?: currentUserId,
                            title = document.getString("title") ?: "Без названия",
                            description = document.getString("description") ?: "",
                            frequency = document.getString("frequency") ?: "Ежедневно",
                            points = document.getLong("points")?.toInt() ?: 10,
                            createdAt = document.getDate("createdAt") ?: Date()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Ошибка парсинга привычки: ${e.message}")
                        null
                    }
                }

                Log.d(TAG, "📊 Спарсено привычек: ${allHabits.size}")

                if (allHabits.isEmpty()) {
                    Log.d(TAG, "📭 Список привычек пуст")
                    runOnUiThread {
                        progressBarLoading.visibility = View.GONE
                        updateUIAfterLoading(0)
                    }
                    return@addOnSuccessListener
                }

                // Получаем выполненные привычки за выбранную дату
                db.collection("completions")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("date", selectedDateString)
                    .get()
                    .addOnSuccessListener { completionsSnapshot ->
                        val completedHabitIds = completionsSnapshot.map { it.getString("habitId") ?: "" }.toSet()
                        Log.d(TAG, "✅ Выполненные привычки: $completedHabitIds")

                        val habitsForDisplay = allHabits.map { habit ->
                            habit.copy(
                                isCompleted = completedHabitIds.contains(habit.habitId)
                            )
                        }

                        runOnUiThread {
                            progressBarLoading.visibility = View.GONE
                            updateHabitsList(habitsForDisplay)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Ошибка загрузки статуса выполнения: ${e.message}")
                        runOnUiThread {
                            progressBarLoading.visibility = View.GONE
                            updateHabitsList(allHabits)
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Ошибка Firestore: ${exception.message}")
                runOnUiThread {
                    progressBarLoading.visibility = View.GONE
                    Toast.makeText(this, "Ошибка загрузки привычек", Toast.LENGTH_SHORT).show()
                    updateUIAfterLoading(0)
                }
            }
    }

    private fun updateHabitsList(newHabits: List<Habit>) {
        Log.d(TAG, "🎯 Обновляем список привычек: ${newHabits.size} привычек")

        habitList.clear()
        habitList.addAll(newHabits)
        habitAdapter.updateHabits(habitList)
        updateUIAfterLoading(newHabits.size)
    }

    private fun updateUIAfterLoading(habitCount: Int) {
        Log.d(TAG, "🔄 Обновление UI после загрузки: $habitCount привычек")

        if (habitCount > 0) {
            tvEmptyState.visibility = View.GONE
            rvHabits.visibility = View.VISIBLE
            Log.d(TAG, "✅ Показываем RecyclerView, скрываем пустое состояние")
        } else {
            tvEmptyState.visibility = View.VISIBLE
            rvHabits.visibility = View.GONE
            Log.d(TAG, "✅ Показываем пустое состояние, скрываем RecyclerView")
        }

        updateProgress()
    }

    private fun showAddHabitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_habit_simple, null)
        val etHabitName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val etHabitDescription = dialogView.findViewById<EditText>(R.id.etHabitDescription)
        val etHabitPoints = dialogView.findViewById<EditText>(R.id.etHabitPoints)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Добавить привычку")
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnAdd.setOnClickListener {
            val name = etHabitName.text.toString().trim()
            val description = etHabitDescription.text.toString().trim()
            val pointsText = etHabitPoints.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val points = pointsText.toIntOrNull() ?: 10
            val newHabit = Habit(
                habitId = db.collection("habits").document().id,
                userId = currentUserId,
                title = name,
                description = description,
                points = points
            )

            saveHabitToFirestore(newHabit)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun saveHabitToFirestore(habit: Habit) {
        val habitData = hashMapOf(
            "habitId" to habit.habitId,
            "userId" to habit.userId,
            "title" to habit.title,
            "description" to habit.description,
            "frequency" to "Ежедневно",
            "points" to habit.points,
            "createdAt" to Timestamp.now()
        )

        db.collection("habits").document(habit.habitId)
            .set(habitData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Привычка сохранена: ${habit.title}")

                // Проверяем достижения по созданию привычек
                checkHabitCreationAchievements()

                loadHabitsForSelectedDate()
                Toast.makeText(this, "Привычка '${habit.title}' добавлена! 🎉", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка сохранения: ${e.message}")
                Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "🔧 Настройка RecyclerView")

        // Используем упрощенный адаптер без передачи списка в конструктор
        habitAdapter = HabitAdapter { habit, isChecked ->
            onHabitChecked(habit, isChecked)
        }

        val layoutManager = LinearLayoutManager(this)
        rvHabits.layoutManager = layoutManager
        rvHabits.adapter = habitAdapter

        Log.d(TAG, "✅ RecyclerView настроен")
    }

    private fun onHabitChecked(habit: Habit, isChecked: Boolean) {
        val index = habitList.indexOfFirst { it.habitId == habit.habitId }
        if (index != -1) {
            habitList[index].isCompleted = isChecked
            habitAdapter.notifyItemChanged(index)
        }

        updateHabitCompletion(habit, isChecked)

        if (isChecked) {
            updateCoins(habit.points)
            // Проверяем достижения по выполнению привычек
            checkHabitCompletionAchievements()

            // Проверяем специальные достижения (время выполнения)
            checkTimeBasedAchievements()
        }
        updateProgress()
    }

    private fun updateHabitCompletion(habit: Habit, isCompleted: Boolean) {
        if (currentUserId.isEmpty()) return

        val selectedDateString = dateFormatForFirestore.format(selectedDate.time)
        val completionDocId = "${habit.habitId}_$selectedDateString"
        val completionRef = db.collection("completions").document(completionDocId)

        if (isCompleted) {
            val completionData = hashMapOf(
                "habitId" to habit.habitId,
                "userId" to currentUserId,
                "date" to selectedDateString,
                "timestamp" to Timestamp.now()
            )
            completionRef.set(completionData)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Выполнение записано: $completionDocId")
                    // Обновляем статистику пользователя
                    updateUserStatsAfterCompletion()
                }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Ошибка записи выполнения: ${e.message}") }
        } else {
            completionRef.delete()
                .addOnSuccessListener { Log.d(TAG, "✅ Выполнение удалено: $completionDocId") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Ошибка удаления выполнения: ${e.message}") }
        }
    }

    private fun updateProgress() {
        val completedCount = habitList.count { it.isCompleted }
        val totalCount = habitList.size
        val progress = if (totalCount > 0) (completedCount * 100 / totalCount) else 0

        tvProgress.text = "$progress%"
        progressBarLevel.progress = progress

        val level = (completedCount / 5) + 1
        tvLevel.text = level.toString()
    }

    private fun updateCoins(amount: Int) {
        val currentCoins = tvCoins.text.toString().toIntOrNull() ?: 0
        val newCoins = currentCoins + amount
        tvCoins.text = newCoins.toString()

        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId)
                .update("totalPoints", newCoins)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Монеты обновлены: $newCoins")
                    // Проверяем достижения по очкам
                    achievementManager.checkPointsAchievements(newCoins)
                }
        }
    }

    private fun loadUserStats() {
        if (currentUserId.isEmpty()) {
            setDefaultStats()
            return
        }

        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalPoints = document.getLong("totalPoints")?.toInt() ?: 0
                    val currentStreak = document.getLong("currentStreak")?.toInt() ?: 0
                    val totalCompletions = document.getLong("totalCompletions")?.toInt() ?: 0

                    tvCoins.text = totalPoints.toString()

                    // Сохраняем статистику для достижений
                    userStats = UserStats(
                        totalPoints = totalPoints,
                        currentStreak = currentStreak,
                        totalCompletions = totalCompletions
                    )

                    // Проверяем достижения по очкам и стрикам - ✅ Теперь работает
                    achievementManager.checkPointsAchievements(totalPoints)
                    achievementManager.checkStreakAchievements(currentStreak)
                } else {
                    setDefaultStats()
                }
            }
            .addOnFailureListener {
                setDefaultStats()
            }
    }

    private fun updateUserStatsAfterCompletion() {
        // Увеличиваем счетчик выполненных привычек
        userStats = userStats.copy(
            totalCompletions = userStats.totalCompletions + 1
        )

        // Обновляем в Firestore
        db.collection("users").document(currentUserId)
            .update(
                "totalCompletions", userStats.totalCompletions,
                "lastActivity", Timestamp.now()
            )
            .addOnSuccessListener {
                Log.d(TAG, "✅ Статистика обновлена: ${userStats.totalCompletions} выполнений")
            }
    }

    private fun setDefaultStats() {
        tvLevel.text = "1"
        tvLives.text = "3"
        tvCoins.text = "150"
    }

    // Методы для работы с достижениями
    private fun checkHabitCreationAchievements() {
        db.collection("habits")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val totalHabits = snapshot.size()
                achievementManager.checkHabitCreationAchievements(totalHabits)
            }
    }

    private fun checkHabitCompletionAchievements() {
        achievementManager.checkHabitCompletionAchievements(
            userStats.totalCompletions + 1, // +1 для текущего выполнения
            getCurrentPoints()
        )
    }

    private fun checkTimeBasedAchievements() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isEarlyMorning = currentHour in 5..7 // 5-7 утра
        val isLateNight = currentHour in 22..23 || currentHour == 0 // 22-24 часа

        achievementManager.checkSpecialAchievements(isEarlyMorning, isLateNight)
    }

    private fun getCurrentPoints(): Int {
        return tvCoins.text.toString().toIntOrNull() ?: 0
    }

    private fun showUserInfo() {
        auth.currentUser?.let { user ->
            val displayName = if (user.isAnonymous) "Гость" else user.email?.substringBefore("@") ?: "Пользователь"
            tvUserName.text = "Привет, $displayName!"
        }
    }

    private fun setupClickListeners() {
        btnAddHabit.setOnClickListener { showAddHabitDialog() }
        btnLogout.setOnClickListener { logoutUser() }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_main

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> true
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
                R.id.nav_achievements -> {
                    startActivity(Intent(this, AchievementActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
    }

    private fun scheduleDeadlineReminder() {
        Log.d(TAG, "Планирование ежедневного напоминания...")

        val deadlineRequest = PeriodicWorkRequestBuilder<DeadlineWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag("DeadlineReminder")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DeadlineReminderId",
            ExistingPeriodicWorkPolicy.REPLACE,
            deadlineRequest
        )
    }
}

// Data class для статистики пользователя
data class UserStats(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val totalCompletions: Int = 0
)