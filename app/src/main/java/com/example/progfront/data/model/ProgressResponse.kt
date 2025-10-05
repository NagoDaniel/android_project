package com.example.progfront.data.model

data class ProgressResponse(
    val id: Int? = null,
    val scheduleId: Int? = null,
    val date: String,
    val logged_time: Int? = null,
    val notes: String? = null,
    val is_completed: Boolean = false
)
