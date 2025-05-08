package com.example.explorandes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.explorandes.adapters.PlaceAdapter
import com.example.explorandes.models.Building
import com.example.explorandes.models.Place
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class BuildingDetailActivity : BaseActivity() {

    private lateinit var placeAdapter: PlaceAdapter
    private lateinit var buildingImage: ImageView
    private lateinit var buildingName: TextView
    private lateinit var buildingDescription: TextView
    private lateinit var buildingCode: TextView
    private lateinit var placesRecyclerView: RecyclerView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var toolbar: MaterialToolbar
    private lateinit var noPlacesMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_building_detail)

        // Get the building object directly
        val building = intent.getParcelableExtra<Building>("BUILDING")
        if (building == null) {
            Toast.makeText(this, "Building not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()

        val fabMap = findViewById<FloatingActionButton>(R.id.fab_map)
        fabMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("BUILDING_ID", building.id)
            startActivity(intent)
        }

        // Load building info directly
        updateBuildingUI(building)

        // If you still want to load places from backend, you can implement that here if needed
        updatePlacesUI(building.places ?: emptyList()) // fallback to empty list
    }

    private fun initializeViews() {
        buildingImage = findViewById(R.id.building_image)
        buildingName = findViewById(R.id.building_name)
        buildingDescription = findViewById(R.id.building_description)
        buildingCode = findViewById(R.id.building_code)
        placesRecyclerView = findViewById(R.id.places_recyclerview)
        progressIndicator = findViewById(R.id.progress_indicator)
        toolbar = findViewById(R.id.toolbar)
        noPlacesMessage = findViewById(R.id.no_places_message)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        placesRecyclerView.layoutManager = LinearLayoutManager(this)
        placeAdapter = PlaceAdapter(emptyList()) { place ->
            Toast.makeText(this, "Selected place: ${place.name}", Toast.LENGTH_SHORT).show()
        }
        placesRecyclerView.adapter = placeAdapter
    }

    private fun updateBuildingUI(building: Building) {
        buildingName.text = building.name
        buildingDescription.text = building.description ?: "No description available"
        buildingCode.text = "Code: ${building.code}"

        if (!building.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(building.imageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .into(buildingImage)
        } else {
            buildingImage.setImageResource(R.drawable.profile_placeholder)
        }
    }

    private fun updatePlacesUI(places: List<Place>) {
        if (places.isEmpty()) {
            noPlacesMessage.visibility = View.VISIBLE
            placesRecyclerView.visibility = View.GONE
        } else {
            noPlacesMessage.visibility = View.GONE
            placesRecyclerView.visibility = View.VISIBLE
            placeAdapter.updateData(places)
        }
    }
}
