package com.example.securepool

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
import androidx.compose.ui.unit.dp
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.MatchResultRequest
import com.example.securepool.ui.theme.SecurePoolTheme
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePoolTheme {
                GameScreen { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(onEnd: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = PlayerData.username
    val opponent = PlayerData.opponent

    var playerScore by remember { mutableStateOf(PlayerData.score) }
    var opponentScore by remember { mutableStateOf(100) }

    fun applyAndSyncScores(outcome: String) {
        PlayerData.score = playerScore

        val result = when (outcome) {
            "win" -> MatchResultRequest(player, opponent, "win")
            "lose" -> MatchResultRequest(opponent, player, "lose")
            "exit" -> MatchResultRequest(opponent, player, "exit")
            else -> return
        }

        scope.launch {
            try {
                val response = RetrofitClient.apiService.sendMatchResult(result)
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Failed to sync match result", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        onEnd()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Match: $player vs $opponent") }) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("$player Score: $playerScore", style = MaterialTheme.typography.bodyLarge)
                Text("$opponent Score: $opponentScore", style = MaterialTheme.typography.bodyLarge)

                Button(onClick = {
                    playerScore += 10
                    opponentScore = maxOf(0, opponentScore - 10)
                    applyAndSyncScores("win")
                }) {
                    Text("Win (+10 for you, –10 for opponent)")
                }

                Button(onClick = {
                    playerScore = maxOf(0, playerScore - 10)
                    opponentScore += 10
                    applyAndSyncScores("lose")
                }) {
                    Text("Lose (–10 for you, +10 for opponent)")
                }

                Button(onClick = {
                    playerScore = maxOf(0, playerScore - 10)
                    opponentScore += 10
                    applyAndSyncScores("exit")
                }) {
                    Text("Exit During Game (–10 for you, +10 for opponent)")
                }
            }
        }
    )
}