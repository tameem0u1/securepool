package com.example.securepool.ui.model

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val leaderboard: List<LeaderboardEntry> = emptyList()
)

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState = _uiState.asStateFlow()

    private val apiService = RetrofitClient.create(application)

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = apiService.getLeaderboard()
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            leaderboard = response.body() ?: emptyList()
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    Toast.makeText(getApplication(), "Failed to load leaderboard", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                Toast.makeText(getApplication(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}