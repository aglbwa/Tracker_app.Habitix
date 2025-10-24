package com.example.tracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StatsActivity : AppCompatActivity() {

    private lateinit var chartExperience: com.github.mikephil.charting.charts.LineChart
    private lateinit var chartCrystals: com.github.mikephil.charting.charts.LineChart
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private lateinit var tvTotalHabits: TextView
    private lateinit var tvTotalCompletions: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvTopHabits: TextView

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    private var currentPeriod = "week" // week or month

    companion object {
        private const val TAG = "StatsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        supportActionBar?.hide()

        initViews()
        setupBottomNavigation()
        setupPeriodButtons()
        loadUserStats()
        loadChartsData()
    }

    private fun initViews() {
        chartExperience = findViewById(R.id.chartExperience)
        chartCrystals = findViewById(R.id.chartCrystals)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnWeek = findViewById(R.id.btnWeek)
        btnMonth = findViewById(R.id.btnMonth)
        tvTotalHabits = findViewById(R.id.tvTotalHabits)
        tvTotalCompletions = findViewById(R.id.tvTotalCompletions)
        tvTotalPoints = findViewById(R.id.tvTotalPoints)
        tvTopHabits = findViewById(R.id.tvTopHabits)
    }

    private fun setupPeriodButtons() {
        btnWeek.isSelected = true
        btnMonth.isSelected = false

        btnWeek.setOnClickListener {
            currentPeriod = "week"
            btnWeek.isSelected = true
            btnMonth.isSelected = false
            loadChartsData()
        }

        btnMonth.setOnClickListener {
            currentPeriod = "month"
            btnWeek.isSelected = false
            btnMonth.isSelected = true
            loadChartsData()
        }
    }

    private fun loadUserStats() {
        if (currentUserId.isEmpty()) return

        // Загружаем общую статистику
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalPoints = document.getLong("totalPoints")?.toInt() ?: 0
                    val totalCompletions = document.getLong("totalCompletions")?.toInt() ?: 0

                    tvTotalPoints.text = totalPoints.toString()
                    tvTotalCompletions.text = totalCompletions.toString()
                }
            }

        // Загружаем количество привычек
        db.collection("habits")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                tvTotalHabits.text = snapshot.size().toString()
            }

        // Загружаем топ привычек
        loadTopHabits()
    }

    private fun loadTopHabits() {
        // Здесь можно добавить логику для загрузки самых выполняемых привычек
        tvTopHabits.text = "1. Утренняя зарядка - 15 раз\n2. Чтение книги - 12 раз\n3. Медитация - 10 раз"
    }

    private fun loadChartsData() {
        val (xpEntries, crystalEntries) = when (currentPeriod) {
            "week" -> getWeeklyStats()
            "month" -> getMonthlyStats()
            else -> getWeeklyStats()
        }

        setupChart(chartExperience, xpEntries, "Опыт", Color.parseColor("#4CAF50"))
        setupChart(chartCrystals, crystalEntries, "Монеты", Color.parseColor("#FFC107"))
    }

    private fun getWeeklyStats(): Pair<List<Entry>, List<Entry>> {
        // Тестовые данные для недели
        val xpEntries = listOf(
            Entry(0f, 25f),
            Entry(1f, 12f),
            Entry(2f, 18f),
            Entry(3f, 30f),
            Entry(4f, 15f),
            Entry(5f, 22f),
            Entry(6f, 28f)
        )

        val crystalEntries = listOf(
            Entry(0f, 5f),
            Entry(1f, 3f),
            Entry(2f, 7f),
            Entry(3f, 10f),
            Entry(4f, 4f),
            Entry(5f, 8f),
            Entry(6f, 12f)
        )

        return Pair(xpEntries, crystalEntries)
    }

    private fun getMonthlyStats(): Pair<List<Entry>, List<Entry>> {
        // Тестовые данные для месяца
        val xpEntries = listOf(
            Entry(0f, 10f),
            Entry(1f, 25f),
            Entry(2f, 15f),
            Entry(3f, 30f)
        )

        val crystalEntries = listOf(
            Entry(0f, 2f),
            Entry(1f, 8f),
            Entry(2f, 5f),
            Entry(3f, 12f)
        )

        return Pair(xpEntries, crystalEntries)
    }

    private fun setupChart(chart: com.github.mikephil.charting.charts.LineChart, entries: List<Entry>, label: String, color: Int) {
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.WHITE)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
            setDrawValues(true)
        }

        chart.data = LineData(dataSet)

        // Общая настройка графика
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            setNoDataTextColor(Color.WHITE)
        }

        // Настройка оси X
        val xAxis = chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.WHITE
            textSize = 12f
            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineColor = Color.WHITE

            val labels = when (currentPeriod) {
                "week" -> arrayOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
                "month" -> arrayOf("Нед1", "Нед2", "Нед3", "Нед4")
                else -> arrayOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
            }
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            labelCount = labels.size
            isGranularityEnabled = true
        }

        // Настройка оси Y (левая)
        chart.axisLeft.apply {
            textColor = Color.WHITE
            setDrawGridLines(true)
            gridColor = Color.parseColor("#4C4C5D")
            setDrawAxisLine(false)
            axisMinimum = 0f
        }

        // Настройка оси Y (правая)
        chart.axisRight.isEnabled = false

        chart.invalidate()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_stats

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
                R.id.nav_stats -> true
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

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
    }
}