package com.example.progfront.data.model

data class HabitResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val goal: String,
    val categoryId: Int,
    val createdAt: String,
    val updatedAt: String
)

