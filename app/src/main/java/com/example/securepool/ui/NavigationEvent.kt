package com.example.securepool.ui

sealed interface NavigationEvent {
    data object NavigateToLogin : NavigationEvent
    data object NavigateToHome : NavigationEvent
}