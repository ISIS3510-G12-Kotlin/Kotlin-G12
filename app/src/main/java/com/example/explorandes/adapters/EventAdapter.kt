package com.example.explorandes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.explorandes.R
import com.example.explorandes.databinding.ItemEventBinding
import com.example.explorandes.models.Event
import com.example.explorandes.models.EventDetail
import com.example.explorandes.utils.VisitedEventsManager

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedEvent = getItem(position)
                    onEventClick(clickedEvent)

                    // Guardar en historial en memoria
                    val eventDetail = EventDetail(
                        id = clickedEvent.id,
                        title = clickedEvent.title,
                        description = clickedEvent.description,
                        startTime = clickedEvent.startTime,
                        endTime = clickedEvent.endTime,
                        locationId = clickedEvent.locationId,
                        locationName = clickedEvent.locationName,
                        organizerName = null,
                        imageUrl = clickedEvent.imageUrl,
                        capacity = null,
                        registrationUrl = null,
                        type = clickedEvent.type,
                        additionalInfo = null
                    )
                    VisitedEventsManager.addEvent(eventDetail, true)
                }
            }
        }

        fun bind(event: Event) {
            binding.apply {
                tvEventTitle.text = event.title
                tvEventDate.text = event.getFormattedDate()
                tvEventTime.text = event.getFormattedTimeRange()

                event.locationName?.let {
                    tvEventLocation.text = it
                    tvEventLocation.visibility = View.VISIBLE
                } ?: run {
                    tvEventLocation.visibility = View.GONE
                }

                tvEventType.text = when (event.type) {
                    Event.TYPE_MOVIE -> "Movie"
                    Event.TYPE_SPORTS -> "Sports"
                    else -> "Event"
                }

                tvEventType.setBackgroundResource(
                    when (event.type) {
                        Event.TYPE_MOVIE -> R.drawable.bg_badge_movie
                        Event.TYPE_SPORTS -> R.drawable.bg_badge_sports
                        else -> R.drawable.bg_badge_event
                    }
                )

                if (event.isHappeningNow()) {
                    cardEvent.strokeWidth = 2
                    cardEvent.strokeColor = root.context.getColor(R.color.colorAccent)
                    tvNowPlaying.visibility = View.VISIBLE
                } else {
                    cardEvent.strokeWidth = 0
                    tvNowPlaying.visibility = View.GONE
                }
                val imageUrl = event.imageUrl
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(ivEventImage.context)
                        .load(imageUrl)
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.placeholder_event)
                                .error(R.drawable.placeholder_event)
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        )
                        .into(ivEventImage)
                } else {

                    ivEventImage.setImageResource(R.drawable.placeholder_event)

                }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean =
            oldItem == newItem
    }
}
