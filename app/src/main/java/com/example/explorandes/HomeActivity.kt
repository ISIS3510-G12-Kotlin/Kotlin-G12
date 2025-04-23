package com.example.explorandes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.adapters.BuildingAdapter
import com.example.explorandes.adapters.RecommendationAdapter
import com.example.explorandes.api.ApiClient
import com.example.explorandes.models.Building
import com.example.explorandes.models.Recommendation
import com.example.explorandes.models.RecommendationType
import com.example.explorandes.ui.account.AccountFragment
import com.example.explorandes.ui.navigation.NavigationFragment
import com.example.explorandes.utils.SessionManager
import com.example.explorandes.viewmodels.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var buildingsRecyclerView: RecyclerView
    private lateinit var recommendationsRecyclerView: RecyclerView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var fragmentContainer: View
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: HomeViewModel
    private lateinit var buildingAdapter: BuildingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize ApiClient - This is critical!
        ApiClient.init(applicationContext)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Verify authentication
        if (!sessionManager.isLoggedIn()) {
            Log.d("HomeActivity", "No active session, redirecting to login")
            navigateToLogin()
            return
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Set up observers
        setupViewModelObservers()

        // Load user data
        viewModel.loadUserData(sessionManager)

        // Initialize UI
        initializeUI()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Make sure the correct tab is selected when returning to this activity
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        if (currentFragment is AccountFragment) {
            bottomNavigation.selectedItemId = R.id.navigation_account
        } else if (findViewById<View>(R.id.nestedScrollView).visibility == View.VISIBLE) {
            bottomNavigation.selectedItemId = R.id.navigation_home
        }
    }

    private fun setupViewModelObservers() {
        // User data changes
        viewModel.user.observe(this) { user ->
            Log.d("HomeActivity", "User loaded: ${user.username}")
            // Update user info in SessionManager
            sessionManager.saveUserInfo(user.id, user.email, user.username)
        }

        // Error handling
        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Log.e("HomeActivity", "Error: $it")
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()

                // Authentication error, redirect to login
                if (it.contains("401") || it.contains("no encontró")) {
                    sessionManager.logout()
                    navigateToLogin()
                }
            }
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // You could show/hide a loading indicator
        }

        // Buildings data
        viewModel.buildings.observe(this) { buildings ->
            if (buildings.isNotEmpty()) {
                buildingAdapter.updateData(buildings)
            } else {
                Log.d("HomeActivity", "No buildings data received")
            }
        }
    }

    private fun initializeUI() {
        // Find views for navigation
        nestedScrollView = findViewById(R.id.nestedScrollView)
        fragmentContainer = findViewById(R.id.fragment_container)

        // Show username
        val userName = sessionManager.getUsername() ?: "Usuario"
        findViewById<TextView>(R.id.greeting_text).text = "Hola, $userName"

        // Setup UI components
        setupCategoryIcons()
        setupRecyclerViews()
        setupBottomNavigation()
        setupClickListeners()

        // Load buildings from API
        viewModel.loadBuildings()

        // Check if we should open directly to the navigation tab
        if (intent.getBooleanExtra("OPEN_NAVIGATION", false)) {
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigation.selectedItemId = R.id.navigation_navigate
        }
    }

    private fun setupCategoryIcons() {
        // Find all category views
        val categoryBuildings = findViewById<View>(R.id.category_buildings)
        //val categoryEvents = findViewById<View>(R.id.category_events)
        val categoryFood = findViewById<View>(R.id.category_food)
        //val categoryStudy = findViewById<View>(R.id.category_study)
        val categoryServices = findViewById<View>(R.id.category_services)

        // Hide Events and Study categories since we're not using them
        findViewById<View>(R.id.category_events).visibility = View.GONE
        findViewById<View>(R.id.category_study).visibility = View.GONE

        // Setup Buildings category
        categoryBuildings.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_building)
        categoryBuildings.findViewById<TextView>(R.id.category_name).text = getString(R.string.buildings)

        // Setup Events category
        //categoryEvents.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_event)
        //categoryEvents.findViewById<TextView>(R.id.category_name).text = getString(R.string.events)

        // Setup Food & Rest category
        categoryFood.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_food)
        categoryFood.findViewById<TextView>(R.id.category_name).text = getString(R.string.food_rest)

        // Setup Study Spaces category
        //categoryStudy.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_study)
        //categoryStudy.findViewById<TextView>(R.id.category_name).text = getString(R.string.study_spaces)

        // Setup Services category
        categoryServices.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_services)
        categoryServices.findViewById<TextView>(R.id.category_name).text = getString(R.string.services)

        // Set click listeners for categories
        categoryBuildings.setOnClickListener {
            viewModel.loadBuildingsByCategory("Buildings")
        }
        categoryFood.setOnClickListener {
            viewModel.loadBuildingsByCategory("Food")
        }
        categoryServices.setOnClickListener {
            viewModel.loadBuildingsByCategory("Services")
        }
    }

    private fun setupRecyclerViews() {
        // Setup Buildings RecyclerView
        buildingsRecyclerView = findViewById(R.id.buildings_recycler)
        buildingsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Initialize with empty list, will be populated from API
        buildingAdapter = BuildingAdapter(emptyList()) { building ->
            // Navigate to building details or show places in this building
            val intent = Intent(this, BuildingDetailActivity::class.java).apply {
                putExtra("BUILDING_ID", building.id)
            }
            startActivity(intent)
        }
        buildingsRecyclerView.adapter = buildingAdapter

        // Setup Recommendations RecyclerView
        recommendationsRecyclerView = findViewById(R.id.recommendations_recycler)
        recommendationsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Create sample data for recommendations
        val recommendations = listOf(
            Recommendation(1L, "Medium", "Anestesia Alberto López", R.drawable.profile_placeholder, RecommendationType.PODCAST),
            Recommendation(2L, "CHOBA", "Audición Alberto López", R.drawable.profile_placeholder, RecommendationType.DOCUMENTARY),
            Recommendation(3L, "C4", "Audición María Pérez", R.drawable.profile_placeholder, RecommendationType.THEATER)
        )

        val recommendationAdapter = RecommendationAdapter(recommendations) { recommendation ->
            Toast.makeText(this, "Selected: ${recommendation.title}", Toast.LENGTH_SHORT).show()
        }
        recommendationsRecyclerView.adapter = recommendationAdapter
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showHomeContent()
                    true
                }
                R.id.navigation_favorites -> {
                    Toast.makeText(this, getString(R.string.favorites), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_navigate -> {
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    // Return false to prevent the item from being selected when returning from MapActivity
                    false
                }
                R.id.navigation_notifications -> {
                    Toast.makeText(this, getString(R.string.notifications), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_account -> {
                    loadFragment(AccountFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        // Set click listeners for the "See all" buttons
        findViewById<TextView>(R.id.see_all_buildings).setOnClickListener {
            // Navigate to buildings list or open navigation tab
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigation.selectedItemId = R.id.navigation_navigate
        }

        findViewById<TextView>(R.id.see_all_recommendations).setOnClickListener {
            Toast.makeText(this, "See all recommendations", Toast.LENGTH_SHORT).show()
        }
        
        // Setup search functionality
        val searchInput = findViewById<EditText>(R.id.search_input)
        searchInput.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
            ) {
                val query = textView.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchBuildings(query)
                    // Hide keyboard
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(textView.windowToken, 0)
                }
                true
            } else {
                false
            }
        }
        
        // Setup filter button
        findViewById<ImageView>(R.id.filter_button).setOnClickListener {
            // Show category selection dialog or filter options
            val categories = arrayOf("All", "Buildings", "Food", "Services")
            
            AlertDialog.Builder(this)
                .setTitle("Filter by Category")
                .setItems(categories) { _, which ->
                    val category = categories[which]
                    if (category == "All") {
                        viewModel.loadBuildings()
                    } else {
                        viewModel.loadBuildingsByCategory(category)
                    }
                }
                .show()
        }
    }

    private fun showHomeContent() {
        // Hide any fragments and show the main content
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }

        // Show the home content
        nestedScrollView.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
    }

    private fun loadFragment(fragment: Fragment) {
        // Hide the home content
        nestedScrollView.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE

        // Load the fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}