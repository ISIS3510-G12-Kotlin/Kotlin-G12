package com.example.explorandes.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.BuildingDetailActivity
import com.example.explorandes.EventDetailActivity
import com.example.explorandes.R
import com.example.explorandes.adapters.BuildingAdapter
import com.example.explorandes.adapters.EventAdapter
import com.example.explorandes.adapters.PlaceAdapter
import com.example.explorandes.models.Building
import com.example.explorandes.models.Event
import com.example.explorandes.models.Place
import com.example.explorandes.viewmodels.HomeViewModel
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.Locale

class SearchFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var searchView: SearchView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var searchTypeToggle: MaterialButtonToggleGroup
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var dateFilterButton: Button
    private lateinit var locationFilter: AutoCompleteTextView
    private lateinit var onlyAvailableCheckbox: CheckBox
    private lateinit var applyFiltersButton: Button
    
    // Adapters
    private lateinit var buildingAdapter: BuildingAdapter
    private lateinit var placeAdapter: PlaceAdapter
    private lateinit var eventAdapter: EventAdapter
    
    // Current search state
    private var currentSearchType = SearchType.ALL
    private var currentQuery = ""
    
    enum class SearchType {
        ALL, BUILDINGS, PLACES, EVENTS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        
        // Initialize views
        searchView = view.findViewById(R.id.search_view)
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        searchTypeToggle = view.findViewById(R.id.search_type_toggle)
        filterChipGroup = view.findViewById(R.id.filter_chip_group)
        dateFilterButton = view.findViewById(R.id.date_filter_button)
        locationFilter = view.findViewById(R.id.location_filter)
        onlyAvailableCheckbox = view.findViewById(R.id.only_available_checkbox)
        applyFiltersButton = view.findViewById(R.id.apply_filters_button)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(
            requireActivity(),
            HomeViewModel.Factory(requireContext())
        )[HomeViewModel::class.java]
        
        // Setup recycler view
        setupRecyclerView()
        
        // Setup search functionality
        setupSearch()
        
        // Setup filter options
        setupFilters()
        
        // Setup observers
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Initialize adapters
        buildingAdapter = BuildingAdapter(emptyList()) { building ->
            navigateToBuilding(building)
        }
        
        placeAdapter = PlaceAdapter(emptyList()) { place ->
            Toast.makeText(context, "Seleccionado: ${place.name}", Toast.LENGTH_SHORT).show()
        }
        
        eventAdapter = EventAdapter { event ->
            navigateToEvent(event)
        }
        
        // Default adapter
        resultsRecyclerView.adapter = buildingAdapter
    }
    
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    currentQuery = query
                    performSearch(query)
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    // Clear results
                    when (currentSearchType) {
                        SearchType.BUILDINGS, SearchType.ALL -> buildingAdapter.updateData(emptyList())
                        SearchType.PLACES -> placeAdapter.updateData(emptyList())
                        SearchType.EVENTS -> eventAdapter.submitList(emptyList())
                    }
                }
                return true
            }
        })
        
        // Setup search type toggle
        searchTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentSearchType = when (checkedId) {
                    R.id.btn_buildings -> SearchType.BUILDINGS
                    R.id.btn_places -> SearchType.PLACES
                    R.id.btn_events -> SearchType.EVENTS
                    else -> SearchType.ALL
                }
                
                // Change adapter based on search type
                when (currentSearchType) {
                    SearchType.BUILDINGS, SearchType.ALL -> resultsRecyclerView.adapter = buildingAdapter
                    SearchType.PLACES -> resultsRecyclerView.adapter = placeAdapter
                    SearchType.EVENTS -> resultsRecyclerView.adapter = eventAdapter
                }
                
                // Re-perform search if there's a query
                if (currentQuery.isNotBlank()) {
                    performSearch(currentQuery)
                }
            }
        }
    }
    
    private fun setupFilters() {
        // Setup location filter with building names
        viewModel.buildings.observe(viewLifecycleOwner) { buildings ->
            val buildingNames = buildings.map { it.name }.toTypedArray()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, buildingNames)
            locationFilter.setAdapter(adapter)
        }
        
        // Date filter button
        dateFilterButton.setOnClickListener {
            // Show date picker dialog
            // For simplicity, we'll just show a toast
            Toast.makeText(context, "Fecha seleccionada", Toast.LENGTH_SHORT).show()
        }
        
        // Apply filters button
        applyFiltersButton.setOnClickListener {
            applyFilters()
        }
    }
    
    private fun applyFilters() {
        // Apply filters based on the selected options
        // For simplicity, we'll just show a toast with the selected filter options
        val selectedLocation = locationFilter.text.toString()
        val onlyAvailable = onlyAvailableCheckbox.isChecked
        
        val filtersText = """
            Búsqueda: "$currentQuery"
            Tipo: ${currentSearchType.name}
            Ubicación: ${if (selectedLocation.isNotBlank()) selectedLocation else "Cualquiera"}
            Solo disponibles: $onlyAvailable
        """.trimIndent()
        
        Toast.makeText(context, "Filtros aplicados:\n$filtersText", Toast.LENGTH_SHORT).show()
        
        // Re-perform search with filters
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery)
        }
    }
    
    private fun performSearch(query: String) {
        progressIndicator.visibility = View.VISIBLE
        
        when (currentSearchType) {
            SearchType.BUILDINGS, SearchType.ALL -> {
                viewModel.buildings.observe(viewLifecycleOwner) { allBuildings ->
                    // Filter buildings by query
                    val filteredBuildings = allBuildings.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.code.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
                    }
                    buildingAdapter.updateData(filteredBuildings)
                    progressIndicator.visibility = View.GONE
                }
                viewModel.loadBuildings()
            }
            SearchType.PLACES -> {
                // We don't have a direct method to load places in the ViewModel
                // For simplicity, we'll filter from buildings' places
                viewModel.buildings.observe(viewLifecycleOwner) { allBuildings ->
                    val allPlaces = allBuildings.flatMap { it.places ?: emptyList() }
                    val filteredPlaces = allPlaces.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.code?.contains(query, ignoreCase = true) == true
                    }
                    placeAdapter.updateData(filteredPlaces)
                    progressIndicator.visibility = View.GONE
                }
                viewModel.loadBuildings()
            }
            SearchType.EVENTS -> {
                viewModel.events.observe(viewLifecycleOwner) { allEvents ->
                    val filteredEvents = allEvents.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true ||
                        it.locationName?.contains(query, ignoreCase = true) == true
                    }
                    eventAdapter.submitList(filteredEvents)
                    progressIndicator.visibility = View.GONE
                }
                viewModel.loadEvents()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
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
        fun newInstance() = SearchFragment()
    }
}