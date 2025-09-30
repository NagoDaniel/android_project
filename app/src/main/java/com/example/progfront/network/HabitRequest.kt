package com.example.progfront.network

data class HabitRequest(
    val name: String,
    val categoryId: Int,
    val goal: String,
    val description: String? = null
)
