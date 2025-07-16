package com.example.securepool

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.RegisterRequest
import com.example.securepool.model.ScoreResponse
import com.example.securepool.model.LeaderboardEntry
import com.example.securepool.ui.theme.SecurePoolTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePoolTheme {
                SecurePoolHomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurePoolHomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var usernameInput by remember { mutableStateOf(PlayerData.username) }
    var playerScore by remember { mutableStateOf(0) }
    var isLoaded by remember { mutableStateOf(false) }
    var leaderboard by remember { mutableStateOf(listOf<LeaderboardEntry>()) }

    fun fetchPlayerData() {
        scope.launch {
            try {
                RetrofitClient.apiService.registerUser(RegisterRequest(usernameInput))

                val scoreResponse = RetrofitClient.apiService.getScore(usernameInput)
                val leaderboardResponse = RetrofitClient.apiService.getLeaderboard()

                if (scoreResponse.isSuccessful && leaderboardResponse.isSuccessful) {
                    val scoreData: ScoreResponse? = scoreResponse.body()
                    val newScore = scoreData?.score ?: 100
                    val fullLeaderboard = leaderboardResponse.body() ?: emptyList()

                    PlayerData.username = usernameInput
                    PlayerData.score = newScore
                    PlayerData.firstLoginTime = System.currentTimeMillis()

                    playerScore = newScore
                    leaderboard = fullLeaderboard
                    isLoaded = true
                } else {
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Backend error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Fetch on initial launch
    LaunchedEffect(usernameInput) {
        if (usernameInput.isNotEmpty()) {
            fetchPlayerData()
        }
    }

    // ✅ Re-fetch score when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isLoaded) {
                fetchPlayerData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Welcome ${PlayerData.username.ifEmpty { "Player" }}")
            })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username") },
                    enabled = !isLoaded
                )

                Text(
                    "Current Score: $playerScore",
                    style = MaterialTheme.typography.headlineSmall
                )

                Button(
                    onClick = {
                        if (PlayerData.score == 0) {
                            val elapsed = System.currentTimeMillis() - PlayerData.firstLoginTime
                            if (elapsed >= 86400000) {
                                PlayerData.score = 100
                                PlayerData.firstLoginTime = System.currentTimeMillis()
                                playerScore = PlayerData.score
                                Toast.makeText(
                                    context,
                                    "Cooldown expired. Points restored!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Cooldown active. Try Practice Room.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@Button
                        }

                        val eligibleOpponents = leaderboard
                            .filter { it.username != PlayerData.username && it.score > 0 }
                            .map { it.username }

                        if (eligibleOpponents.isEmpty()) {
                            Toast.makeText(
                                context,
                                "No opponent available to match!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            PlayerData.opponent = eligibleOpponents.random()
                            context.startActivity(Intent(context, GameActivity::class.java))
                        }
                    },
                    enabled = isLoaded
                ) {
                    Text("Start Match")
                }

                Button(
                    onClick = {
                        context.startActivity(Intent(context, PracticeActivity::class.java))
                    },
                    enabled = isLoaded
                ) {
                    Text("Practice Room")
                }

                Button(
                    onClick = {
                        context.startActivity(Intent(context, LeaderboardActivity::class.java))
                    },
                    enabled = isLoaded
                ) {
                    Text("Show Ranking")
                }
            }
        }
    )
}