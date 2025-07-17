package com.example.securepool.model

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val userId: Int
)