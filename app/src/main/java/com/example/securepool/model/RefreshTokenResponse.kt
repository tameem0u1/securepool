package com.example.securepool.model

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)
