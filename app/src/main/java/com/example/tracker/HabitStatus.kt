package com.example.tracker

import java.util.Date

data class HabitStatus(
    val statusId: String = "",
    val habitId: String = "",
    val date: Date = Date(),
    val isCompleted: Boolean = false
)