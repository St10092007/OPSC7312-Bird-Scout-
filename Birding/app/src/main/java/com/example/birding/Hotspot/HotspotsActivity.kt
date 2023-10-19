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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import com.example.birding.Home.HomeActivity
import com.example.birding.R
import com.example.birding.Settings.SettingsActivity
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

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
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private var isNavigating = false
    private var isMetric: Boolean = true
    private var maxDistance: Int = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotspots)
        mapView = findViewById(R.id.mapView)
        selectedHotspot = findViewById(R.id.selectedHotspot)
        hotspotDirectionBtn = findViewById(R.id.hotspotDirectionBtn)
        hotspotDetailsFrameLayout = findViewById(R.id.hotspotDetailsFrameLayout)
        distanceToHotspot = findViewById(R.id.distanceToHotspot)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Check for location permissions and request them if necessary
        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            // Location permission is granted; fetch user's last location and eBird hotspots
            fetchLastLocation { location ->
                location?.let {
                    fetchEBirdHotspots(LatLng(location.latitude, location.longitude))
                    loadPreferences()
                }
            }
        }
        // navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_hotspots
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.menu_hotspots -> {
                    // Handle hotspots menu item
                    // already on hotspots
                    true
                }
                R.id.menu_observations -> {
                    // Handle observations menu item
//                    startActivity(Intent(this, observationsActivity::class.java))
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

    private fun loadPreferences() {
        val settings = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE)
        isMetric = settings.getBoolean(SettingsActivity.IS_METRIC_PREFERENCE_KEY, true)
        maxDistance = settings.getInt(SettingsActivity.MAX_DISTANCE_PREFERENCE_KEY, 50)
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

                // Set a click listener for the "My Location" button
                googleMap.setOnMyLocationButtonClickListener {
                    centerMapOnUserLocation()
                    true
                }
            } catch (securityException: SecurityException) {
                // Handle the SecurityException, typically by requesting permissions or displaying a message to the user
            }
        }
    }

    // User Interaction with Markers
    private fun toggleHotspotDetails(marker: Marker) {
        // Check if the clicked marker is the same as the previously selected marker
        if (selectedMarker == marker) {
            // Hide the hotspot details
            selectedMarker?.setIcon(createCustomMarkerbird(this,50))
            hotspotDetailsFrameLayout.visibility = View.GONE
            selectedMarker = null // Deselect the marker
            return
        }

        // Remove the previous route if it exists
        removeCurrentRoute()

        // Hide the distance text
        distanceToHotspot.visibility = View.GONE

        // Select the clicked marker (show hotspot details)
        selectedMarker?.setIcon(createCustomMarkerbird(this,50))
        selectedMarker = marker
        selectedMarker?.setIcon(createCustomMarker())
        selectedMarker?.zIndex = 2.0f

        // Update the hotspot name in the TextView
        val hotspotName = marker.title
        selectedHotspot.text = hotspotName

        hotspotDetailsFrameLayout.visibility = View.VISIBLE
        selectedHotspot.visibility = View.VISIBLE
        hotspotDirectionBtn.visibility = View.VISIBLE

        // Calculate and display the distance
        val origin = location?.let { LatLng(it.latitude, it.longitude) }
        if (origin != null) {
            val distance = calculateDistance(origin, marker.position)

            // Update the distance based on user preferences
            val formattedDistance = if (isMetric) {
                if (distance > 1000) {
                    val distanceInKm = distance / 1000.0
                    String.format("%.2f km", distanceInKm)
                } else {
                    String.format("%.2f meters", distance)
                }
            } else {
                val miles = distance * 0.000621371  // Convert meters to miles
                String.format("%.2f mi", miles)
            }

            distanceToHotspot.text = "Distance: $formattedDistance"
            distanceToHotspot.visibility = View.VISIBLE
        } else {
            distanceToHotspot.visibility = View.GONE
        }


        hotspotDirectionBtn.setOnClickListener {
            if (isNavigating) {
                // User is navigating; stop the navigation
                isNavigating = false
                removeCurrentRoute() // Remove the previous route
                resetDirectionButton() // Reset the button appearance
            } else {
                // User is not navigating; start navigation
                isNavigating = true
                hotspotDirectionBtn.apply {
                    text = "Stop"
                    setBackgroundColor(Color.RED)
                }
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
        val unitPreference = sharedPreferences.getString("measurement_unit", "metric") // Use the correct preference key
        var maxDistance = sharedPreferences.getInt("max_distance", 10)

        // Convert maxDistance to kilometers if the user prefers miles
        val distance = if (unitPreference == "miles") {
            (maxDistance * 1.60934).toInt()
        } else {
            maxDistance
        }

        // Build URL
        val eBirdAPIUrl ="https://api.ebird.org/v2/ref/hotspot/geo?lat=${location.latitude}&lng=${location.longitude}&dist=${distance}&fmt=json"

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
                            Log.d("ResponseString", responseString)

                            if (responseString.startsWith("[")) {
                                // Valid JSON array response
                                val hotspotsArray = JSONArray(responseString)
//                                val locIds = mutableListOf<String>()
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
                                            .icon(createCustomMarkerbird(this,50))
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
    private fun calculateAndDisplayRoute(origin: LatLng, destination: LatLng) {
        val apiKey = "AIzaSyDHTmCbWEXU66wNV7hIIhaBPPJqXjnJX6I"
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
                    removeCurrentRoute()

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
    private fun createCustomMarkerbird(context: Context, targetSize: Int): BitmapDescriptor {
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
    private fun createCustomMarker(): BitmapDescriptor {
        // Adjust the radius as needed
        val circleRadius = 50

        // Create a bitmap with ARGB_8888 configuration
        val bitmap = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888)

        // Create a canvas to draw on the bitmap
        val canvas = Canvas(bitmap)

        // Define the paint properties for a blue circle
        val paint = Paint()
        paint.color = Color.parseColor("#004575") // Use your blue theme color
        paint.alpha = 150 // Adjust the alpha (transparency) as needed
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

    private fun removeCurrentRoute() {
        currentRoute?.remove()
        currentRoute = null
    }
}