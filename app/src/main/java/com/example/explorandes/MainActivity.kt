package com.example.explorandes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.explorandes.services.BrightnessController
import com.example.explorandes.services.LightSensorManager
import com.example.explorandes.api.ApiClient
import com.example.explorandes.models.AuthRequest
import com.example.explorandes.models.RegisterRequest
import com.example.explorandes.utils.SessionManager
import com.bumptech.glide.Glide
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.FileStorage
import com.example.explorandes.utils.DataStoreManager
import com.google.gson.JsonSyntaxException
import java.io.IOException
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.isActive
import com.example.explorandes.utils.UserDataCache


class MainActivity : BaseActivity() {


    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize sessionManager at activity level
        sessionManager = SessionManager(this)

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "User is already logged in, navigating to HomeActivity")
            startHomeActivity()
            return
        }

        // Inicializar ApiClient con el contexto
        ApiClient.init(applicationContext)

        setContent {
            AppNavigator()
        }

        

        // Initialize ApiClient
        ApiClient.init(applicationContext)
    }

    

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

// Navigation controller a
@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    // Check if user is already logged in
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    LaunchedEffect(Unit) {
        delay(2000) // Wait 2 seconds
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F29)),
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
    // Get context for connectivity check
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectivityHelper = remember { ConnectivityHelper(context) }

    // State for connectivity
    var isConnected by remember { mutableStateOf(connectivityHelper.isInternetAvailable()) }
    var isChecking by remember { mutableStateOf(false) }

    // In your HomeScreen, replace the infinite loop with this
    LaunchedEffect(Unit) {
        // Set up a repeating job with a safe cancellation policy
        val job = scope.launch {
            while (isActive) { // isActive is a property of CoroutineScope that is false when cancelled
                try {
                    isConnected = connectivityHelper.isInternetAvailable()
                    delay(5000) // Move delay after the operation
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error checking connectivity", e)
                    delay(10000) // Longer delay after error
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F29)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Explor", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Andes", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))

            // Show offline warning if needed
            if (!isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3250))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "You're currently offline",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Login and registration require internet connection",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // Use coroutine to check connection asynchronously
                                isChecking = true
                                scope.launch {
                                    // Simulate network operation
                                    delay(1000)
                                    isConnected = connectivityHelper.isInternetAvailable()
                                    isChecking = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Check Connection", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isConnected) {
                        navController.navigate("login")
                    } else {
                        // Use another coroutine to show a delayed message
                        scope.launch {
                            isChecking = true
                            delay(500)
                            isChecking = false
                            isConnected = connectivityHelper.isInternetAvailable()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFE91E63) else Color(0xFF752F44)
                )
            ) {
                Text("Sign In", color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (isConnected) {
                        navController.navigate("register")
                    } else {
                        // Use another coroutine to show a delayed message
                        scope.launch {
                            isChecking = true
                            delay(500)
                            isChecking = false
                            isConnected = connectivityHelper.isInternetAvailable()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFF3949AB) else Color(0xFF2A3570)
                )
            ) {
                Text("Create Account", color = Color.White)
            }

            // BOTON VERDE
// Add after the Create Account button
            Spacer(modifier = Modifier.height(16.dp))

// State to control dialog visibility
            var showDialog by remember { mutableStateOf(false) }
// State to track the currently viewed settings
            var savedSettings by remember { mutableStateOf<Map<String, String>?>(null) }

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Storage Demo", color = Color.White)
            }

