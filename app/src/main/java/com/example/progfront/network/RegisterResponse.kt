package com.example.progfront.network

data class RegisterResponse(
    val message: String,
    val user: User,
    val tokens: Tokens
)

