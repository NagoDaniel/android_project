package com.example.progfront.network

data class WeekdayScheduleRequest(
    val habitId: Int,
    val start_time: String,
    val daysOfWeek: List<Int>, // 1=Monday ... 7=Sunday
    val numberOfWeeks: Int = 4,
    val duration_minutes: Int? = null,
    val end_time: String? = null,
    val participantIds: List<Int>? = null,
    val notes: String? = null
)
