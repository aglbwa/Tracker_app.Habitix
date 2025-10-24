package com.example.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeadlineWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val dateFormatForFirestore = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currentUserId get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "DeadlineWorker"
        private const val CHANNEL_ID = "deadline_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun doWork(): Result {
        Log.d(TAG, "DeadlineWorker запущен.")
        if (currentUserId.isNullOrEmpty()) {
            Log.w(TAG, "Пользователь не авторизован или ID пуст. Пропускаем проверку.")
            return Result.success()
        }

        // Синхронный вызов, чтобы дождаться результата проверки
        val notCompletedCount = checkHabitsStatus()
        Log.d(TAG, "Невыполненных привычек обнаружено: $notCompletedCount")

        if (notCompletedCount > 0) {
            sendNotification(applicationContext, notCompletedCount)
        }

        return Result.success()
    }

    /**
     * !!! ВАЖНАЯ ЛОГИКА !!!
     * Проверяет, сколько ЕЖЕДНЕВНЫХ привычек не выполнено на ТЕКУЩИЙ день.
     */
    private fun checkHabitsStatus(): Int {
        val todayDateString = dateFormatForFirestore.format(Date())
        var notCompletedCount = 0

        // 1. Получаем все базовые привычки пользователя (должны быть выполнены)
        val habitsTask = db.collection("habits")
            .whereEqualTo("userId", currentUserId)
            .get()

        val habitsSnapshot = try {
            com.google.android.gms.tasks.Tasks.await(habitsTask)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки базовых привычек: ${e.message}")
            return 0
        }

        val allHabitIds = habitsSnapshot.documents.map { it.id }
        if (allHabitIds.isEmpty()) return 0

        // 2. Получаем выполненные привычки за СЕГОДНЯ
        val completionsTask = db.collection("completions")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("date", todayDateString)
            .get()

        val completionsSnapshot = try {
            com.google.android.gms.tasks.Tasks.await(completionsTask)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки выполненных привычек: ${e.message}")
            return allHabitIds.size // Если ошибка, считаем, что ничего не выполнено
        }

        val completedHabitIds = completionsSnapshot.documents.map {
            it.getString("habitId")
        }.toSet()

        // 3. Вычисляем невыполненные
        notCompletedCount = allHabitIds.count { !completedHabitIds.contains(it) }

        return notCompletedCount
    }

    private fun sendNotification(context: Context, count: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Создание канала уведомлений (необходимо для Android 8.0 и выше)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о дедлайнах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминание о невыполненных привычках"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = context.getString(
            R.string.notification_text, // Создайте этот ресурс в strings.xml, например: "Осталось выполнить %d привычек. Завершите день победой!"
            count
        )

        // 2. Создание уведомления
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Иконка, созданная ранее
            .setContentTitle("Дедлайн близко! ⏰")
            .setContentText("Осталось выполнить $count привычек. Завершите день победой!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 3. Отправка уведомления
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}