package com.example.explorandes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.explorandes.models.Event
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventDetailActivity : BaseActivity() {
    
    private lateinit var eventImage: ImageView
    private lateinit var eventTitle: TextView
    private lateinit var eventDate: TextView
    private lateinit var eventTime: TextView
    private lateinit var eventLocation: TextView
    private lateinit var eventDescription: TextView
    private lateinit var eventType: TextView
    private lateinit var mapButton: Button
    private lateinit var addToCalendarFab: FloatingActionButton
    private lateinit var shareFab: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar
    
    private var event: Event? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        eventImage = findViewById(R.id.event_image)
        eventTitle = findViewById(R.id.event_title)
        eventDate = findViewById(R.id.event_date)
        eventTime = findViewById(R.id.event_time)
        eventLocation = findViewById(R.id.event_location)
        eventDescription = findViewById(R.id.event_description)
        eventType = findViewById(R.id.event_type)
        mapButton = findViewById(R.id.map_button)
        addToCalendarFab = findViewById(R.id.fab_add_to_calendar)
        shareFab = findViewById(R.id.fab_share)
        
        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        
        // Get event from intent
        event = intent.getParcelableExtra("EVENT")
        
        if (event == null) {
            Toast.makeText(this, "No se encontró información del evento", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load event data
        loadEventData()
        
        // Set up button listeners
        setupButtonListeners()
    }
    
    private fun loadEventData() {
        event?.let { event ->
            // Set text data
            eventTitle.text = event.title
            eventDate.text = event.getFormattedDate()
            eventTime.text = event.getFormattedTimeRange()
            eventLocation.text = event.locationName ?: "Ubicación no disponible"
            eventDescription.text = event.description ?: "Sin descripción"
            
            // Set event type badge
            when (event.type) {
                Event.TYPE_EVENT -> {
                    eventType.text = "Event"
                    eventType.setBackgroundResource(R.drawable.bg_badge_event)
                }
                Event.TYPE_MOVIE -> {
                    eventType.text = "Movie"
                    eventType.setBackgroundResource(R.drawable.bg_badge_movie)
                }
                Event.TYPE_SPORTS -> {
                    eventType.text = "Sports"
                    eventType.setBackgroundResource(R.drawable.bg_badge_sports)
                }
                else -> {
                    eventType.text = "Event"
                    eventType.setBackgroundResource(R.drawable.bg_badge_event)
                }
            }
            
            // Handle "Now Playing" badge visibility
            findViewById<TextView>(R.id.tv_now_playing)?.apply {
                visibility = if (event.isHappeningNow()) View.VISIBLE else View.GONE
            }
            
            // Load image with Glide
            Glide.with(this)
                .load(event.imageUrl)
                .placeholder(R.drawable.placeholder_event)
                .error(R.drawable.placeholder_event)
                .into(eventImage)
        }
    }
    
    private fun setupButtonListeners() {
        event?.let { event ->
            // Map button - show event location on map
            mapButton.setOnClickListener {
                event.locationId?.let { locationId ->
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("BUILDING_ID", locationId)
                    startActivity(intent)
                } ?: Toast.makeText(this, "No hay información de ubicación disponible", Toast.LENGTH_SHORT).show()
            }
            
            // Add to calendar button
            addToCalendarFab.setOnClickListener {
                addEventToCalendar(event)
            }
            
            // Share button
            shareFab.setOnClickListener {
                shareEvent(event)
            }
        }
    }
    
    private fun addEventToCalendar(event: Event) {
        try {
            val startMillis = LocalDateTime.parse(
                event.startTime,
                DateTimeFormatter.ISO_DATE_TIME
            ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val endMillis = LocalDateTime.parse(
                event.endTime,
                DateTimeFormatter.ISO_DATE_TIME
            ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                putExtra(CalendarContract.Events.TITLE, event.title)
                putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                event.locationName?.let {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, it)
                }
            }
            
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al agregar al calendario: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareEvent(event: Event) {
        val shareText = """
            ¡Evento en UniAndes!
            
            ${event.title}
            Fecha: ${event.getFormattedDate()}
            Hora: ${event.getFormattedTimeRange()}
            ${if (event.locationName != null) "Lugar: ${event.locationName}" else ""}
            
            ${event.description ?: ""}
            
            #ExplorAndes
        """.trimIndent()
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        
        startActivity(Intent.createChooser(sendIntent, "Compartir evento"))
    }
}