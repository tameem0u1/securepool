package com.example.securepool

import SecurePoolHomeScreen
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.securepool.ui.NavigationEvent
import com.example.securepool.ui.model.HomeViewModel
import com.example.securepool.ui.theme.SecurePoolTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecurePoolTheme {
                // Collect the state from the ViewModel
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is NavigationEvent.NavigateToLogin -> {
                                Toast.makeText(this@MainActivity, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                            else -> {} // Ignore other events
                        }
                    }
                }

                SecurePoolHomeScreen(
                    uiState = uiState,
                    onRestoreScore = { viewModel.restoreScore() },
                    onFindOpponent = { viewModel.findOpponent() },
                    onRefresh = { viewModel.loadData() }
                )
            }
        }
    }
}