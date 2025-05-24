package com.example.explorandes.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.BuildingDetailActivity
import com.example.explorandes.EventDetailActivity
import com.example.explorandes.R
import com.example.explorandes.models.Building
import com.example.explorandes.models.Event

class CombinedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var buildings: List<Building> = emptyList()
    private var events: List<Event> = emptyList()

    companion object {
        private const val TYPE_HEADER_BUILDINGS = 0
        private const val TYPE_BUILDING = 1
        private const val TYPE_HEADER_EVENTS = 2
        private const val TYPE_EVENT = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 && buildings.isNotEmpty() -> TYPE_HEADER_BUILDINGS
            position <= buildings.size && buildings.isNotEmpty() -> TYPE_BUILDING
            position == buildings.size + 1 && events.isNotEmpty() -> TYPE_HEADER_EVENTS
            else -> TYPE_EVENT
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        if (buildings.isNotEmpty()) {
            count += buildings.size + 1 // +1 for header
        }
        if (events.isNotEmpty()) {
            count += events.size + 1 // +1 for header
        }
        return count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER_BUILDINGS, TYPE_HEADER_EVENTS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_BUILDING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_building, parent, false)
                BuildingViewHolder(view)
            }
            TYPE_EVENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_event, parent, false)
                EventViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_HEADER_BUILDINGS -> {
                (holder as HeaderViewHolder).bind("Edificios (${buildings.size})")
            }
            TYPE_HEADER_EVENTS -> {
                (holder as HeaderViewHolder).bind("Eventos (${events.size})")
            }
            TYPE_BUILDING -> {
                val buildingPosition = position - 1 // -1 for header
                if (buildingPosition >= 0 && buildingPosition < buildings.size) {
                    (holder as BuildingViewHolder).bind(buildings[buildingPosition])
                }
            }
            TYPE_EVENT -> {
                val eventPosition = if (buildings.isNotEmpty()) {
                    position - buildings.size - 2 // -2 for both headers
                } else {
                    position - 1 // -1 for events header only
                }
                if (eventPosition >= 0 && eventPosition < events.size) {
                    (holder as EventViewHolder).bind(events[eventPosition])
                }
            }
        }
    }

    fun updateData(newBuildings: List<Building>, newEvents: List<Event>) {
        buildings = newBuildings
        events = newEvents
        notifyDataSetChanged()
    }

    // Header ViewHolder
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.section_title)

        fun bind(title: String) {
            titleText.text = title
        }
    }

    // Building ViewHolder
    class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val buildingImage: android.widget.ImageView? = itemView.findViewById(R.id.building_image)
        private val buildingName: TextView? = itemView.findViewById(R.id.building_name)
        private val buildingCode: TextView? = itemView.findViewById(R.id.building_code)

        fun bind(building: Building) {
            buildingName?.text = building.name
            buildingCode?.text = building.code

            // Load image if available
            buildingImage?.let { imageView ->
                if (!building.imageUrl.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(itemView.context)
                        .load(building.imageUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.profile_placeholder)
                }
            }

            // Set click listener
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, BuildingDetailActivity::class.java)
                intent.putExtra("BUILDING", building)
                itemView.context.startActivity(intent)
            }
        }
    }

    // Event ViewHolder
    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventImage: android.widget.ImageView? = itemView.findViewById(R.id.event_image)
        private val eventTitle: TextView? = itemView.findViewById(R.id.event_title)
        private val eventDate: TextView? = itemView.findViewById(R.id.event_date)
        private val eventLocation: TextView? = itemView.findViewById(R.id.event_location)

        fun bind(event: Event) {
            eventTitle?.text = event.title
            eventDate?.text = event.getFormattedDate()
            eventLocation?.text = event.locationName ?: "Ubicación no disponible"

            // Load image if available
            eventImage?.let { imageView ->
                if (!event.imageUrl.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(itemView.context)
                        .load(event.imageUrl)
                        .placeholder(R.drawable.placeholder_event)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_event)
                }
            }

            // Set click listener
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, EventDetailActivity::class.java)
                intent.putExtra("EVENT", event)
                itemView.context.startActivity(intent)
            }
        }
    }
}