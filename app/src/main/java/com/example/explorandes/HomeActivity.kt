package com.example.explorandes

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.adapters.BuildingAdapter
import com.example.explorandes.adapters.EventAdapter
import com.example.explorandes.api.ApiClient
import com.example.explorandes.fragments.EventListFragment
import com.example.explorandes.models.Recommendation
import com.example.explorandes.models.RecommendationType
import com.example.explorandes.ui.buildings.BuildingsListFragment
import com.example.explorandes.utils.SessionManager
import com.example.explorandes.viewmodels.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import com.bumptech.glide.Glide
import com.example.explorandes.utils.UserDataCache


class HomeActivity : BaseActivity() {

    private lateinit var buildingsRecyclerView: RecyclerView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var noConnectionView: View
    private lateinit var fragmentContainer: View
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: HomeViewModel
    private lateinit var buildingAdapter: BuildingAdapter
    private lateinit var eventAdapter: EventAdapter
    private lateinit var app: ExplorAndesApplication
    
    // Connectivity tracking fields
    private var wasOfflineBefore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicializar ExplorAndesApplication
        app = application as ExplorAndesApplication

        ApiClient.init(applicationContext)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        nestedScrollView = findViewById(R.id.nestedScrollView)
        fragmentContainer = findViewById(R.id.fragment_container)
        noConnectionView = findViewById(R.id.no_connection_view)

        // Use ViewModelProvider with Context Factory
        viewModel = ViewModelProvider(
            this,
            HomeViewModel.Factory(this)
        )[HomeViewModel::class.java]
        
        // Check initial connectivity
        wasOfflineBefore = !hasInternetConnection()
        if (wasOfflineBefore) {
            showNoConnection()
        }

        setupViewModelObservers()
        viewModel.loadUserData(sessionManager)
        initializeUI()
        
