// Import statements for required libraries and components
package com.example.birding.Hotspot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.Core.Species
import com.example.birding.Core.SpeciesListResponse
import com.example.birding.Home.HomeActivity
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.example.birding.Settings.AccountSettingsActivity
import com.example.birding.Settings.AccountSettingsActivity.Companion.IS_METRIC_PREFERENCE_KEY
import com.example.birding.Settings.AccountSettingsActivity.Companion.MAX_DISTANCE_PREFERENCE_KEY
import com.example.birding.Settings.AccountSettingsActivity.Companion.PREFERENCES_NAME
import com.example.birding.Settings.SettingsActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson

// Class definition for HotspotsActivity
class HotspotsActivity : AppCompatActivity(), OnMapReadyCallback {

    // UI and Location-related Components
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private var selectedMarker: Marker? = null
    private var location: Location? = null
    private val locationPermissionCode = 100
    private lateinit var hotspotDetailsFrameLayout: FrameLayout
    private lateinit var tvSelectedHotspot: TextView
    private lateinit var btnHotspotDirection: Button
    private lateinit var tvDistanceToHotspot: TextView
    private var currentRoute: Polyline? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private var isNavigating = false

    // onCreate method called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotspots)
        mapView = findViewById(R.id.mapView)
        tvSelectedHotspot = findViewById(R.id.tvSelectedHotspot)
        btnHotspotDirection = findViewById(R.id.btnHotspotDirection)
        hotspotDetailsFrameLayout = findViewById(R.id.hotspotDetailsFrameLayout)
        tvDistanceToHotspot = findViewById(R.id.tvDistanceToHotspot)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)


        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

        // Check for location permissions and request them if necessary
        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            // Location permission is granted; fetch user's last location and eBird hotspots
            fetchLastLocation { location ->
                location?.let {
                    fetchEBirdHotspots(LatLng(location.latitude, location.longitude))
                }
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { newLocation ->
                    // Handle the new location here
                    location = newLocation
                    centerMapOnUserLocation()
                }
            }
        }

        setupBottomNavigation()
        }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_hotspots

        val observationMenuItem = bottomNavigationView.menu.findItem(R.id.menu_observations)
        val hotspotsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_hotspots)
        val homeMenuItem = bottomNavigationView.menu.findItem(R.id.menu_home)
        val settingsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_settings)

        // Set tooltips for navigation items
        observationMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Observations") }
        hotspotsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Hotspots") }
        homeMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Home") }
        settingsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Settings") }

        // Bottom navigation item click listener
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))

                    true
                }
                R.id.menu_hotspots -> {
                    // hotspots is already selected, do nothing.
                    true
                }
                R.id.menu_observations -> {
                    startActivity(Intent(this, ObservationsActivity::class.java))
                    true
                }
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    // Location and Permissions
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocationPermission && coarseLocationPermission
    }

    // Map Initialization
    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap

        // Enable zoom controls
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Check for location permissions before enabling user's location
        if (hasLocationPermission()) {
            try {
                googleMap.isMyLocationEnabled = true

                // Fetch nearby hotspots from the eBird API and add markers
                fetchLastLocation { location ->
                    location?.let {
                        fetchEBirdHotspots(LatLng(location.latitude, location.longitude))
                    }
                }

                // Set a marker click listener
                googleMap.setOnMarkerClickListener { marker ->
                    toggleHotspotDetails(marker)
                    true // Indicate that the click event has been consumed
                }
                googleMap.setOnMyLocationButtonClickListener {
                    centerMapOnUserLocation()
                    true
                }
            } catch (securityException: SecurityException) {
                Toast.makeText(this, "Location permission is required for this operation", Toast.LENGTH_SHORT).show()
                requestLocationPermission()
            }
        }
    }

    // User Interaction with Markers
    private fun toggleHotspotDetails(marker: Marker) {
        // Check if the clicked marker is the same as the previously selected marker
        if (selectedMarker == marker) {
            // Hide the hotspot details
            selectedMarker?.setIcon(customMarkerBird(this,100))
            hotspotDetailsFrameLayout.visibility = View.GONE
            selectedMarker = null // Deselect the marker
            return
        }

        // Remove the previous route if it exists
        removeRoute()

        // Hide the distance text
        tvDistanceToHotspot.visibility = View.GONE

        // Select the clicked marker (show hotspot details)
        selectedMarker?.setIcon(customMarkerBird(this,100))
        selectedMarker = marker
        selectedMarker?.setIcon(createCustomMarker())
        selectedMarker?.zIndex = 2.0f

        // Update the hotspot name in the TextView
        val hotspotName = marker.title
        tvSelectedHotspot.text = hotspotName

        hotspotDetailsFrameLayout.visibility = View.VISIBLE
        tvSelectedHotspot.visibility = View.VISIBLE
        btnHotspotDirection.visibility = View.VISIBLE
        btnHotspotDirection.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_blue))

        val unitPreference = sharedPreferences.getString(IS_METRIC_PREFERENCE_KEY, "Kilometers")

        // Calculate and display the distance
        val origin = location?.let { LatLng(it.latitude, it.longitude) }
        if (origin != null) {
            val distance = calculateDistance(origin, marker.position)

            // Update the distance based on user preferences
            val formattedDistance = if (unitPreference == "Kilometers") {
                if (distance > 1000) {
                    val distanceInKm = distance / 1000.0
                    String.format("%.2f km", distanceInKm)
                } else {
                    String.format("%.2f meters", distance)
                }
            } else if (unitPreference == "Miles") {
                // Convert meters to miles
                val miles = distance * 0.000621371
                String.format("%.2f mi", miles)
            } else {
                // Default to Kilometers
                if (distance > 1000) {
                    val distanceInKm = distance / 1000.0
                    String.format("%.2f km", distanceInKm)
                } else {
                    String.format("%.2f meters", distance)
                }
            }

            tvDistanceToHotspot.text = "Distance: $formattedDistance"
            tvDistanceToHotspot.visibility = View.VISIBLE
            val builder = LatLngBounds.builder()
            builder.include(origin)
            builder.include(marker.position)
            val bounds = builder.build()

            // Move the camera to focus on the bounding box
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)
        } else {
            tvDistanceToHotspot.visibility = View.GONE
        }
