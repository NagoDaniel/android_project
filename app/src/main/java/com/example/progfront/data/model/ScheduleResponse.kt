package com.example.progfront.data.model

data class ScheduleResponse(
    val id: Int,
    val habitId: Int,
    val date: String,
    val start_time: String,
    val end_time: String?,
    val duration_minutes: Int?,
    val status: String, // "Planned", "Completed", "Skipped"
    val is_custom: Boolean,
    val notes: String?,
    val participantIds: List<Int>?,
    val habit: HabitResponse?,
    val createdAt: String,
    val updatedAt: String
)

