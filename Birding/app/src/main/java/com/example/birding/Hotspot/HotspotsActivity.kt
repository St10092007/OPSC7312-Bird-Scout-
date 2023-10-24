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
import com.example.birding.Home.HomeActivity
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.example.birding.Settings.SettingsActivity
import com.example.birding.Settings.SettingsActivity.Companion.IS_METRIC_PREFERENCE_KEY
import com.example.birding.Settings.SettingsActivity.Companion.MAX_DISTANCE_PREFERENCE_KEY
import com.example.birding.Settings.SettingsActivity.Companion.PREFERENCES_NAME
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

class HotspotsActivity : AppCompatActivity(), OnMapReadyCallback {

    // UI and Location-related Components
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private var selectedMarker: Marker? = null
    private var location: Location? = null
    private val locationPermissionCode = 100
    private lateinit var hotspotDetailsFrameLayout: FrameLayout
    private lateinit var selectedHotspot: TextView
    private lateinit var hotspotDirectionBtn: Button
    private lateinit var distanceToHotspot: TextView
    private var currentRoute: Polyline? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotspots)
        mapView = findViewById(R.id.mapView)
        selectedHotspot = findViewById(R.id.tvSelectedHotspot)
        hotspotDirectionBtn = findViewById(R.id.btnHotspotDirection)
        hotspotDetailsFrameLayout = findViewById(R.id.hotspotDetailsFrameLayout)
        distanceToHotspot = findViewById(R.id.tvDistanceToHotspot)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

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

        // navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_hotspots

        val observationMenuItem = bottomNavigationView.menu.findItem(R.id.menu_observations)
        val hotspotsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_hotspots)
        val homeMenuItem = bottomNavigationView.menu.findItem(R.id.menu_home)
        val settingsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_settings)

        observationMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Observations") }
        hotspotsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Hotspots") }
        homeMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Home") }
        settingsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Settings") }

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.menu_hotspots -> {
                    true
                }
                R.id.menu_observations -> {
                    // Handle observations menu item
                    startActivity(Intent(this, ObservationsActivity::class.java))
                    true
                }
                R.id.menu_settings -> {
                    // Handle settings menu item
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
        distanceToHotspot.visibility = View.GONE

        // Select the clicked marker (show hotspot details)
        selectedMarker?.setIcon(customMarkerBird(this,100))
        selectedMarker = marker
        selectedMarker?.setIcon(createCustomMarker())
        selectedMarker?.zIndex = 2.0f

        // Update the hotspot name in the TextView
        val hotspotName = marker.title
        selectedHotspot.text = hotspotName

        hotspotDetailsFrameLayout.visibility = View.VISIBLE
        selectedHotspot.visibility = View.VISIBLE
        hotspotDirectionBtn.visibility = View.VISIBLE

        val unitPreference = sharedPreferences.getBoolean("isMetric", true)

        // Calculate and display the distance
        val origin = location?.let { LatLng(it.latitude, it.longitude) }
        if (origin != null) {
            val distance = calculateDistance(origin, marker.position)

            // Update the distance based on user preferences
            val formattedDistance = if (unitPreference) {
                if (distance > 1000) {
                    val distanceInKm = distance / 1000.0
                    String.format("%.2f km", distanceInKm)
                } else {
                    String.format("%.2f meters", distance)
                }
            } else {
                // Convert meters to miles
                val miles = distance * 0.000621371
                String.format("%.2f mi", miles)
            }

            distanceToHotspot.text = "Distance: $formattedDistance"
            distanceToHotspot.visibility = View.VISIBLE
            val builder = LatLngBounds.builder()
            builder.include(origin)
            builder.include(marker.position)
            val bounds = builder.build()

            // Move the camera to focus on the bounding box
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)
        } else {
            distanceToHotspot.visibility = View.GONE
        }


        hotspotDirectionBtn.setOnClickListener {
            if (isNavigating) {
                // User is navigating; stop the navigation
                isNavigating = false
                removeRoute()
                resetDirectionButton()
                stopLocationUpdates()
            } else {
                // User is not navigating; start navigation
                isNavigating = true
                hotspotDirectionBtn.apply {
                    text = "Stop"
                    setBackgroundColor(Color.RED)
                }
                startLocationUpdates()
                // Calculate and display the route to the selected hotspot
                if (origin != null) {
                    calculateAndDisplayRoute(origin, marker.position)
                }
            }
        }
    }

    private fun resetDirectionButton() {
        hotspotDirectionBtn.apply {
            text = "Get Directions"
            setBackgroundColor(ContextCompat.getColor(this@HotspotsActivity, R.color.primary_blue))
        }
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
        // link : https://documenter.getpostman.com/view/664302/S1ENwy59#c9947c5c-2dce-4c6d-9911-7d702235506c
        // Build URL
        val eBirdAPIUrl ="https://api.ebird.org/v2/ref/hotspot/geo?lat=${location.latitude}&lng=${location.longitude}&dist=${convertedDistance}&fmt=json"


        //The following code was taken and modified from the OPEN SOURCE CODING (INTERMEDIATE) MODULE MANUAL 2023
        //Author : The Independent Institute of Education (The IIE)
        //Unpublished

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
                    Log.d("MyApp", "Response Code: $responseCode")
                    // Check if the request was successful (HTTP status code 200)
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Fetch data from the connection
                        val inputStream = connection.inputStream
                        val urlData = inputStream.bufferedReader().use { it.readText() }
                        Log.d("MyApp", "Response Data: $urlData")

                        // Parse JSON data
                        try {
                            val responseString = urlData.trim() // Trim any leading/trailing whitespace
                            Log.d("ResponseString", responseString)

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
                if (result != null) {
                    // Remove the previous route if it exists
                    removeRoute()

                    // Draw the route on the map
                    val decodedPath = decodePolyline(result.routes[0].overviewPolyline.encodedPath)

                    runOnUiThread {
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
                } else {
                    // Handle error or display a message
                    runOnUiThread {
                        Toast.makeText(this@HotspotsActivity, "Error calculating route", Toast.LENGTH_SHORT).show()
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
        paint.color = Color.parseColor("#004575")
        paint.alpha = 150
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        // Draw a blue circle on the canvas
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat(), paint)

        val icon = BitmapFactory.decodeResource(resources, R.drawable.dove)
        val iconSize = circleRadius * 2
        val iconLeft = circleRadius - iconSize / 2
        val iconTop = circleRadius - iconSize / 2
        val iconRect = Rect(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        canvas.drawBitmap(icon, null, iconRect, null)

        // Create a BitmapDescriptor from the custom marker image
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // clearing the Route
    private fun removeRoute() {
        currentRoute?.remove()
        currentRoute = null
    }
}