package com.example.securepool.ui.model

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.RegisterRequest
import com.example.securepool.api.TokenManager
import com.example.securepool.ui.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // Use the shared navigation event for consistency
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val apiService = RetrofitClient.create(application)
    private val tokenManager = TokenManager(application)

    // Add functions to update the state from the UI
    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onPasswordVisibilityChange(isVisible: Boolean) {
        _uiState.update { it.copy(isPasswordVisible = isVisible) }
    }

    fun loginUser() {
        val currentState = _uiState.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            Toast.makeText(getApplication(), "Username and password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val request = RegisterRequest(currentState.username, currentState.password)
                val response = apiService.loginUser(request)
                val body = response.body()

                if (response.isSuccessful && body != null) {
                    tokenManager.saveTokens(body.accessToken, body.refreshToken)
                    tokenManager.saveUsername(body.username)
                    // Emit a navigation event instead of updating state
                    _navigationEvent.emit(NavigationEvent.NavigateToHome)
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Login failed"
                    Toast.makeText(getApplication(), errorMessage, Toast.LENGTH_LONG).show()
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}