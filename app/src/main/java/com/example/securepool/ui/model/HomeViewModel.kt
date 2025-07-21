package com.example.securepool.ui.model

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.LeaderboardEntry
import com.example.securepool.api.TokenManager
import com.example.securepool.model.RegisterRequest
import com.example.securepool.ui.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val username: String = "",
    val score: Int = 0,
    val isLoading: Boolean = true,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val opponent: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val apiService = RetrofitClient.create(application)
    private val tokenManager = TokenManager(application)

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoading = true) }

        val username = tokenManager.getUsername()

        // If there's no username, redirect to login screen
        if (username.isNullOrEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            viewModelScope.launch {
                _navigationEvent.emit(NavigationEvent.NavigateToLogin)
            }
            return
        }

        viewModelScope.launch {
            try {
                // Use the retrieved username for the API calls
                val scoreResponse = apiService.getScore(username)
                val leaderboardResponse = apiService.getLeaderboard()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        username = username,
                        score = scoreResponse.body()?.score ?: 0,
                        leaderboard = leaderboardResponse.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                Toast.makeText(getApplication(), "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun restoreScore() {
        viewModelScope.launch {
            try {
                val username = _uiState.value.username
                if (username.isEmpty()) return@launch // Don't run if username is not loaded

                val request = RegisterRequest(username, "placeholder")

                val response = apiService.restoreScore(request)

                if (response.isSuccessful) {
                    Toast.makeText(getApplication(), "Points restored!", Toast.LENGTH_SHORT).show()
                    loadData() // Reload data to get the new score
                } else {
                    Toast.makeText(getApplication(), "Restore request failed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // TODO: handle opponent matching server side via websocket
    fun findOpponent(): String? {
        val currentUser = _uiState.value.username
        val opponent = _uiState.value.leaderboard
            .filter { it.username != currentUser && it.score > 0 }
            .randomOrNull()?.username

        return opponent
    }
}