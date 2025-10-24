package com.example.tracker

import com.google.firebase.firestore.Exclude
import java.util.Date

// data class для хранения постоянных метаданных привычки
data class Habit(
    val habitId: String,
    val userId: String,
    val title: String,
    val description: String,
    val frequency: String = "Ежедневно",
    val points: Int,
    val createdAt: Date = Date(),

    // Поле, не хранящееся в Firestore, используется только локально
    // для передачи текущего статуса в адаптер.
    @get:Exclude
    @set:Exclude
    var isCompleted: Boolean = false
)