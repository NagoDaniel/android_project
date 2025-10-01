package com.example.progfront.data.model

data class HabitRequest(
    val name: String,
    val categoryId: Int,
    val goal: String,
    val description: String? = null
)

