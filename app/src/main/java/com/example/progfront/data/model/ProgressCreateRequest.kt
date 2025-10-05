package com.example.progfront.data.model

data class ProgressCreateRequest(
    val scheduleId: Int,
    val date: String,
    val logged_time: Int? = null,
    val notes: String? = null,
    val is_completed: Boolean? = null
)
