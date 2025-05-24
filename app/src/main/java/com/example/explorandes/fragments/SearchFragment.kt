package com.example.explorandes.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
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
import com.example.explorandes.viewmodels.HomeViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator

class SearchFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var onlyAvailableCheckbox: CheckBox
    private lateinit var applyFiltersButton: Button

    // Category buttons
    private lateinit var btnTodo: Button
    private lateinit var btnEdificios: Button
    private lateinit var btnEventos: Button

    // Adapters
    private lateinit var buildingAdapter: BuildingAdapter
    private lateinit var eventAdapter: EventAdapter

    // Current search state
    private var currentCategory = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        initializeViewModel()
        setupRecyclerView()
        setupSearch()
        setupCategoryButtons()
        setupFilters()
        observeViewModel()
        loadInitialData()
    }

    private fun initializeViews(view: View) {
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        onlyAvailableCheckbox = view.findViewById(R.id.only_available_checkbox)
        applyFiltersButton = view.findViewById(R.id.apply_filters_button)

        hideUnwantedViews(view)

        btnTodo = findButtonSafely(view, listOf("btn_todo", "button_todo", "todo_button"))
        btnEdificios = findButtonSafely(view, listOf("btn_edificios", "btn_buildings", "button_buildings"))
        btnEventos = findButtonSafely(view, listOf("btn_eventos", "btn_events", "button_events"))

        setupFilterButtons(view)
        debugLogAllButtons(view)
    }

    private fun hideUnwantedViews(view: View) {
        val elementsToHide = listOf(
            "search_view", "search_bar", "searchView", "buscador",
            "location_filter", "ubicacion_filter", "spinner_location", "dropdown_ubicacion",
            "date_filter_button", "date_selector", "seleccionar_fecha",
            "btn_date", "button_date",
            "filtros_adicionales", "additional_filters", "filter_container",
            "filtros_container", "additional_filters_container",
            "spinner", "dropdown", "autocomplete", "textinput"
        )

        elementsToHide.forEach { idName ->
            try {
                val resourceId = resources.getIdentifier(idName, "id", requireContext().packageName)
                if (resourceId != 0) {
                    val element = view.findViewById<View>(resourceId)
                    element?.visibility = View.GONE
                    Log.d("SearchFragment", "Hidden element: $idName")
                }
            } catch (e: Exception) {
                // Element doesn't exist, ignore
            }
        }

        hideFilterSection(view)

        try {
            val rootView = view as? ViewGroup
            rootView?.let { hideViewsByContent(it) }
        } catch (e: Exception) {
            Log.w("SearchFragment", "Could not hide views by content")
        }

        hideLugaresButton(view)
    }

    private fun hideFilterSection(view: View) {
        try {
            val rootView = view as? ViewGroup
            rootView?.let { hideFilterSectionRecursive(it) }
        } catch (e: Exception) {
            Log.w("SearchFragment", "Could not hide filter section")
        }
    }

    private fun hideFilterSectionRecursive(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            val shouldHideSection = when {
                child is android.widget.TextView &&
                        child.text?.contains("Filtros adicionales", ignoreCase = true) == true -> {
                    (child.parent as? View)?.visibility = View.GONE
                    Log.d("SearchFragment", "Hidden 'Filtros adicionales' section")
                    true
                }
                child is ViewGroup && hasFilterContent(child) -> {
                    child.visibility = View.GONE
                    Log.d("SearchFragment", "Hidden filter container")
                    true
                }
                else -> false
            }

            if (!shouldHideSection && child is ViewGroup) {
                hideFilterSectionRecursive(child)
            }
        }
    }

    private fun hasFilterContent(viewGroup: ViewGroup): Boolean {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when {
                child is android.widget.TextView &&
                        child.text?.contains("Filtros", ignoreCase = true) == true -> return true
                child is android.widget.Spinner -> return true
                child is android.widget.AutoCompleteTextView -> return true
                child is ViewGroup && hasFilterContent(child) -> return true
            }
        }
        return false
    }

    private fun hideViewsByContent(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            val shouldHide = when {
                child is androidx.appcompat.widget.SearchView -> true
                child is android.widget.SearchView -> true
                child is android.widget.Spinner -> true
                child is android.widget.AutoCompleteTextView -> true
                child.contentDescription?.contains("search", ignoreCase = true) == true -> true
                child.contentDescription?.contains("buscar", ignoreCase = true) == true -> true
                child.contentDescription?.contains("fecha", ignoreCase = true) == true -> true
                child.contentDescription?.contains("date", ignoreCase = true) == true -> true
                child.contentDescription?.contains("ubicacion", ignoreCase = true) == true -> true
                child.contentDescription?.contains("location", ignoreCase = true) == true -> true
                child.contentDescription?.contains("lugares", ignoreCase = true) == true -> true
                child.contentDescription?.contains("places", ignoreCase = true) == true -> true
                else -> false
            }

            if (shouldHide) {
                child.visibility = View.GONE
                Log.d("SearchFragment", "Hidden view by content: ${child.javaClass.simpleName}")
            }

            if (child is ViewGroup) {
                hideViewsByContent(child)
            }
        }
    }

    private fun hideLugaresButton(view: View) {
        val lugaresIds = listOf(
            "btn_lugares", "btn_places", "button_places", "lugares_button",
            "button_lugares", "lugares", "places"
        )

        lugaresIds.forEach { idName ->
            try {
                val resourceId = resources.getIdentifier(idName, "id", requireContext().packageName)
                if (resourceId != 0) {
                    val element = view.findViewById<View>(resourceId)
                    element?.visibility = View.GONE
                    Log.d("SearchFragment", "Hidden Lugares button: $idName")
                }
            } catch (e: Exception) {
                // Element doesn't exist, ignore
            }
        }
    }

    private fun findButtonSafely(view: View, possibleIds: List<String>): Button {
        for (idName in possibleIds) {
            try {
                val resourceId = resources.getIdentifier(idName, "id", requireContext().packageName)
                if (resourceId != 0) {
                    val button = view.findViewById<Button>(resourceId)
                    if (button != null) {
                        Log.d("SearchFragment", "Found button: $idName")
                        return button
                    }
                }
            } catch (e: Exception) {
                // Continue trying
            }
        }

        val foundButton = findButtonInViewGroup(view as? ViewGroup, possibleIds)
        if (foundButton != null) {
            return foundButton
        }

        Log.w("SearchFragment", "Button not found for IDs: $possibleIds, creating dummy")
        return Button(requireContext()).apply {
            visibility = View.GONE
            text = "Not Found"
        }
    }

    private fun findButtonInViewGroup(viewGroup: ViewGroup?, possibleIds: List<String>): Button? {
        if (viewGroup == null) return null

        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            when {
                child is Button -> {
                    val buttonText = child.text?.toString()?.lowercase() ?: ""
                    val buttonId = try {
                        context?.resources?.getResourceEntryName(child.id) ?: ""
                    } catch (e: Exception) { "" }

                    val matches = possibleIds.any { id ->
                        buttonId.contains(id, ignoreCase = true) ||
                                buttonText.contains(id.replace("btn_", "").replace("_", ""), ignoreCase = true)
                    }

                    if (matches) {
                        Log.d("SearchFragment", "Found button by traversal: $buttonText (ID: $buttonId)")
                        return child
                    }
                }
                child is ViewGroup -> {
                    val found = findButtonInViewGroup(child, possibleIds)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun setupFilterButtons(view: View) {
        val filterButtons = mapOf(
            "Académico" to listOf("academico", "btn_academico", "academic"),
            "Servicios" to listOf("servicios", "btn_servicios", "services"),
            "Alimentación" to listOf("alimentacion", "btn_alimentacion", "food"),
            "Estudio" to listOf("estudio", "btn_estudio", "study")
        )

        filterButtons.forEach { (filterName, possibleIds) ->
            possibleIds.forEach { idName ->
                try {
                    val resourceId = resources.getIdentifier(idName, "id", requireContext().packageName)
                    if (resourceId != 0) {
                        val button = view.findViewById<View>(resourceId)
                        button?.setOnClickListener {
                            Log.d("SearchFragment", "$filterName filter clicked")
                            applyBuildingFilter(filterName)
                        }
                        if (button != null) {
                            Log.d("SearchFragment", "Found and setup filter button: $idName for $filterName")
                            return@forEach
                        }
                    }
                } catch (e: Exception) {
                    // Continue trying
                }
            }
        }

        findButtonsByText(view)
    }

    private fun findButtonsByText(view: View) {
        if (view !is ViewGroup) return

        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)

            when {
                child is android.widget.Button -> {
                    val buttonText = child.text?.toString()?.lowercase() ?: ""
                    when {
                        buttonText.contains("académico") || buttonText.contains("academico") -> {
                            child.setOnClickListener {
                                Log.d("SearchFragment", "Académico filter clicked (by text)")
                                applyBuildingFilter("Académico")
                            }
                        }
                        buttonText.contains("servicios") -> {
                            child.setOnClickListener {
                                Log.d("SearchFragment", "Servicios filter clicked (by text)")
                                applyBuildingFilter("Servicios")
                            }
                        }
                        buttonText.contains("alimentación") || buttonText.contains("alimentacion") -> {
                            child.setOnClickListener {
                                Log.d("SearchFragment", "Alimentación filter clicked (by text)")
                                applyBuildingFilter("Alimentación")
                            }
                        }
                        buttonText.contains("estudio") -> {
                            child.setOnClickListener {
                                Log.d("SearchFragment", "Estudio filter clicked (by text)")
                                applyBuildingFilter("Estudio")
                            }
                        }
                    }
                }
                child is ViewGroup -> findButtonsByText(child)
            }
        }
    }

    private fun applyBuildingFilter(filterType: String) {
        Log.d("SearchFragment", "Applying building filter: $filterType")

        viewModel.buildings.value?.let { allBuildings ->
            val filteredBuildings = when (filterType) {
                "Académico" -> allBuildings.filter { building ->
                    building.name.contains("aula", ignoreCase = true) ||
                            building.name.contains("salón", ignoreCase = true) ||
                            building.name.contains("clase", ignoreCase = true) ||
                            building.description?.contains("académico", ignoreCase = true) == true
                }
                "Servicios" -> allBuildings.filter { building ->
                    building.name.contains("servicio", ignoreCase = true) ||
                            building.name.contains("administr", ignoreCase = true) ||
                            building.name.contains("oficina", ignoreCase = true) ||
                            building.description?.contains("servicio", ignoreCase = true) == true
                }
                "Alimentación" -> allBuildings.filter { building ->
                    building.name.contains("cafetería", ignoreCase = true) ||
                            building.name.contains("restaurante", ignoreCase = true) ||
                            building.name.contains("comida", ignoreCase = true) ||
                            building.description?.contains("alimentación", ignoreCase = true) == true ||
                            building.description?.contains("comida", ignoreCase = true) == true
                }
                "Estudio" -> allBuildings.filter { building ->
                    building.name.contains("biblioteca", ignoreCase = true) ||
                            building.name.contains("estudio", ignoreCase = true) ||
                            building.name.contains("sala", ignoreCase = true) ||
                            building.description?.contains("estudio", ignoreCase = true) == true
                }
                else -> allBuildings
            }

            Log.d("SearchFragment", "Filtered buildings for $filterType: ${filteredBuildings.size}")

            currentCategory = "BUILDINGS"
            resultsRecyclerView.adapter = buildingAdapter
            buildingAdapter.updateData(filteredBuildings)

            Toast.makeText(
                context,
                "Mostrando edificios de $filterType (${filteredBuildings.size})",
                Toast.LENGTH_SHORT
            ).show()
        } ?: run {
            viewModel.loadBuildings()
            Toast.makeText(context, "Cargando edificios...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            requireActivity(),
            HomeViewModel.Factory(requireContext())
        )[HomeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        buildingAdapter = BuildingAdapter(emptyList()) { building ->
            navigateToBuilding(building)
        }

        eventAdapter = EventAdapter { event ->
            navigateToEvent(event)
        }

        resultsRecyclerView.adapter = buildingAdapter
    }

    private fun setupSearch() {
        Log.d("SearchFragment", "Search functionality disabled - no search bar")
    }

    private fun setupCategoryButtons() {
        btnTodo.setOnClickListener {
            Log.d("SearchFragment", "TODO button clicked")
            selectCategory("ALL")
        }

        btnEdificios.setOnClickListener {
            Log.d("SearchFragment", "EDIFICIOS button clicked")
            selectCategory("BUILDINGS")
        }

        btnEventos.setOnClickListener {
            Log.d("SearchFragment", "EVENTOS button clicked")
            selectCategory("EVENTS")
        }
    }

    private fun selectCategory(category: String) {
        currentCategory = category
        Log.d("SearchFragment", "Category selected: $category")

        when (category) {
            "EVENTS" -> {
                resultsRecyclerView.adapter = eventAdapter
                Toast.makeText(context, "Mostrando Eventos", Toast.LENGTH_SHORT).show()

                val currentEvents = viewModel.events.value
                if (currentEvents.isNullOrEmpty()) {
                    Log.d("SearchFragment", "No events available, loading...")
                    viewModel.loadEvents()
                } else {
                    Log.d("SearchFragment", "Events available: ${currentEvents.size}")
                    performSearch()
                }
            }
            "BUILDINGS" -> {
                resultsRecyclerView.adapter = buildingAdapter
                Toast.makeText(context, "Mostrando Edificios", Toast.LENGTH_SHORT).show()

                val currentBuildings = viewModel.buildings.value
                if (currentBuildings.isNullOrEmpty()) {
                    Log.d("SearchFragment", "No buildings available, loading...")
                    viewModel.loadBuildings()
                } else {
                    Log.d("SearchFragment", "Buildings available: ${currentBuildings.size}")
                    buildingAdapter.updateData(currentBuildings)
                }
            }
            "ALL" -> {
                resultsRecyclerView.adapter = buildingAdapter
                Toast.makeText(context, "Mostrando Todo", Toast.LENGTH_SHORT).show()

                viewModel.loadBuildings()
                viewModel.loadEvents()

                val currentBuildings = viewModel.buildings.value ?: emptyList()
                val currentEvents = viewModel.events.value ?: emptyList()

                Log.d("SearchFragment", "ALL mode - Buildings: ${currentBuildings.size}, Events: ${currentEvents.size}")

                if (currentBuildings.isNotEmpty()) {
                    buildingAdapter.updateData(currentBuildings)

                    view?.postDelayed({
                        val message = "Mostrando ${currentBuildings.size} edificios" +
                                if (currentEvents.isNotEmpty()) " y ${currentEvents.size} eventos disponibles" else ""
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }, 500)
                } else {
                    Toast.makeText(context, "Cargando datos...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFilters() {
        applyFiltersButton.setOnClickListener {
            Log.d("SearchFragment", "Apply filters clicked")

            val isChecked = onlyAvailableCheckbox.isChecked
            Log.d("SearchFragment", "Checkbox state: $isChecked, Category: $currentCategory")

            when (currentCategory) {
                "EVENTS" -> {
                    performSearch()
                    val message = if (isChecked) {
                        "Filtrando eventos pasados"
                    } else {
                        "Mostrando todos los eventos"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                "BUILDINGS" -> {
                    val currentBuildings = viewModel.buildings.value
                    if (!currentBuildings.isNullOrEmpty()) {
                        buildingAdapter.updateData(currentBuildings)
                        Toast.makeText(context, "Mostrando todos los edificios", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.loadBuildings()
                        Toast.makeText(context, "Cargando edificios...", Toast.LENGTH_SHORT).show()
                    }
                }
                "ALL" -> {
                    viewModel.loadBuildings()
                    viewModel.loadEvents()
                    val currentBuildings = viewModel.buildings.value ?: emptyList()
                    if (currentBuildings.isNotEmpty()) {
                        buildingAdapter.updateData(currentBuildings)
                    }
                    Toast.makeText(context, "Mostrando todos los resultados", Toast.LENGTH_SHORT).show()
                }
            }
        }

        onlyAvailableCheckbox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("SearchFragment", "Checkbox changed: $isChecked")

            if (currentCategory == "EVENTS") {
                performSearch()

                val message = if (isChecked) {
                    "Filtrando eventos no disponibles"
                } else {
                    "Mostrando todos los eventos"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadInitialData() {
        Log.d("SearchFragment", "Loading initial data")

        viewModel.loadBuildings()
        viewModel.loadEvents()

        viewModel.buildings.value?.let { buildings ->
            Log.d("SearchFragment", "Buildings already available: ${buildings.size}")
            if (currentCategory != "EVENTS") {
                buildingAdapter.updateData(buildings)
            }
        }

        viewModel.events.value?.let { events ->
            Log.d("SearchFragment", "Events already available: ${events.size}")
            if (currentCategory == "EVENTS") {
                eventAdapter.submitList(events)
            }
        }
    }

    private fun performSearch() {
        Log.d("SearchFragment", "Performing search - Category: $currentCategory")

        when (currentCategory) {
            "EVENTS" -> searchEvents()
            "BUILDINGS" -> searchBuildings()
            "ALL" -> searchBuildings()
        }
    }

    private fun searchBuildings() {
        viewModel.buildings.value?.let { allBuildings ->
            Log.d("SearchFragment", "Showing all ${allBuildings.size} buildings")
            buildingAdapter.updateData(allBuildings)
        }
    }

    private fun searchEvents() {
        viewModel.events.value?.let { allEvents ->
            val onlyAvailable = onlyAvailableCheckbox.isChecked

            Log.d("SearchFragment", "Searching events - Total: ${allEvents.size}, OnlyAvailable: $onlyAvailable")

            val filteredEvents = if (onlyAvailable) {
                allEvents.filter { event ->
                    val isPast = isPastEvent(event)
                    Log.d("SearchFragment", "Event '${event.title}' - isPast: $isPast")
                    !isPast
                }
            } else {
                allEvents
            }

            Log.d("SearchFragment", "Found ${filteredEvents.size} events after filtering")
            eventAdapter.submitList(filteredEvents)

            if (filteredEvents.isEmpty() && allEvents.isNotEmpty()) {
                val message = if (onlyAvailable) {
                    "No available events found (all events are past)"
                } else {
                    "No events to display"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.w("SearchFragment", "No events data available")
            Toast.makeText(context, "Loading events...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPastEvent(event: Event): Boolean {
        return try {
            val now = java.time.LocalDateTime.now()
            val eventEnd = java.time.LocalDateTime.parse(
                event.endTime,
                java.time.format.DateTimeFormatter.ISO_DATE_TIME
            )
            eventEnd.isBefore(now)
        } catch (e: Exception) {
            false
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d("SearchFragment", "Loading state: $isLoading")
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("SearchFragment", "Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.buildings.observe(viewLifecycleOwner) { buildings ->
            Log.d("SearchFragment", "Buildings data received: ${buildings?.size ?: 0} items")
            if (!buildings.isNullOrEmpty()) {
                when (currentCategory) {
                    "BUILDINGS" -> buildingAdapter.updateData(buildings)
                    "ALL" -> {
                        buildingAdapter.updateData(buildings)
                        val eventCount = viewModel.events.value?.size ?: 0
                        view?.postDelayed({
                            Toast.makeText(
                                context,
                                "Edificios: ${buildings.size}, Eventos: $eventCount",
                                Toast.LENGTH_SHORT
                            ).show()
                        }, 300)
                    }
                }
            }
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            Log.d("SearchFragment", "Events data received: ${events?.size ?: 0} items")
            if (!events.isNullOrEmpty()) {
                when (currentCategory) {
                    "EVENTS" -> performSearch()
                    "ALL" -> {
                        val buildingCount = viewModel.buildings.value?.size ?: 0
                        view?.postDelayed({
                            Toast.makeText(
                                context,
                                "Edificios: $buildingCount, Eventos: ${events.size}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }, 300)
                    }
                }
            }
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

    private fun debugLogAllButtons(view: View) {
        Log.d("SearchFragment", "=== DEBUG: Finding all buttons in layout ===")
        if (view is ViewGroup) {
            findAllButtons(view, 0)
        }
    }

    private fun findAllButtons(viewGroup: ViewGroup?, depth: Int) {
        if (viewGroup == null) return

        val indent = "  ".repeat(depth)

        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            when {
                child is Button -> {
                    val buttonText = child.text?.toString() ?: "No text"
                    val buttonId = try {
                        context?.resources?.getResourceEntryName(child.id) ?: "no_id"
                    } catch (e: Exception) { "no_id" }

                    Log.d("SearchFragment", "${indent}BUTTON: '$buttonText' (ID: $buttonId)")
                }
                child is android.widget.TextView -> {
                    val text = child.text?.toString() ?: ""
                    if (text.isNotEmpty()) {
                        val textId = try {
                            context?.resources?.getResourceEntryName(child.id) ?: "no_id"
                        } catch (e: Exception) { "no_id" }
                        Log.d("SearchFragment", "${indent}TextView: '$text' (ID: $textId)")
                    }
                }
                child is ViewGroup -> {
                    Log.d("SearchFragment", "${indent}ViewGroup: ${child.javaClass.simpleName}")
                    findAllButtons(child, depth + 1)
                }
            }
        }
    }

    companion object {
        fun newInstance() = SearchFragment()
    }
}