package com.example.explorandes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.R
import com.example.explorandes.models.Building
import com.example.explorandes.utils.GlideImageLoader

class BuildingAdapter(
    private var buildings: List<Building>,
    private val onBuildingClicked: (Building) -> Unit
) : RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder>() {

    class BuildingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.building_image)
        val name: TextView = view.findViewById(R.id.building_name)
        val code: TextView = view.findViewById(R.id.building_code)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_building, parent, false)
        return BuildingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        val building = buildings[position]
        val context = holder.itemView.context
        
        holder.name.text = building.name
        holder.code.text = building.code

        // Usar el imageLoader para cargar imágenes con caché
        val imageLoader = GlideImageLoader(context)
        imageLoader.loadImage(building.imageUrl, holder.image)

        // Set click listener on the entire item
        holder.itemView.setOnClickListener {
            onBuildingClicked(building)
        }
        //Comment 1
    }

    override fun getItemCount() = buildings.size

    fun updateData(newBuildings: List<Building>) {
        buildings = newBuildings
        notifyDataSetChanged()
    }
}