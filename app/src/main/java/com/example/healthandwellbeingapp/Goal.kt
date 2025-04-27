package com.example.healthandwellbeingapp.goals

data class Goal(
    val name: String = "",
    val startValue: Float = 0f,
    val targetValue: Float = 0f,
    var currentValue: Float = 0f,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val dailyImprovement: Float = 0f,
    var id: String = ""
)
