package com.example.tracker

import android.content.Intent // <-- ИСПРАВЛЕНО
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class TimerActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var etTimeInput: EditText
    private lateinit var btnSet: Button
    private lateinit var btnStartPause: Button
    private lateinit var btnReset: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning: Boolean = false
    private var timeLeftInMillis: Long = 0
    private var initialTimeInMillis: Long = 0

    companion object {
        private const val START_TIME_IN_MILLIS: Long = 600000 // 10 минут по умолчанию
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        supportActionBar?.hide()
        initViews()
        setupListeners()
        setupBottomNavigation()

        timeLeftInMillis = START_TIME_IN_MILLIS
        updateCountDownText()
        updateButtons() // Вызываем, чтобы установить текст и состояние кнопок
    }

    private fun initViews() {
        tvTimer = findViewById(R.id.tvTimer)
        etTimeInput = findViewById(R.id.etTimeInput)
        btnSet = findViewById(R.id.btnSetTime)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnReset = findViewById(R.id.btnReset)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        btnSet.setOnClickListener { setTime() }
        btnStartPause.setOnClickListener { startPause() }
        btnReset.setOnClickListener { resetTimer() }
    }

    // --- Логика Таймера ---

    private fun setTime() {
        val input = etTimeInput.text.toString()
        if (input.isEmpty()) {
            Toast.makeText(this, "Введите время в минутах!", Toast.LENGTH_SHORT).show()
            return
        }

        val minutes = input.toLongOrNull() ?: 0
        if (minutes <= 0) {
            Toast.makeText(this, "Время должно быть больше нуля", Toast.LENGTH_SHORT).show()
            return
        }

        pauseTimer() // Остановим, если запущен
        initialTimeInMillis = minutes * 60000
        timeLeftInMillis = initialTimeInMillis
        updateCountDownText()
        etTimeInput.setText("")
        updateButtons()
    }

    private fun startPause() {
        if (initialTimeInMillis == 0L) { // Проверяем, было ли время установлено
            Toast.makeText(this, "Сначала установите время", Toast.LENGTH_SHORT).show()
            return
        }

        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                isTimerRunning = false
                Toast.makeText(this@TimerActivity, "Время вышло! 🎉", Toast.LENGTH_LONG).show()
                updateButtons()
            }
        }.start()

        isTimerRunning = true
        updateButtons()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        updateButtons()
    }

    private fun resetTimer() {
        pauseTimer()
        timeLeftInMillis = initialTimeInMillis
        if (initialTimeInMillis == 0L) {
            timeLeftInMillis = START_TIME_IN_MILLIS
        }
        updateCountDownText()
        updateButtons()
    }

    private fun updateCountDownText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        tvTimer.text = timeLeftFormatted
    }

    private fun updateButtons() {
        if (isTimerRunning) {
            btnStartPause.text = "Пауза"
            // УДАЛЕНА строка с R.drawable.btn_pause
            btnSet.isEnabled = false
        } else {
            btnStartPause.text = if (timeLeftInMillis > 0 && timeLeftInMillis != initialTimeInMillis) "Продолжить" else "Старт"
            // УДАЛЕНА строка с R.drawable.btn_start
            btnSet.isEnabled = true
        }
        btnReset.isEnabled = (timeLeftInMillis != initialTimeInMillis) && (initialTimeInMillis > 0)
    }

    // --- Навигация ---

    private fun setupBottomNavigation() {
        // Устанавливаем иконку таймера как активную
        bottomNavigation.selectedItemId = R.id.nav_timer

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_timer -> true
                R.id.nav_stats -> { // <-- Добавьте этот переход
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
                    Toast.makeText(this, "Профиль/Выход", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}