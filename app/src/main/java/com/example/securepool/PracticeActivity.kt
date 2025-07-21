package com.example.securepool

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.securepool.ui.NavigationEvent
import com.example.securepool.ui.model.PracticeUiState
import com.example.securepool.ui.model.PracticeViewModel
import com.example.securepool.ui.theme.SecurePoolTheme

class PracticeActivity : ComponentActivity() {

    private val viewModel: PracticeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecurePoolTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is NavigationEvent.NavigateToLogin -> {
                                Toast.makeText(this@PracticeActivity, "Please log in.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@PracticeActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                            else -> {} // Ignore other events
                        }
                    }
                }

                PracticeScreen(uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(uiState: PracticeUiState) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Practice Mode — ${uiState.username}") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text("Hello ${uiState.username} 👋", style = MaterialTheme.typography.headlineSmall)
                Text("You're currently in cooldown or low-score mode.", style = MaterialTheme.typography.bodyLarge)
                Text("Your current score is ${uiState.score}. You can train here, but scores won’t change.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}