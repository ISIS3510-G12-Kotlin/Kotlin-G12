package com.example.explorandes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.R
import com.example.explorandes.databinding.ItemEventBinding
import com.example.explorandes.models.Event
import com.example.explorandes.utils.GlideImageLoader

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEventClick(getItem(position))
                }
            }
        }

        fun bind(event: Event) {
            binding.apply {
                tvEventTitle.text = event.title
                tvEventDate.text = event.getFormattedDate()
                tvEventTime.text = event.getFormattedTimeRange()

                // Set location text if available
                event.locationName?.let {
                    tvEventLocation.text = it
                    tvEventLocation.visibility = View.VISIBLE
                } ?: run {
                    tvEventLocation.visibility = View.GONE
                }

                // Set event type badge
                when (event.type) {
                    Event.TYPE_EVENT -> {
                        tvEventType.text = "Event"
                        tvEventType.setBackgroundResource(R.drawable.bg_badge_event)
                    }
                    Event.TYPE_MOVIE -> {
                        tvEventType.text = "Movie"
                        tvEventType.setBackgroundResource(R.drawable.bg_badge_movie)
                    }
                    Event.TYPE_SPORTS -> {
                        tvEventType.text = "Sports"
                        tvEventType.setBackgroundResource(R.drawable.bg_badge_sports)
                    }
                    else -> {
                        tvEventType.text = "Event"
                        tvEventType.setBackgroundResource(R.drawable.bg_badge_event)
                    }
                }

                // Highlight ongoing events
                if (event.isHappeningNow()) {
                    cardEvent.strokeWidth = 2
                    cardEvent.strokeColor = root.context.getColor(R.color.colorAccent)
                    tvNowPlaying.visibility = View.VISIBLE
                } else {
                    cardEvent.strokeWidth = 0
                    tvNowPlaying.visibility = View.GONE
                }

                // Load event image usando el cacheado
                val imageLoader = GlideImageLoader(ivEventImage.context)
                imageLoader.loadImage(event.imageUrl, ivEventImage, R.drawable.placeholder_event)
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}