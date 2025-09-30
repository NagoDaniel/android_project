package com.example.progfront.network

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val provider: String,
    val confirmed: Boolean,
    val blocked: Boolean,
    val createdAt: String,
    val updatedAt: String
)

