package com.example.securepool.model

data class ScoreResponse(
    val username: String,
    val score: Int,
    val lastZeroTimestamp: String?
)