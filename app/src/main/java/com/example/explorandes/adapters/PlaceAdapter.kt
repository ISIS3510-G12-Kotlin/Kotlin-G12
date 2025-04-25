package com.example.explorandes.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.MapActivity
import com.example.explorandes.R
import com.example.explorandes.models.Place
import com.example.explorandes.utils.GlideImageLoader

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
        val context = holder.itemView.context
        
        holder.placeName.text = place.name
        holder.placeLocation.text = place.floor ?: ""

        // Usar el imageLoader para cargar imágenes (con estrategia para miniaturas)
        val imageLoader = GlideImageLoader(context)
        imageLoader.loadThumbnail(place.imageUrl, holder.placeImage)

        // Configure item click
        holder.itemView.setOnClickListener {
            onPlaceClick(place)
        }
        
        // Configure navigation button
        holder.itemView.findViewById<Button>(R.id.btn_navigate).setOnClickListener {
            // First get buildingId from the place
            val buildingId = place.building?.id ?: place.buildingId
            
            // If we have a buildingId, start the MapActivity
            if (buildingId != null) {
                val intent = Intent(context, MapActivity::class.java)
                intent.putExtra("BUILDING_ID", buildingId)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = places.size

    // Method to update data when fetched from backend
    fun updateData(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}