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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.RegisterRequest
import com.example.securepool.model.ScoreResponse
import com.example.securepool.ui.theme.SecurePoolTheme
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePoolTheme {
                LoginScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SecurePool Login") }) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Enter Gamer Username") },
                    singleLine = true
                )

                Button(onClick = {
                    if (username.isBlank()) {
                        Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            // Register the user (idempotent backend behavior)
                            RetrofitClient.apiService.registerUser(RegisterRequest(username))

                            // Fetch user score
                            val response = RetrofitClient.apiService.getScore(username)
                            if (response.isSuccessful) {
                                val scoreData: ScoreResponse? = response.body()
                                val score = scoreData?.score ?: 100

                                PlayerData.username = username
                                PlayerData.score = score
                                PlayerData.firstLoginTime = System.currentTimeMillis()

                                context.startActivity(Intent(context, MainActivity::class.java))
                            } else {
                                Toast.makeText(context, "Failed to retrieve user score", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Login")
                }
            }
        }
    )
}