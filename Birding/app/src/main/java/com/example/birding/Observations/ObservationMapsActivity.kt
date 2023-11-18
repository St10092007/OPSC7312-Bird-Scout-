package com.example.birding.Observations

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.birding.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions

class ObservationMapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var observationsList: List<BirdObservation>
    private lateinit var mapView: MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_observation_maps)

        observationsList = intent.getSerializableExtra("observationsList") as List<BirdObservation>

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        for (observation in observationsList) {
            val location = observation.location
            googleMap.addMarker(MarkerOptions().position(location).title(observation.species))
        }

        val firstObservation = observationsList.firstOrNull()
        if (firstObservation != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstObservation.location, 10f))
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}