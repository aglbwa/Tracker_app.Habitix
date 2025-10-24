package com.example.tracker

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DataRepository(private val db: FirebaseFirestore) {

    private val TAG = "DataRepository"

    companion object {
        fun getWeeklyStats(): Pair<List<com.github.mikephil.charting.data.Entry>, List<com.github.mikephil.charting.data.Entry>> {
            // Заглушка для тестовых данных
            val xpEntries = listOf(
                com.github.mikephil.charting.data.Entry(0f, 25f),
                com.github.mikephil.charting.data.Entry(1f, 12f),
                com.github.mikephil.charting.data.Entry(2f, 18f),
                com.github.mikephil.charting.data.Entry(3f, 30f),
                com.github.mikephil.charting.data.Entry(4f, 15f),
                com.github.mikephil.charting.data.Entry(5f, 22f),
                com.github.mikephil.charting.data.Entry(6f, 28f)
            )

            val crystalEntries = listOf(
                com.github.mikephil.charting.data.Entry(0f, 5f),
                com.github.mikephil.charting.data.Entry(1f, 3f),
                com.github.mikephil.charting.data.Entry(2f, 7f),
                com.github.mikephil.charting.data.Entry(3f, 10f),
                com.github.mikephil.charting.data.Entry(4f, 4f),
                com.github.mikephil.charting.data.Entry(5f, 8f),
                com.github.mikephil.charting.data.Entry(6f, 12f)
            )

            return Pair(xpEntries, crystalEntries)
        }

        fun recordReward(points: Int, crystals: Int) {
            // Заглушка для записи наград
            Log.d("DataRepository", "Записана награда: $points очков, $crystals кристаллов")
        }
    }

    fun addHabitsListener(
        userId: String,
        onHabitsLoaded: (List<Habit>) -> Unit
    ): ListenerRegistration {
        return db.collection("habits")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Ошибка при подписке на привычки", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val habits = snapshot.documents.mapNotNull { document ->
                        document.toObject(Habit::class.java)
                    }
                    onHabitsLoaded(habits)
                }
            }
    }

    fun getHabitStatus(habitId: String, date: Date, onStatusLoaded: (Boolean) -> Unit) {
        val today = getNormalizedDate(date)

        db.collection("habitStatus")
            .whereEqualTo("habitId", habitId)
            .whereEqualTo("date", today)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val isCompleted = querySnapshot.documents.any {
                    it.toObject(HabitStatus::class.java)?.isCompleted ?: false
                }
                onStatusLoaded(isCompleted)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка загрузки статуса для $habitId", e)
                onStatusLoaded(false)
            }
    }

    fun setHabitCompleted(habit: Habit, isCompleted: Boolean, date: Date) {
        val today = getNormalizedDate(date)
        val statusRef = db.collection("habitStatus")
            .document("${habit.habitId}_${today.time}")

        val newStatus = HabitStatus(
            statusId = statusRef.id,
            habitId = habit.habitId,
            date = today,
            isCompleted = isCompleted
        )

        statusRef.set(newStatus)
            .addOnSuccessListener {
                Log.d(TAG, "Статус для ${habit.title} обновлен до $isCompleted")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка обновления статуса", e)
            }
    }

    private fun getNormalizedDate(date: Date): Date {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        val dateString = format.format(date)
        return format.parse(dateString) ?: date
    }
}