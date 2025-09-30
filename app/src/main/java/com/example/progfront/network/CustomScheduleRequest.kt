package com.example.progfront.network

data class CustomScheduleRequest(
    val habitId: Int,
    val date: String,
    val start_time: String,
    val is_custom: Boolean = true,
    val end_time: String? = null,
    val duration_minutes: Int? = null,
    val participantIds: List<Int>? = null,
    val notes: String? = null
)
