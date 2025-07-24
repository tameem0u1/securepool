package com.example.securepool.model

data class MatchResultRequest(
    val winner: String,
    val loser: String,
    val outcome: String // "win", "lose", or "exit"
)