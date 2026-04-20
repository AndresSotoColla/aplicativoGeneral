package com.example.aplicativogeneral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf(sharedPreferences.getString("username", "") ?: "") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(sharedPreferences.getBoolean("rememberMe", false)) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B5E20), // Dark Green
            Color(0xFF4CAF50)  // Light Green
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Muestreos Sumapaz",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                    )
                    Text(text = "Recordar usuario", color = Color(0xFF1B5E20), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL("https://productorescampesinos.com/api/login_movil.php")
                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "POST"
                                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                                connection.doOutput = true

                                val jsonParam = JSONObject()
                                jsonParam.put("username", username.trim())
                                jsonParam.put("password", password)

                                val os = connection.outputStream
                                val writer = OutputStreamWriter(os, "UTF-8")
                                writer.write(jsonParam.toString())
                                writer.flush()
                                writer.close()
                                os.close()

                                val responseCode = connection.responseCode
                                val inputStream = if (responseCode in 200..299) {
                                    connection.inputStream
                                } else {
                                    connection.errorStream ?: connection.inputStream
                                }

                                val reader = BufferedReader(InputStreamReader(inputStream))
                                val responseText = reader.use { it.readText() }

                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    
                                    val isError = responseCode !in 200..299 || responseText.contains("error", ignoreCase = true) || responseText.contains("incorrect", ignoreCase = true)
                                    
                                    if (isError) {
                                        if (responseText.contains("No tienes permitido", ignoreCase = true)) {
                                            Toast.makeText(context, "No tienes permitido acceso al aplicativo movil", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        if (rememberMe) {
                                            sharedPreferences.edit()
                                                .putString("username", username.trim())
                                                .putBoolean("rememberMe", true)
                                                .apply()
                                        } else {
                                            sharedPreferences.edit()
                                                .remove("username")
                                                .putBoolean("rememberMe", false)
                                                .apply()
                                        }
                                        onLoginSuccess()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "INGRESAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