// Show the dialog when showDialog is true
            if (showDialog) {
                val context = LocalContext.current
                val prefs = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }

                // Read existing settings when dialog opens
                LaunchedEffect(Unit) {
                    val theme = prefs.getString("theme", "Not set") ?: "Not set"
                    val notification = prefs.getString("notification", "Not set") ?: "Not set"
                    val lastLogin = prefs.getString("lastLogin", "Never") ?: "Never"

                    savedSettings = mapOf(
                        "theme" to theme,
                        "notification" to notification,
                        "lastLogin" to lastLogin
                    )
                }

                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Local Storage Demo") },
                    text = {
                        Column {
                            Text(
                                "ExplorAndes uses multiple local storage strategies:",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("1. SharedPreferences: Stores user settings (theme, language)")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("2. Local Files: Saves offline account information to log in")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("3. Key-Value Database: Stores structured data for offline use")

                            // Show saved settings if available
                            savedSettings?.let { settings ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color(0xFF3949AB), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Currently Saved Settings:", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Format time to readable form if it exists
                                val formattedTime = try {
                                    val timestamp = settings["lastLogin"]?.toLongOrNull() ?: 0L
                                    if (timestamp > 0) {
                                        val date = java.util.Date(timestamp)
                                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        formatter.format(date)
                                    } else {
                                        "Never"
                                    }
                                } catch (e: Exception) {
                                    "Unknown"
                                }

                                // Display settings nicely
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Theme:", color = Color.Gray)
                                    Text(settings["theme"] ?: "Not set")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Notifications:", color = Color.Gray)
                                    Text(settings["notification"] ?: "Not set")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Last Login:", color = Color.Gray)
                                    Text(formattedTime)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Store user preferences in structured format
                                val userPrefs = mapOf(
                                    "theme" to "dark",
                                    "notification" to "enabled",
                                    "lastLogin" to System.currentTimeMillis().toString()
                                )

                                // Save to SharedPreferences with type information preserved
                                with(prefs.edit()) {
                                    putString("theme", userPrefs["theme"])
                                    putString("notification", userPrefs["notification"])
                                    putString("lastLogin", userPrefs["lastLogin"])
                                    apply()
                                }

                                // ALSO save to a local file to demonstrate file storage
                                try {
                                    val jsonContent = """
                            {
                                "theme": "${userPrefs["theme"]}",
                                "notification": "${userPrefs["notification"]}",
                                "lastLogin": "${userPrefs["lastLogin"]}"
                            }
                        """.trimIndent()

                                    context.openFileOutput("user_settings.json", Context.MODE_PRIVATE).use { outputStream ->
                                        outputStream.write(jsonContent.toByteArray())
                                    }

                                    Toast.makeText(context, "Settings saved to SharedPreferences AND local file!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error saving to file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                                // Update display
                                savedSettings = userPrefs
                            }
                        ) {
                            Text("Save Example")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showDialog = false }
                        ) {
                            Text("Close")
                        }
                    }
                )
            }

            // Show checking message if needed
            if (isChecking) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Checking connection status...",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    // Get the context to launch intent
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Create session manager
    val sessionManager = remember { SessionManager(context) }

    // State for input fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

// Get Remember Me state from SharedPreferences
    val prefs = remember { context.getSharedPreferences("user_credentials", Context.MODE_PRIVATE) }
    var rememberMe by remember { mutableStateOf(prefs.getBoolean("remember_me", false)) }

