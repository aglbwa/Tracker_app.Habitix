package com.example.tracker

import java.util.Date

data class Achievement(
    val achievementId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val pointsRequired: Int = 0,
    val icon: String = "",
    val dateEarned: Date = Date(),
    val isEarned: Boolean = false,
    val type: String = ""
)