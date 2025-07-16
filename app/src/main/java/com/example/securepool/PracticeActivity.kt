package com.example.securepool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.securepool.ui.theme.SecurePoolTheme

class PracticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePoolTheme {
                PracticeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen() {
    val username = PlayerData.username
    val score = PlayerData.score

    Scaffold(
        topBar = { TopAppBar(title = { Text("Practice Mode — $username") }) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Hello $username 👋", style = MaterialTheme.typography.headlineSmall)
                Text("You're currently in cooldown or low-score mode.", style = MaterialTheme.typography.bodyLarge)
                Text("Your current score is $score. You can train here, but scores won’t change.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}