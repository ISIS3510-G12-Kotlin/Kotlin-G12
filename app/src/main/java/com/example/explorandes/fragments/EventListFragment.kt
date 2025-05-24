package com.example.explorandes.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.explorandes.EventDetailActivity
import com.example.explorandes.R
import com.example.explorandes.adapters.EventAdapter
import com.example.explorandes.api.ApiClient
import com.example.explorandes.databinding.FragmentEventListBinding
import com.example.explorandes.models.Event
import com.example.explorandes.repositories.EventRepository
import com.example.explorandes.utils.NetworkResult
import com.example.explorandes.viewmodels.EventViewModel
import com.google.android.material.snackbar.Snackbar

class EventListFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EventViewModel
    private lateinit var eventAdapter: EventAdapter
    private lateinit var noConnectionView: View
    private var wasOfflineBefore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventRepository = EventRepository(ApiClient.apiService, requireContext())
        val factory = EventViewModel.Factory(eventRepository)
        viewModel = ViewModelProvider(this, factory)[EventViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rootView = binding.root
        val inflatedOfflineView = layoutInflater.inflate(R.layout.layout_no_connection, null)
        inflatedOfflineView.visibility = View.GONE
        inflatedOfflineView.findViewById<Button>(R.id.retry_button)?.setOnClickListener {
            val connected = viewModel.checkConnectivity()
            if (connected) {
                wasOfflineBefore = false
                showContent()
                viewModel.loadEvents()
            } else {
                Toast.makeText(context, "Still no internet connection", Toast.LENGTH_SHORT).show()
            }
        }
        noConnectionView = inflatedOfflineView
        (rootView as? ViewGroup)?.addView(inflatedOfflineView)

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupSearchView()
        setupRefreshLayout()
        observeViewModel()

        // Botón para abrir historial
        binding.btnOpenVisited.setOnClickListener {
            findNavController().navigate(R.id.action_eventListFragment_to_visitedFragment)
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                if (wasOfflineBefore) {
                    Snackbar.make(requireView(), "Internet connection restored", Snackbar.LENGTH_SHORT).show()
                    viewModel.loadEvents()
                    showContent()
                }
                wasOfflineBefore = false
            } else {
                wasOfflineBefore = true
                if (eventAdapter.itemCount > 0) {
                    Snackbar.make(requireView(), "You're offline. Showing cached events.", Snackbar.LENGTH_LONG)
                        .setAction("Retry") {
                            viewModel.checkConnectivity()
                        }.show()
                } else {
                    showOfflineView()
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            navigateToEventDetail(event)
        }
        binding.recyclerViewEvents.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupFilterChips() {
        binding.apply {
            chipAll.setOnClickListener { viewModel.applyFilter(null) }
            chipEvents.setOnClickListener { viewModel.applyFilter(Event.TYPE_EVENT) }
            chipMovies.setOnClickListener { viewModel.applyFilter(Event.TYPE_MOVIE) }
            chipSports.setOnClickListener { viewModel.applyFilter(Event.TYPE_SPORTS) }
            chipUpcoming.setOnClickListener { viewModel.applyFilter("upcoming") }
            chipOngoing.setOnClickListener { viewModel.applyFilter("ongoing") }
            viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
                updateChipSelection(filter)
            }
        }
    }

    private fun updateChipSelection(filter: String?) {
        binding.apply {
            val chips = listOf(chipAll, chipEvents, chipMovies, chipSports, chipUpcoming, chipOngoing)
            chips.forEach { it.isChecked = false }
            when (filter) {
                null -> chipAll.isChecked = true
                Event.TYPE_EVENT -> chipEvents.isChecked = true
                Event.TYPE_MOVIE -> chipMovies.isChecked = true
                Event.TYPE_SPORTS -> chipSports.isChecked = true
                "upcoming" -> chipUpcoming.isChecked = true
                "ongoing" -> chipOngoing.isChecked = true
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.takeIf { it.isNotBlank() }?.let {
                    viewModel.searchEvents(it)
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) viewModel.loadEvents()
                return true
            }
        })
        binding.searchView.setOnCloseListener {
            viewModel.loadEvents()
            false
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadEvents()
        }
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    val events = result.data ?: emptyList()
                    eventAdapter.submitList(events)
                    if (events.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerViewEvents.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerViewEvents.visibility = View.VISIBLE
                    }
                }
                is NetworkResult.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    if (result.message?.contains("internet") == true ||
                        result.message?.contains("network") == true ||
                        result.message?.contains("connection") == true) {
                        if (eventAdapter.itemCount == 0) {
                            showOfflineView()
                        } else {
                            Snackbar.make(requireView(), "You're offline. Showing cached events.", Snackbar.LENGTH_LONG)
                                .setAction("Retry") { viewModel.checkConnectivity() }.show()
                        }
                    } else {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is NetworkResult.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun navigateToEventDetail(event: Event) {
        try {
            viewModel.setSelectedEvent(event)
            if (findNavController().currentDestination?.id == R.id.eventListFragment) {
                findNavController().navigate(R.id.action_eventListFragment_to_eventDetailFragment)
            } else {
                val intent = Intent(requireContext(), EventDetailActivity::class.java)
                intent.putExtra("EVENT", event)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error navigating to event details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOfflineView() {
        binding.swipeRefreshLayout.visibility = View.GONE
        noConnectionView.visibility = View.VISIBLE
    }

    private fun showContent() {
        binding.swipeRefreshLayout.visibility = View.VISIBLE
        noConnectionView.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkConnectivity()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EventListFragment()
    }
}
