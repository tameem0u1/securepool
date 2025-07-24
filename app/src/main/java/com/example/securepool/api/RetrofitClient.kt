package com.example.securepool.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://10.0.2.2:443/"

    fun create(context: Context): SecurePoolService {
        val tokenManager = TokenManager(context)

        // Create an OkHttpClient and add BOTH the interceptor and the authenticator
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager)) // Adds token to every request
            .authenticator(TokenAuthenticator(tokenManager, context)) // Handles 401 errors
            .build()

        val apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return apiService.create(SecurePoolService::class.java)
    }
}