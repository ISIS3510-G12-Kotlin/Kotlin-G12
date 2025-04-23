package com.example.explorandes.adapters

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.explorandes.MapActivity
import com.example.explorandes.R
import com.example.explorandes.models.Place

class PlaceAdapter(
    private var places: List<Place>,
    private val onPlaceClick: (Place) -> Unit = {}
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeImage: ImageView = itemView.findViewById(R.id.place_image)
        val placeName: TextView = itemView.findViewById(R.id.place_name)
        val placeLocation: TextView = itemView.findViewById(R.id.place_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.placeName.text = place.name
        holder.placeLocation.text = place.floor ?: ""

        // Load image from URL
        if (!place.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(place.imageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .into(holder.placeImage)
        } else {
            holder.placeImage.setImageResource(R.drawable.profile_placeholder)
        }

        // Configure item click
        holder.itemView.setOnClickListener {
            onPlaceClick(place)
        }
        
        // Configure favorite button
        holder.itemView.findViewById<Button>(R.id.btn_favorite).setOnClickListener {
            // TODO: Implement favorite functionality in future iterations
            Toast.makeText(holder.itemView.context, "Favorite feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Configure events button
        holder.itemView.findViewById<Button>(R.id.btn_events).setOnClickListener {
            // TODO: Implement events functionality in future iterations
            Toast.makeText(holder.itemView.context, "Events feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = places.size

    // Method to update data when fetched from backend
    fun updateData(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}