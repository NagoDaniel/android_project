package com.example.progfront.data.model

data class RegisterResponse(
    val message: String,
    val user: User,
    val tokens: Tokens
)

