package com.example.securepool

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.LeaderboardEntry
import com.example.securepool.ui.theme.SecurePoolTheme
import kotlinx.coroutines.launch

class LeaderboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePoolTheme {
                LeaderboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen() {
    val context = LocalContext.current
    val username = PlayerData.username
    val scope = rememberCoroutineScope()

    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }

    // ✅ Fetch leaderboard from backend
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getLeaderboard()
            if (response.isSuccessful) {
                leaderboard = response.body() ?: emptyList()
                isLoaded = true
            } else {
                Toast.makeText(context, "Failed to load leaderboard", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Leaderboard — Logged in as $username") })
        },
        content = { padding ->
            if (!isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    leaderboard.forEach {
                        Text("${it.username}: ${it.score} pts", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    )
}