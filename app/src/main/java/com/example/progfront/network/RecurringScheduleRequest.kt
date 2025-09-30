package com.example.progfront.network

data class RecurringScheduleRequest(
    val habitId: Int,
    val start_time: String,
    val repeatPattern: String = "none", // "none", "daily", "weekdays", "weekends"
    val is_custom: Boolean = true,
    val end_time: String? = null,
    val duration_minutes: Int? = null,
    val repeatDays: Int = 30,
    val participantIds: List<Int>? = null,
    val notes: String? = null
)
