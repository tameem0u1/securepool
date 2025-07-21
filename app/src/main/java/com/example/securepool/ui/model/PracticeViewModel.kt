package com.example.securepool.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.securepool.api.RetrofitClient
import com.example.securepool.api.TokenManager
import com.example.securepool.ui.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeUiState(
    val isLoading: Boolean = true,
    val username: String = "",
    val score: Int = 0
)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val apiService = RetrofitClient.create(application)
    private val tokenManager = TokenManager(application)

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val username = tokenManager.getUsername()
        if (username.isNullOrEmpty()) {
            viewModelScope.launch {
                _navigationEvent.emit(NavigationEvent.NavigateToLogin)
            }
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.getScore(username)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        username = username,
                        score = response.body()?.score ?: 0
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}