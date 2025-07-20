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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.securepool.api.RetrofitClient
import com.example.securepool.model.*
import com.example.securepool.ui.theme.SecurePoolTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.util.*

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePoolTheme { LoginScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
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

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(icon, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Button(onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Enter both fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            val request = RegisterRequest(username, password)
                            val loginResponse = RetrofitClient.apiService.loginUser(request)
                            val loginBody = loginResponse.body()

                            if (loginResponse.isSuccessful && loginBody?.success == true) {

                                PlayerData.username = loginBody.username
                                PlayerData.score = loginBody.score
                                PlayerData.lastZeroTimestamp = loginBody.lastZeroTimestamp

                                if (PlayerData.score == 0 && PlayerData.lastZeroTimestamp != null) {
                                    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
                                    val past = try {
                                        LocalDateTime.parse(PlayerData.lastZeroTimestamp, fmt)
                                    } catch (_: Exception) { null }

                                    if (past != null) {
                                        val elapsed = Duration.between(past, LocalDateTime.now()).toMinutes()
                                        if (elapsed >= 2) {
                                            val restoreRes = RetrofitClient.apiService.restoreScore(request)
                                            if (restoreRes.isSuccessful) {
                                                val newScore = RetrofitClient.apiService.getScore(username).body()?.score ?: 100
                                                PlayerData.score = newScore
                                                PlayerData.lastZeroTimestamp = null
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Out of points – Train up – Your score will recharge to 100 after 24h.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }

                                val intent = Intent(context, MainActivity::class.java)
                                intent.putExtra("mode", "home")
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Login")
                }

                TextButton(onClick = {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("mode", "signup")
                    context.startActivity(intent)
                }) {
                    Text("Create Account")
                }
            }
        }
    )
}