        // Agregar botón para reintentar conexión
        noConnectionView.findViewById<View>(R.id.retry_button)?.setOnClickListener {
            if (hasInternetConnection()) {
                hideNoConnection()
                refreshData()
            } else {
                Toast.makeText(this, "Still no internet connection", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe the connectivity status from ViewModel
        viewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                if (wasOfflineBefore) {
                    // If we're coming back online, show a message
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Internet connection restored",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Refresh data
                    refreshData()
                    hideNoConnection()
                }
                wasOfflineBefore = false
            } else {
                wasOfflineBefore = true
                
                // If we have content already loaded, show a snackbar
                // Otherwise, show the full offline view
                if (viewModel.buildings.value.isNullOrEmpty() && viewModel.events.value.isNullOrEmpty()) {
                    showNoConnection()
                } else {
                    // Still show a warning but don't hide the content
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "You're offline. Showing cached data.",
                        Snackbar.LENGTH_LONG
                    ).setAction("Retry") {
                        viewModel.checkConnectivity()
                    }.show()
                }
            }
        }
    }
    
    private fun refreshData() {
        viewModel.loadBuildings()
        viewModel.loadEvents()
    }

    private fun showNoConnection() {
        noConnectionView.visibility = View.VISIBLE
        nestedScrollView.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
    }
    
    private fun hideNoConnection() {
        noConnectionView.visibility = View.GONE
        // Muestra el contenido que estaba visible anteriormente
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            fragmentContainer.visibility = View.VISIBLE
        } else {
            nestedScrollView.visibility = View.VISIBLE
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupViewModelObservers() {
        viewModel.user.observe(this) { user ->
            // Save all user info to SessionManager
            sessionManager.saveUserInfo(user.id, user.email, user.username)
            
            // Update UI with fresh user data
            val greetingText: TextView = findViewById(R.id.greeting_text)
            greetingText.text = "Hola, ${user.username}"
            
            // Save profile image URL if available
            user.profileImageUrl?.let { imageUrl ->
                sessionManager.saveProfilePictureUrl(imageUrl)
                
                // Update profile image
                val profileImage: ImageView? = findViewById(R.id.profile_image)
                profileImage?.let { imageView ->
                    com.bumptech.glide.Glide.with(this@HomeActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .circleCrop()
                        .into(imageView)
                }
            }
            
            android.util.Log.d("HomeActivity", "User data updated: ${user.username}")
        }

        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                // Check if it's a connectivity error
                if (it.contains("internet") || it.contains("connection") || it.contains("network")) {
                    // Handle as connectivity issue
                    if (viewModel.buildings.value.isNullOrEmpty() && viewModel.events.value.isNullOrEmpty()) {
                        showNoConnection()
                    } else {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            it,
                            Snackbar.LENGTH_LONG
                        ).setAction("Retry") {
                            viewModel.checkConnectivity()
                        }.show()
                    }
                } else if (it.contains("401") || it.contains("no encontr")) {
                    // Handle authentication errors
                    sessionManager.logout()
                    navigateToLogin()
                } else {
                    // Other errors
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.buildings.observe(this) { buildings ->
            buildingAdapter.updateData(buildings)
        }

        viewModel.events.observe(this) { events ->
            Log.d("HomeActivity", "Events LiveData updated: received ${events?.size ?: 0} events")
            eventAdapter.submitList(events)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // If loading and we have no data yet, show loading indicator
            if (isLoading && viewModel.buildings.value?.isEmpty() == true) {
                // You could add a loading indicator here if needed
            }
        }
        
        // Observe connectivity status
        viewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                if (wasOfflineBefore) {
                    // If we're coming back online, show a message
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Internet connection restored",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Refresh data
                    refreshData()
                    hideNoConnection()
                }
                wasOfflineBefore = false
            } else {
                wasOfflineBefore = true
                
                // If we have content already loaded, show a snackbar
                // Otherwise, show the full offline view
                if (viewModel.buildings.value.isNullOrEmpty() && viewModel.events.value.isNullOrEmpty()) {
                    showNoConnection()
                } else {
                    // Still show a warning but don't hide the content
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "You're offline. Showing cached data.",
                        Snackbar.LENGTH_LONG
                    ).setAction("Retry") {
                        viewModel.checkConnectivity()
                    }.show()
                }
            }
        }
    }

    private fun initializeUI() {
        val userName = sessionManager.getCachedUserName()
        findViewById<TextView>(R.id.greeting_text).text = "Hola, $userName"
        
        val profileImageView = findViewById<ImageView>(R.id.profile_image)
        val profileImageUrl = sessionManager.getCachedProfilePicUrl()
        
        if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .circleCrop()
                .into(profileImageView)
        }
        
        setupCategoryIcons()
        setupRecyclerViews()
        setupBottomNavigation()
        setupClickListeners()

        loadUserDataIfConnected()
        viewModel.loadBuildings()
        viewModel.loadEvents()
        
        if (intent.getBooleanExtra("OPEN_NAVIGATION", false)) {
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.navigation_navigate
        }
    }

    private fun loadUserDataIfConnected() {
        if (hasInternetConnection()) {
            viewModel.loadUserData(sessionManager)
        }
    }

    private fun setupCategoryIcons() {
        val categoryAll = findViewById<View>(R.id.category_all)
        val categoryBuildings = findViewById<View>(R.id.category_buildings)
        val categoryEvents = findViewById<View>(R.id.category_events)
        val categoryFood = findViewById<View>(R.id.category_food)
        val categoryStudy = findViewById<View>(R.id.category_study)
        val categoryServices = findViewById<View>(R.id.category_services)

        categoryAll.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_home)
        categoryAll.findViewById<TextView>(R.id.category_name).text = getString(R.string.all)

        categoryBuildings.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_building)
        categoryBuildings.findViewById<TextView>(R.id.category_name).text = getString(R.string.buildings)

        categoryEvents.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_event)
        categoryEvents.findViewById<TextView>(R.id.category_name).text = getString(R.string.events)

        categoryFood.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_food)
        categoryFood.findViewById<TextView>(R.id.category_name).text = getString(R.string.food_rest)

        categoryStudy.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_study)
        categoryStudy.findViewById<TextView>(R.id.category_name).text = getString(R.string.study_spaces)

        categoryServices.findViewById<ImageView>(R.id.category_icon).setImageResource(R.drawable.ic_services)
        categoryServices.findViewById<TextView>(R.id.category_name).text = getString(R.string.services)

        categoryEvents.visibility = View.GONE
        categoryStudy.visibility = View.GONE

        categoryAll.setOnClickListener { viewModel.loadBuildings() }
        categoryBuildings.setOnClickListener { viewModel.loadBuildingsByCategory("Buildings") }
        categoryFood.setOnClickListener { viewModel.loadBuildingsByCategory("Food") }
        categoryServices.setOnClickListener { viewModel.loadBuildingsByCategory("Services") }
    }

    private fun setupRecyclerViews() {
        buildingsRecyclerView = findViewById(R.id.buildings_recycler)
        buildingsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        buildingAdapter = BuildingAdapter(emptyList()) { building ->
            val intent = Intent(this, BuildingDetailActivity::class.java)
            intent.putExtra("BUILDING", building)
            startActivity(intent)
        }
        buildingsRecyclerView.adapter = buildingAdapter

        eventsRecyclerView = findViewById(R.id.events_recycler)
        eventsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        eventAdapter = EventAdapter { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("EVENT", event)
            startActivity(intent)
        }
        eventsRecyclerView.adapter = eventAdapter
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { showHomeContent(); true }
                R.id.navigation_favorites -> { Toast.makeText(this, getString(R.string.favorites), Toast.LENGTH_SHORT).show(); true }
                R.id.navigation_navigate -> { startActivity(Intent(this, MapActivity::class.java)); true }
                R.id.navigation_view -> { 
                    // Show events list when clicking "View"
                    nestedScrollView.visibility = View.GONE
                    fragmentContainer.visibility = View.VISIBLE
                    
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, EventListFragment.newInstance())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.navigation_account -> { loadFragment(com.example.explorandes.ui.account.AccountFragment()); true }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.see_all_buildings).setOnClickListener {
            nestedScrollView.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
    
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BuildingsListFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }


    }

    private fun showHomeContent() {
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        nestedScrollView.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        noConnectionView.visibility = View.GONE
    }

    private fun loadFragment(fragment: Fragment) {
        nestedScrollView.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    // Override onResume to check connectivity
    override fun onResume() {
        super.onResume()
        // Check connectivity when activity returns to foreground
        viewModel.checkConnectivity()
        
        // If we were offline but now have connection, refresh data and update UI
        if (wasOfflineBefore && hasInternetConnection()) {
            hideNoConnection()
            refreshData()
            wasOfflineBefore = false
            
            Snackbar.make(
                findViewById(android.R.id.content),
                "Internet connection restored",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
    
    // Handle back button press for fragments
    override fun onBackPressed() {
        if (fragmentContainer.visibility == View.VISIBLE) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                // Show home content if there are no more fragments in back stack
                showHomeContent()
            }
        } else {
            super.onBackPressed()
        }
    }
}