//        btnHotspotDirection.setOnClickListener {
//            if (isNavigating) {
//                // User is navigating; stop the navigation
//                isNavigating = false
//                removeRoute()
//                resetDirectionButton()
//                stopLocationUpdates()
//            } else {
//                // User is not navigating; start navigation
//                isNavigating = true
//                btnHotspotDirection.apply {
//                    text = "Stop"
//                    setBackgroundColor(ContextCompat.getColor(this@HotspotsActivity, R.color.primary_red))
//                }
//                startLocationUpdates()
//                // Calculate and display the route to the selected hotspot
//                if (origin != null) {
//                    calculateAndDisplayRoute(origin, marker.position)
//                    // Fetch and display bird species data for the selected hotspot
//                    marker.title?.let { it1 -> fetchBirdSpeciesData(it1) }
//                }
//            }
//        }
        btnHotspotDirection.setOnClickListener {
            if (isNavigating) {
                // User is navigating; stop the navigation
                isNavigating = false
                removeRoute()
                resetDirectionButton()
                stopLocationUpdates()
                selectedMarker?.setIcon(customMarkerBird(this, 100))
                selectedMarker = null
            } else {
                // User is not navigating; start navigation
                isNavigating = true


//                btnHotspotDirection.setBackgroundResource(R.drawable.btn_background_red)
//                btnHotspotDirection.setTextColor(ContextCompat.getColor(this, R.color.primary_red))
//                btnHotspotDirection.setBackgroundColor(0xFFFF0000.toInt()) // Red color

                btnHotspotDirection.apply {
                    text = "Stop"
                    setBackgroundColor(ContextCompat.getColor(this@HotspotsActivity, R.color.primary_red))

                }
                Log.d("ButtonColors", "Background Color: ${btnHotspotDirection.background}")
                Log.d("ButtonColors", "Text Color: ${btnHotspotDirection.currentTextColor}")

                startLocationUpdates()
                // Calculate and display the route to the selected hotspot
                if (origin != null) {
                    calculateAndDisplayRoute(origin, marker.position)
                }
            }
        }
    }

    private fun resetDirectionButton() {
        btnHotspotDirection.apply {
            text = "Get Directions"
            setBackgroundColor(ContextCompat.getColor(this@HotspotsActivity, R.color.primary_blue))
        }
        centerMapOnUserLocation()
        hotspotDetailsFrameLayout.visibility = View.GONE
    }

    private fun calculateDistance(origin: LatLng, destination: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            origin.latitude, origin.longitude,
            destination.latitude, destination.longitude,
            results
        )
        return results[0]
    }

    private fun centerMapOnUserLocation() {
        if (hasLocationPermission()) {
            fetchLastLocation { location ->
                location?.let {
                    fetchEBirdHotspots(LatLng(location.latitude, location.longitude))
                }
            }
        }
    }

    // Location Services and Data Fetching
    private fun fetchLastLocation(callback: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                this.location = location
                location?.let {
                    // Move the camera to the user's current location
                    val userLocation = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                    // Add a marker for the user's current location
                    googleMap.addMarker(
                        MarkerOptions().position(userLocation).title("You are here")
                    )
                    // Call the callback with the user's location
                    callback(location)
                }
            }
    }

    private fun fetchEBirdHotspots(location: LatLng) {
        val apiKey = "h5qidkb3h7cv"
        // Retrieve user preferences from SharedPreferences
        val selectedUnit = sharedPreferences.getString(IS_METRIC_PREFERENCE_KEY, "Kilometers") ?: "Kilometers"
        val selectedDistance = sharedPreferences.getInt(MAX_DISTANCE_PREFERENCE_KEY, 50)
        // Convert maxDistance to kilometers if the user prefers miles
        val convertedDistance = if (selectedUnit == "Kilometers") {
            selectedDistance // No need to convert, as it's already in kilometers
        } else {
            // Convert miles to kilometers
            kilometersToMiles(selectedDistance)
        }
        Log.e("MyApp", "converted distance : $convertedDistance")

        // EBIRD API 2.0
        val eBirdAPIUrl ="https://api.ebird.org/v2/ref/hotspot/geo?lat=${location.latitude}&lng=${location.longitude}&dist=${convertedDistance}&fmt=json"

        // Create URL object
        var url: URL? = null
        try {
            url = URL(eBirdAPIUrl)
            thread {
                try {
                    // Create an HTTP URL connection
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    // Add the API key to the request headers
                    connection.setRequestProperty("X-eBirdApiToken", apiKey)

                    // Get the response code
                    val responseCode = connection.responseCode
                    // Check if the request was successful (HTTP status code 200)
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Fetch data from the connection
                        val inputStream = connection.inputStream
                        val urlData = inputStream.bufferedReader().use { it.readText() }

                        // Parse JSON data
                        try {
                            val responseString = urlData.trim() // Trim any leading/trailing whitespace

                            if (responseString.startsWith("[")) {
                                // Valid JSON array response
                                val hotspotsArray = JSONArray(responseString)
                                // Process the JSON array here
                                for (i in 0 until hotspotsArray.length()) {
                                    val hotspot = hotspotsArray.getJSONObject(i)
                                    val name = hotspot.getString("locName")
                                    val lat = hotspot.getDouble("lat")
                                    val lng = hotspot.getDouble("lng")
                                    val hotspotLocation = LatLng(lat, lng)

                                    // Add markers for each hotspot
                                    runOnUiThread {
                                        googleMap.addMarker(
                                            MarkerOptions()
                                                .position(hotspotLocation)
                                                .title(name)
                                                .icon(customMarkerBird(this,100))
                                                .zIndex(1.0f))
                                    }
                                }
                            } else {
                                // Handle other types of responses or errors
                                runOnUiThread {
                                    Toast.makeText(this@HotspotsActivity, "Unexpected API response: $urlData", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            // Handle JSON parsing error
                            runOnUiThread {
                                Toast.makeText(this@HotspotsActivity, "Error parsing eBird data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Handle HTTP error here (e.g., display an error message)
                        runOnUiThread {
                            Toast.makeText(this@HotspotsActivity, "HTTP error: $responseCode", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Close the connection
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle other exceptions here
                    runOnUiThread {
                        Toast.makeText(this@HotspotsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
    }

    private fun kilometersToMiles(kilometers: Int): Int {
        return (kilometers / 1.60934).toInt()
    }

    private fun calculateAndDisplayRoute(origin: LatLng, destination: LatLng) {
        val apiKey = "AIzaSyDHTmCbWEXU66wNV7hIIhaBPPJqXjnJX6I"

        //Some Portions of this code are modifications based on work created and shared by Google and used according to terms described in the Creative Commons 4.0 Attribution License.
        //Author: Google
        //Date: Last updated 2023-10-23
        //link : https://developer.android.com/training/maps/maps-and-places

        val geoApiContext = GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()

        val request = DirectionsApi.newRequest(geoApiContext)
            .mode(TravelMode.DRIVING)
            .origin(origin.latitude.toString() + "," + origin.longitude.toString())
            .destination(destination.latitude.toString() + "," + destination.longitude.toString())

        request.setCallback(object : PendingResult.Callback<DirectionsResult?> {
            override fun onResult(result: DirectionsResult?) {
                runOnUiThread {
                if (result != null) {
                    // Remove the previous route if it exists
                    removeRoute()

                    // Draw the route on the map
                    val decodedPath = decodePolyline(result.routes[0].overviewPolyline.encodedPath)


                        // Update UI on the main thread
                        currentRoute = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .color(Color.BLUE)
                                .width(10f)
                        )

                        // Get the LatLngBounds that include both origin and destination
                        val builder = LatLngBounds.builder()
                        builder.include(origin)
                        builder.include(destination)
                        val bounds = builder.build()

                        // Move the camera to center on the route with padding
                        val padding = 100
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    }
                 else {
                    // Handle error or display a message
                    runOnUiThread {
                        Toast.makeText(this@HotspotsActivity, "Error calculating route", Toast.LENGTH_SHORT).show()
                    }
                }
                }
            }

            override fun onFailure(e: Throwable?) {
                // Handle error or display a message
                runOnUiThread {
                    Toast.makeText(this@HotspotsActivity, "Error calculating route: ${e?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    private fun removeRoute() {
        currentRoute?.remove()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest()
            .setInterval(10000) // Update interval in milliseconds
            .setFastestInterval(5000) // Fastest update interval
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Lifecycle Management
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

    // Marker and Drawing Functions
    //The following code was taken and modified from StackOverflow
    //Author: Jaskaran Singh
    //Date: 4 October 2016
    //link : https://stackoverflow.com/a/39851340
    fun decodePolyline(polyline: String): List<LatLng> {
        val len = polyline.length
        var index = 0
        val decoded = mutableListOf<LatLng>()
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = polyline[index++].toInt() - 63
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = polyline[index++].toInt() - 63
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            decoded.add(latLng)
        }

        return decoded
    }

    private fun customMarkerBird(context: Context, targetSize: Int): BitmapDescriptor {
        // Load the dove icon from your drawables
        val doveIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.dove)

        if (doveIcon is BitmapDrawable) {
            val originalBitmap = doveIcon.bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, false)
            return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        } else {
            // If the drawable isn't a BitmapDrawable, handle the error accordingly
            throw IllegalArgumentException("Invalid drawable")
        }
    }

    //Some Portions of this code are modifications based on work created and shared by Google and used according to terms described in the Creative Commons 4.0 Attribution License.
    //Author: Google
    //Date: Last updated 2023-10-23
    //link : https://developer.android.com/training/maps/maps-and-places
    private fun createCustomMarker(): BitmapDescriptor {
        val circleRadius = 50

        val bitmap = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.RED
        paint.isAntiAlias = true
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat(), paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

//    private fun fetchBirdSpeciesData(hotspotId: String) {
//        val apiKey = "AIzaSyDHTmCbWEXU66wNV7hIIhaBPPJqXjnJX6I"
//        val eBirdAPIUrl = "https://api.ebird.org/v2/product/spplist/za"
//
//        val url = URL(eBirdAPIUrl)
//        thread {
//            try {
//                val connection = url.openConnection() as HttpURLConnection
//                connection.requestMethod = "GET"
//                connection.setRequestProperty("X-eBirdApiToken", apiKey)
//
//                val responseCode = connection.responseCode
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    val inputStream = connection.inputStream
//                    val jsonData = inputStream.bufferedReader().use { it.readText() }
//
//                    runOnUiThread {
//                        displayBirdSpeciesData(jsonData)
//                    }
//                } else {
//                    runOnUiThread {
//                        Toast.makeText(
//                            this@HotspotsActivity,
//                            "Error fetching bird species data: $responseCode",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//
//                connection.disconnect()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                runOnUiThread {
//                    Toast.makeText(
//                        this@HotspotsActivity,
//                        "Error fetching bird species data: ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

//    private fun fetchBirdSpeciesData(location: LatLng) {
//        val apiKey = "h5qidkb3h7cv"
//        // EBIRD API 2.0
//        val eBirdAPIUrl = "https://api.ebird.org/v2/product/spplist/$hotspotId/recent"
//
//        // Create URL object
//        var url: URL? = null
//        try {
//            url = URL(eBirdAPIUrl)
//            thread {
//                try {
//                    // Create an HTTP URL connection
//                    val connection = url.openConnection() as HttpURLConnection
//                    connection.requestMethod = "GET"
//
//                    // Add the API key to the request headers
//                    connection.setRequestProperty("X-eBirdApiToken", apiKey)
//
//                    // Get the response code
//                    val responseCode = connection.responseCode
//                    // Check if the request was successful (HTTP status code 200)
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        // Fetch data from the connection
//                        val inputStream = connection.inputStream
//                        val urlData = inputStream.bufferedReader().use { it.readText() }
//
//                        // Parse JSON data
//                        try {
//                            val responseString = urlData.trim() // Trim any leading/trailing whitespace
//
//                            if (responseString.startsWith("[")) {
//                                // Valid JSON array response
//                                val speciesArray = JSONArray(responseString)
//                                // Process the JSON array here
//                                for (i in 0 until speciesArray.length()) {
//                                    val hotspot = speciesArray.getJSONObject(i)
//                                    val name = hotspot.getString("locName")
//                                    val lat = hotspot.getDouble("lat")
//                                    val lng = hotspot.getDouble("lng")
//                                    val hotspotLocation = LatLng(lat, lng)
//
//                                    // Add markers for each hotspot
//                                    runOnUiThread {
//                                        googleMap.addMarker(
//                                            MarkerOptions()
//                                                .position(hotspotLocation)
//                                                .title(name)
//                                                .icon(customMarkerBird(this,100))
//                                                .zIndex(1.0f))
//                                    }
//                                }
//                            } else {
//                                // Handle other types of responses or errors
//                                runOnUiThread {
//                                    Toast.makeText(this@HotspotsActivity, "Unexpected API response: $urlData", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        } catch (e: JSONException) {
//                            e.printStackTrace()
//                            // Handle JSON parsing error
//                            runOnUiThread {
//                                Toast.makeText(this@HotspotsActivity, "Error parsing eBird data: ${e.message}", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    } else {
//                        // Handle HTTP error here (e.g., display an error message)
//                        runOnUiThread {
//                            Toast.makeText(this@HotspotsActivity, "HTTP error: $responseCode", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                    // Close the connection
//                    connection.disconnect()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    // Handle other exceptions here
//                    runOnUiThread {
//                        Toast.makeText(this@HotspotsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        } catch (e: MalformedURLException) {
//            e.printStackTrace()
//        }
//    }
//    private fun displayBirdSpeciesData(jsonData: String) {
//        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewBirdSpecies)
//        val birdSpeciesList = parseJsonData(jsonData)  // Implement a method to parse JSON data and extract bird species information
//        val adapter = BirdSpeciesAdapter(birdSpeciesList)  // Create a custom adapter for displaying bird species
//        recyclerView.adapter = adapter
//    }
//
//    private fun parseJsonData(jsonData: String): List<Species> {
//        val gson = Gson()
//        val speciesListResponse = gson.fromJson(jsonData, SpeciesListResponse::class.java)
//        return speciesListResponse.species
//    }



}
