package com.example.explorandes.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.explorandes.R
import com.example.explorandes.databinding.FragmentEventDetailBinding
import com.example.explorandes.models.EventDetail
import com.example.explorandes.utils.NetworkResult
import com.example.explorandes.utils.ConnectivityHelper
import com.example.explorandes.utils.VisitedEventsManager
import com.example.explorandes.viewmodels.EventDetailViewModel
import com.example.explorandes.viewmodels.EventViewModel

import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EventDetailViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var eventId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { eventId = it.getLong("eventId", -1) }

        viewModel = ViewModelProvider(
            this,
            EventDetailViewModel.Factory(requireContext())
        )[EventDetailViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        val sharedViewModel = ViewModelProvider(requireActivity())[EventViewModel::class.java]
        sharedViewModel.selectedEvent.value?.let {
            eventId = it.id
        }
        if (eventId != -1L) viewModel.loadEventDetail(eventId)
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if (eventId != -1L) {
                viewModel.loadEventDetail(eventId)
            }
        }

        binding.fabShare.setOnClickListener {
            val eventDetail = (viewModel.eventDetail.value as? NetworkResult.Success)?.data
            eventDetail?.let { shareEvent(it) }
        }

        binding.fabAddToCalendar.setOnClickListener {
            val eventDetail = (viewModel.eventDetail.value as? NetworkResult.Success)?.data
            eventDetail?.let { addEventToCalendar(it) }
        }

        binding.locationLayout.setOnClickListener {
            val eventDetail = (viewModel.eventDetail.value as? NetworkResult.Success)?.data
            eventDetail?.locationId?.let { navigateToLocation(it) }
        }
    }

    private fun setupObservers() {
        viewModel.eventDetail.observe(viewLifecycleOwner) { result ->
            swipeRefreshLayout.isRefreshing = false
            when (result) {
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    result.data?.let { updateUI(it) }
                }
                is NetworkResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    result.data?.let {
                        updateUI(it)
                        Snackbar.make(binding.root, "Error al actualizar: ${result.message}", Snackbar.LENGTH_LONG).show()
                    } ?: showErrorView(result.message ?: "Error desconocido")
                }
                is NetworkResult.Loading -> binding.progressBar.visibility = View.VISIBLE
            }
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (!isConnected) {
                Snackbar.make(
                    binding.root,
                    "Sin conexión. Mostrando datos almacenados localmente.",
                    Snackbar.LENGTH_LONG
                ).setAction("Reintentar") { viewModel.checkConnectivity() }.show()
            }
        }

        viewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                "pending_sync" -> Snackbar.make(
                    binding.root,
                    "Cambios guardados localmente, pendientes de sincronización",
                    Snackbar.LENGTH_LONG
                ).setAction("Sincronizar") { if (viewModel.checkConnectivity()) viewModel.syncPendingEvents() }.show()

                "synced" -> Snackbar.make(binding.root, "Cambios sincronizados correctamente", Snackbar.LENGTH_SHORT).show()
                "sync_error" -> Snackbar.make(binding.root, "Error al sincronizar cambios", Snackbar.LENGTH_LONG)
                    .setAction("Reintentar") { if (viewModel.checkConnectivity()) viewModel.syncPendingEvents() }.show()

                "sync_completed" -> Snackbar.make(binding.root, "Sincronización completada", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(eventDetail: EventDetail) {
        binding.apply {
            toolbarLayout.title = eventDetail.title
            Glide.with(requireContext())
                .load(eventDetail.imageUrl ?: R.drawable.placeholder_event)
                .placeholder(R.drawable.placeholder_event)
                .error(R.drawable.placeholder_event)
                .centerCrop()
                .into(eventImage)

            tvDescription.text = eventDetail.description.orEmpty()
            tvDescription.visibility = if (eventDetail.description.isNullOrBlank()) View.GONE else View.VISIBLE

            tvDate.text = eventDetail.getFormattedDate()
            tvTime.text = eventDetail.getFormattedTimeRange()
            tvLocation.text = eventDetail.locationName.orEmpty()
            locationLayout.visibility = if (eventDetail.locationName != null) View.VISIBLE else View.GONE

            tvEventType.text = eventDetail.type?.replaceFirstChar { it.uppercase() } ?: "Event"
            tvEventType.setBackgroundResource(
                when (eventDetail.type) {
                    "movies" -> R.drawable.bg_badge_movie
                    "sports" -> R.drawable.bg_badge_sports
                    else -> R.drawable.bg_badge_event
                }
            )

            tvNowPlaying.visibility = if (eventDetail.isHappeningNow()) View.VISIBLE else View.GONE
            fabAddToCalendar.visibility = if (eventDetail.isUpcoming()) View.VISIBLE else View.GONE
            additionalInfoCard.visibility = if (eventDetail.additionalInfo.isNullOrBlank()) View.GONE else View.VISIBLE
            tvAdditionalInfo.text = eventDetail.additionalInfo.orEmpty()
            organizerLayout.visibility = if (eventDetail.organizerName.isNullOrBlank()) View.GONE else View.VISIBLE
            tvOrganizerName.text = eventDetail.organizerName.orEmpty()
            capacityLayout.visibility = if (eventDetail.capacity == null) View.GONE else View.VISIBLE
            tvCapacity.text = "${eventDetail.capacity} personas"
            btnRegister.visibility = if (eventDetail.registrationUrl.isNullOrBlank()) View.GONE else View.VISIBLE
            btnRegister.setOnClickListener {
                Toast.makeText(context, "Regístrate en: ${eventDetail.registrationUrl}", Toast.LENGTH_SHORT).show()
            }
        }

        val isConnected = ConnectivityHelper.getInstance(requireContext()).isConnected(requireContext())
        VisitedEventsManager.addEvent(eventDetail, isConnected)
        Log.d("Historial", "Evento insertado en memoria: ${eventDetail.title}")
    }

    private fun showErrorView(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
        binding.btnRetry.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            viewModel.loadEventDetail(eventId)
        }
    }

    private fun navigateToLocation(locationId: Long) {
        findNavController().navigate(R.id.buildingDetailFragment, Bundle().apply {
            putLong("buildingId", locationId)
        })
    }

    private fun addEventToCalendar(eventDetail: EventDetail) {
        val startMillis = LocalDateTime.parse(eventDetail.startTime, DateTimeFormatter.ISO_DATE_TIME)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = LocalDateTime.parse(eventDetail.endTime, DateTimeFormatter.ISO_DATE_TIME)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, eventDetail.title)
            putExtra(CalendarContract.Events.DESCRIPTION, eventDetail.description)
            eventDetail.locationName?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        }
        startActivity(intent)
    }

    private fun shareEvent(eventDetail: EventDetail) {
        val shareText = """
            Event: ${eventDetail.title}
            Date: ${eventDetail.getFormattedDate()}
            Time: ${eventDetail.getFormattedTimeRange()}
            ${eventDetail.locationName?.let { "Location: $it" }.orEmpty()}
            
            ${eventDetail.description.orEmpty()}
        """.trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share Event"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
