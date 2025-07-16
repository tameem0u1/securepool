package com.example.securepool.api

import com.example.securepool.model.*
import retrofit2.Response
import retrofit2.http.*

interface SecurePoolService {

    @POST("/api/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("/api/score")
    suspend fun getScore(@Query("username") username: String): Response<ScoreResponse>

    @GET("/api/leaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>

    @POST("/api/matchResult")
    suspend fun sendMatchResult(@Body result: MatchResultRequest): Response<Unit>
}