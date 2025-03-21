package com.example.explorandes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import com.example.explorandes.services.BrightnessController
import com.example.explorandes.services.LightSensorManager

class MainActivity : ComponentActivity() {

    private lateinit var lightSensorManager: LightSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

         // Initialize light sensor
        lightSensorManager = LightSensorManager(this) { lux ->
            adjustBrightness(lux)
        }

        lightSensorManager.startListening()
        
        setContent {
            AppNavigator()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lightSensorManager.stopListening()
    }

    private fun adjustBrightness(lux: Float) {
        val brightnessLevel = when {
            lux < 20 -> 1.0f  // Increase brightness in low light
            lux < 100 -> 0.7f
            lux < 500 -> 0.5f
            else -> 0.3f  // Reduce brightness in bright light
        }

        BrightnessController.setBrightness(this, brightnessLevel)
    }
}

// Controlador de navegación
@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000) // Espera 2 segundos
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController) }
            composable("login") { LoginScreen() }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0F29)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Explor", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Andes", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Explore UniAndes Like Never Before", fontSize = 14.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Explor", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Andes", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("login") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("Start Exploring", color = Color.White)
            }
        }
    }
}

@Composable
fun LoginScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Find Your Way Around",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE91E63)
            )
            Text("Your interactive campus map at a glance", fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            // Campos de entrada
            TextField(value = "", onValueChange = {}, placeholder = { Text("Email or Phone Number") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = "", onValueChange = {}, placeholder = { Text("Password") })

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // Launch HomeActivity with navigation flag
                    val intent = Intent(context, HomeActivity::class.java).apply {
                        // This will tell HomeActivity to open with the navigation tab selected
                        putExtra("OPEN_NAVIGATION", true)
                    }
                    context.startActivity(intent)
                    // Finish current activity if needed
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("Sign In", color = Color.White)
            }

            // Skip login button for development
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    // Launch HomeActivity with navigation flag
                    val intent = Intent(context, HomeActivity::class.java).apply {
                        // This will tell HomeActivity to open with the navigation tab selected
                        putExtra("OPEN_NAVIGATION", true)
                    }
                    context.startActivity(intent)
                    // Finish current activity if needed
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            ) {
                Text("Skip Login (Development Only)", color = Color.Gray)
            }
        }
    }
}