// Load saved credentials when screen opens
    LaunchedEffect(Unit) {
        if (rememberMe) {
            val savedEmail = prefs.getString("saved_email", "")
            val savedPassword = prefs.getString("saved_password", "")

            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                email = savedEmail
                password = savedPassword
            }
        }
    }

    // State for loading and error
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F29)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Sign In",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE91E63)
            )
            Text("Find your way around campus", fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            // Input fields
            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            // Remember Me checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = {
                        rememberMe = it
                        // Save the Remember Me preference immediately
                        prefs.edit().putBoolean("remember_me", it).apply()
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFE91E63),
                        uncheckedColor = Color.Gray
                    )
                )

                Text(
                    text = "Remember me",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Display error message if any
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // First check for connectivity
                    if (!ConnectivityHelper(context).isInternetAvailable()) {
                        errorMessage = "Cannot login while offline. Please connect to the internet."
                        return@Button
                    }
                    // Validate inputs
                    when {
                        email.isEmpty() -> errorMessage = "Email is required"
                        password.isEmpty() -> errorMessage = "Password is required"
                        else -> {
                            // Clear previous error
                            errorMessage = null

                            // ADD THIS CODE HERE - SAVE CREDENTIALS IF REMEMBER ME IS CHECKED
                            // Get the Remember Me state (we need to get it from outside the Row)
                            val rememberMe = (context.getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
                                .getBoolean("remember_me", false))

                            // Save credentials if Remember Me is checked
                            if (rememberMe) {
                                val prefs = context.getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("saved_email", email)
                                    .putString("saved_password", password)
                                    .putBoolean("credentials_saved", true)
                                    .apply()
                            }
                            // END OF ADDED CODE



                            isLoading = true

                            // Perform login with error handling
                            scope.launch {
                                try {
                                    val response = ApiClient.apiService.login(AuthRequest(email = email, password = password))

                                    if (response.isSuccessful && response.body() != null) {
                                        // Obtain response
                                        val authResponse = response.body()!!
                                        Log.d("LoginScreen", "Login successful: ${authResponse.token}")

                                        // Save token
                                        sessionManager.saveToken(authResponse.token)

                                        // Extract and save user data
                                        val userId = authResponse.id ?: -1L
                                        val userEmail = authResponse.email ?: ""
                                        val userName = authResponse.username
                                            ?: authResponse.firstName
                                            ?: email.split('@')[0]
                                        val profilePicUrl = authResponse.user?.profileImageUrl

                                        // Save comprehensive user info
                                        if (userId > 0) {
                                            try {
                                                sessionManager.saveUserInfo(userId, userEmail, userName)

                                                // Save profile pic URL if available
                                                if (profilePicUrl != null && profilePicUrl.isNotEmpty()) {
                                                    sessionManager.saveProfilePictureUrl(profilePicUrl)
                                                }

                                                ///CACHE
                                                // CACHE USER PROFILE IN ARRAYMAP - Add this code
                                                // Create profile data
                                                val userProfile = mapOf(
                                                    "id" to userId,
                                                    "email" to userEmail,
                                                    "username" to userName,
                                                    "profilePicUrl" to (profilePicUrl ?: ""),
                                                    "lastLogin" to System.currentTimeMillis()
                                                )

                                                // Save to UserDataCache using ArrayMap (memory-efficient)
                                                UserDataCache.put(UserDataCache.KEY_PROFILE, userProfile)
                                                Log.d("LoginScreen", "User profile cached using ArrayMap: $userName")

                                                // Initialize other cache collections if needed
                                                if (!UserDataCache.contains(UserDataCache.KEY_VISITED_BUILDINGS)) {
                                                    UserDataCache.put(UserDataCache.KEY_VISITED_BUILDINGS, mutableListOf<Map<String, Any>>())
                                                }

                                                if (!UserDataCache.contains(UserDataCache.KEY_FAVORITE_BUILDINGS)) {
                                                    UserDataCache.put(UserDataCache.KEY_FAVORITE_BUILDINGS, mutableMapOf<Long, String>())
                                                }

                                                if (!UserDataCache.contains(UserDataCache.KEY_RECENT_SEARCHES)) {
                                                    UserDataCache.put(UserDataCache.KEY_RECENT_SEARCHES, mutableListOf<String>())
                                                }


                                                // Navigate to home
                                                val intent = Intent(context, HomeActivity::class.java)
                                                context.startActivity(intent)
                                                (context as? ComponentActivity)?.finish()
                                            } catch (e: Exception) {
                                                Log.e("LoginScreen", "Error saving user info", e)
                                                errorMessage = "Error saving user data: ${e.localizedMessage}"
                                            }
                                        } else {
                                            errorMessage = "Invalid user ID received from server"
                                        }
                                    } else {
                                        // Handle HTTP errors with proper response handling
                                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                        Log.e("LoginScreen", "Login error: ${response.code()} - $errorBody")

                                        errorMessage = when (response.code()) {
                                            401 -> "Invalid email or password"
                                            403 -> "Access denied"
                                            404 -> "Service not found"
                                            500 -> "Server error. Please try again later."
                                            else -> "Login failed: ${response.code()}"
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Handle network or other exceptions
                                    Log.e("LoginScreen", "Login exception", e)

                                    errorMessage = when (e) {
                                        is IOException -> "Network error. Please check your connection."
                                        is JsonSyntaxException -> "Error parsing server response."
                                        else -> "Login failed: ${e.localizedMessage ?: "Unknown error"}"
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Sign In", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate("register") }
            ) {
                Text("Don't have an account? Sign Up", color = Color(0xFFE91E63))
            }

            // Offline mode notice
            if (!ConnectivityHelper(context).isInternetAvailable()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You're offline. Login requires internet connection.",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
@Composable
fun RegisterScreen(navController: NavHostController) {
    // Get the context to launch intent
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Create session manager
    val sessionManager = remember { SessionManager(context) }

    // State for input fields
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    // State for loading and error
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F29)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Create Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3949AB)
            )
            Text("Join the ExplorAndes community", fontSize = 16.sp, color = Color.White)
            // Add offline warning
            if (!ConnectivityHelper(context).isInternetAvailable()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3250))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "You're offline",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Registration requires an internet connection",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input fields
            TextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = firstName,
                onValueChange = { firstName = it },
                placeholder = { Text("First Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = lastName,
                onValueChange = { lastName = it },
                placeholder = { Text("Last Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1A1F39),
                    focusedContainerColor = Color(0xFF1A1F39),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

            // Display error message if any
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // First check connectivity
                    if (!ConnectivityHelper(context).isInternetAvailable()) {
                        errorMessage = "Cannot register while offline. Please connect to the internet."
                        return@Button
                    }
                    // Validate inputs
                    when {
                        username.isEmpty() -> errorMessage = "Username is required"
                        email.isEmpty() -> errorMessage = "Email is required"
                        !email.contains("@") -> errorMessage = "Please enter a valid email"
                        password.isEmpty() -> errorMessage = "Password is required"
                        password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                        password != confirmPassword -> errorMessage = "Passwords don't match"
                        else -> {
                            // Clear previous error
                            errorMessage = null
                            isLoading = true

                            // Create register request object
                            val registerRequest = RegisterRequest(
                                username = username,
                                email = email,
                                password = password,
                                firstName = firstName.takeIf { it.isNotEmpty() },
                                lastName = lastName.takeIf { it.isNotEmpty() }
                            )

                            // Perform registration
                            scope.launch {
                                try {
                                    Log.d("RegisterScreen", "Enviando solicitud de registro: $registerRequest")
                                    val response = ApiClient.apiService.register(registerRequest)

                                    if (response.isSuccessful && response.body() != null) {
                                        // Obtener la respuesta
                                        val authResponse = response.body()!!
                                        Log.d("RegisterScreen", "Registro exitoso: ${authResponse.token}")

                                        // Guardar token
                                        sessionManager.saveToken(authResponse.token)

                                        // Extraer datos del usuario de la respuesta plana
                                        val userId = authResponse.id
                                        val userEmail = authResponse.email
                                        val userName = authResponse.username ?: authResponse.firstName ?: email.split('@')[0]

                                        if (userId != null && userEmail != null) {
                                            Log.d("RegisterScreen", "Datos de usuario: id=$userId, email=$userEmail, name=$userName")
                                            sessionManager.saveUserInfo(userId, userEmail, userName ?: username)

                                            // Verificar que se guardó correctamente
                                            if (sessionManager.getUserId() > 0) {
                                                // Navigate to home
                                                val intent = Intent(context, HomeActivity::class.java)
                                                context.startActivity(intent)
                                                // Finish current activity if needed
                                                if (context is ComponentActivity) {
                                                    context.finish()
                                                }
                                            } else {
                                                errorMessage = "Error al guardar datos de usuario"
                                            }
                                        } else {
                                            errorMessage = "No se recibieron datos de usuario en la respuesta"
                                            Log.e("RegisterScreen", "No hay datos de usuario en la respuesta")
                                            // Eliminamos el token ya que no tenemos datos completos
                                            sessionManager.logout()
                                        }
                                    } else {
                                        errorMessage = "Registration failed: ${response.code()} - ${response.errorBody()?.string()}"
                                        Log.e("RegisterScreen", "Error en registro: ${response.code()}")
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Registration failed: ${e.localizedMessage}"
                                    Log.e("RegisterScreen", "Excepción en registro: ${e.message}", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Create Account", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate("login") }
            ) {
                Text("Already have an account? Sign In", color = Color(0xFFE91E63))
            }
        }
    }

}