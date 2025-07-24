package com.example.securepool.model

data class LoginResponse (
    val success: Boolean,
    val username: String,
    val score: Int,
    val lastZeroTimestamp: String?,
    val accessToken: String,
    val refreshToken: String
)