package com.example.securepool

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.*
import com.example.securepool.ui.theme.SecurePoolTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val launchMode = intent.getStringExtra("mode") ?: "home"

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

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
    val currentTime = remember { mutableStateOf("") }

    fun fetchPlayerData() {
        scope.launch {
            try {
                val scoreResponse = RetrofitClient.apiService.getScore(usernameInput)
                val leaderboardResponse = RetrofitClient.apiService.getLeaderboard()

                if (scoreResponse.isSuccessful && leaderboardResponse.isSuccessful) {
                    val scoreData = scoreResponse.body()
                    val newScore = scoreData?.score ?: 100
                    val fullLeaderboard = leaderboardResponse.body() ?: emptyList()

                    PlayerData.username = usernameInput
                    PlayerData.score = newScore
                    PlayerData.lastZeroTimestamp = scoreData?.lastZeroTimestamp
                    PlayerData.firstLoginTime = System.currentTimeMillis()

                    playerScore = newScore
                    leaderboard = fullLeaderboard
                    isLoaded = true

                    if (newScore == 0 && scoreData?.lastZeroTimestamp != null) {
                        currentTime.value = LocalDateTime.now().format(fmt)
                    }
                } else {
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Backend error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(usernameInput) {
        if (usernameInput.isNotEmpty()) {
            fetchPlayerData()
        }
    }

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

                Text("Current Score: $playerScore", style = MaterialTheme.typography.headlineSmall)

                if (playerScore == 0 && PlayerData.lastZeroTimestamp != null) {
                    Text("Last Zero Time: ${PlayerData.lastZeroTimestamp}")
                    Text("Current Time: ${currentTime.value}")
                }

                Button(onClick = {
                    if (PlayerData.score == 0) {
                        currentTime.value = LocalDateTime.now().format(fmt)

                        val elapsed = System.currentTimeMillis() - PlayerData.firstLoginTime
                        if (elapsed >= 120000) {
                            scope.launch {
                                try {
                                    val req = RegisterRequest(PlayerData.username, "placeholder")
                                    val res = RetrofitClient.apiService.restoreScore(req)
                                    if (res.isSuccessful) {
                                        PlayerData.score = 100
                                        PlayerData.lastZeroTimestamp = null
                                        PlayerData.firstLoginTime = System.currentTimeMillis()
                                        playerScore = 100
                                        Toast.makeText(context, "Cooldown expired. Points restored!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Restore request failed", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Cooldown active. Try Practice Room.", Toast.LENGTH_SHORT).show()
                        }
                        return@Button
                    }

                    val eligibleOpponents = leaderboard
                        .filter { it.username != PlayerData.username && it.score > 0 }
                        .map { it.username }

                    if (eligibleOpponents.isEmpty()) {
                        Toast.makeText(context, "No opponent available to match!", Toast.LENGTH_SHORT).show()
                    } else {
                        PlayerData.opponent = eligibleOpponents.random()
                        context.startActivity(Intent(context, GameActivity::class.java))
                    }
                }, enabled = isLoaded) {
                    Text("Start Match")
                }

                Button(onClick = {
                    context.startActivity(Intent(context, PracticeActivity::class.java))
                }, enabled = isLoaded) {
                    Text("Practice Room")
                }

                Button(onClick = {
                    context.startActivity(Intent(context, LeaderboardActivity::class.java))
                }, enabled = isLoaded) {
                    Text("Show Ranking")
                }
            }
        }
    )
}