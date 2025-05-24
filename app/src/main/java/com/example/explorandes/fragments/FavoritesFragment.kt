package com.example.explorandes.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.BuildingDetailActivity
import com.example.explorandes.EventDetailActivity
import com.example.explorandes.R
import com.example.explorandes.adapters.BuildingAdapter
import com.example.explorandes.adapters.EventAdapter
import com.example.explorandes.models.Building
import com.example.explorandes.models.Event
import com.example.explorandes.utils.DataStoreManager
import com.example.explorandes.viewmodels.FavoritesViewModel
import com.example.explorandes.viewmodels.HomeViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.tabs.TabLayout

class FavoritesFragment : Fragment() {

    private lateinit var viewModel: FavoritesViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var dataStoreManager: DataStoreManager
    
    private lateinit var buildingAdapter: BuildingAdapter
    private lateinit var eventAdapter: EventAdapter
    
    private val favoriteBuildings = mutableListOf<Building>()
    private val favoriteEvents = mutableListOf<Event>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        
        // Initialize views
        tabLayout = view.findViewById(R.id.tab_layout)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.empty_view)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModels
        homeViewModel = ViewModelProvider(
            requireActivity(),
            HomeViewModel.Factory(requireContext())
        )[HomeViewModel::class.java]
        
        viewModel = ViewModelProvider(this)[FavoritesViewModel::class.java]
        
        // Initialize DataStoreManager
        dataStoreManager = DataStoreManager(requireContext())
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup TabLayout
        setupTabLayout()
        
        // Load data
        loadData()
        
        // Observe ViewModel
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Initialize adapters
        buildingAdapter = BuildingAdapter(emptyList()) { building ->
            navigateToBuilding(building)
        }
        
        eventAdapter = EventAdapter { event ->
            navigateToEvent(event)
        }
        
        // Set default adapter
        recyclerView.adapter = buildingAdapter
    }
    
    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Edificios"))
        tabLayout.addTab(tabLayout.newTab().setText("Eventos"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Buildings tab
                        recyclerView.adapter = buildingAdapter
                        updateEmptyViewVisibility(favoriteBuildings.isEmpty())
                    }
                    1 -> {
                        // Events tab
                        recyclerView.adapter = eventAdapter
                        updateEmptyViewVisibility(favoriteEvents.isEmpty())
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadData() {
        progressIndicator.visibility = View.VISIBLE
        
        // Load buildings and events data
        homeViewModel.buildings.observe(viewLifecycleOwner) { buildings ->
            // For this example, we'll simulate favorites by selecting a few buildings
            // In a real app, you would load favorites from DataStore or database
            favoriteBuildings.clear()
            if (buildings.isNotEmpty()) {
                favoriteBuildings.addAll(buildings.take(3))
            }
            
            buildingAdapter.updateData(favoriteBuildings)
            
            if (tabLayout.selectedTabPosition == 0) {
                updateEmptyViewVisibility(favoriteBuildings.isEmpty())
            }
            
            progressIndicator.visibility = View.GONE
        }
        
        homeViewModel.events.observe(viewLifecycleOwner) { events ->
            // For this example, we'll simulate favorites by selecting a few events
            favoriteEvents.clear()
            if (events.isNotEmpty()) {
                favoriteEvents.addAll(events.take(2))
            }
            
            eventAdapter.submitList(favoriteEvents)
            
            if (tabLayout.selectedTabPosition == 1) {
                updateEmptyViewVisibility(favoriteEvents.isEmpty())
            }
            
            progressIndicator.visibility = View.GONE
        }
        
        // Load data
        homeViewModel.loadBuildings()
        homeViewModel.loadEvents()
    }
    
    private fun observeViewModel() {
        // Observe loading state
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun updateEmptyViewVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            
            // Update empty message based on selected tab
            when (tabLayout.selectedTabPosition) {
                0 -> emptyView.text = "No tienes edificios favoritos"
                1 -> emptyView.text = "No tienes eventos favoritos"
            }
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun navigateToBuilding(building: Building) {
        val intent = Intent(requireContext(), BuildingDetailActivity::class.java)
        intent.putExtra("BUILDING", building)
        startActivity(intent)
    }
    
    private fun navigateToEvent(event: Event) {
        val intent = Intent(requireContext(), EventDetailActivity::class.java)
        intent.putExtra("EVENT", event)
        startActivity(intent)
    }
    
    companion object {
        fun newInstance() = FavoritesFragment()
    }
}