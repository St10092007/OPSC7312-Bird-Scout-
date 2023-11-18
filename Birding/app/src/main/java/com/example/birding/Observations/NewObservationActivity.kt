package com.example.birding.Observations

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.birding.R
import com.example.birding.Settings.AccountSettingsActivity
import com.example.birding.Settings.AccountSettingsActivity.Companion.IS_METRIC_PREFERENCE_KEY
import com.example.birding.Settings.AccountSettingsActivity.Companion.MAX_DISTANCE_PREFERENCE_KEY
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import org.json.JSONArray
import org.json.JSONException
import java.lang.Double.max
import java.lang.Double.min
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread


class NewObservationActivity : AppCompatActivity() {
    private lateinit var tvTitle: TextView
    private lateinit var etSpeciesName: EditText
    private lateinit var tvDate: TextView
    private lateinit var etDateInput: EditText
    private lateinit var tvLocation: TextView
    private lateinit var tvNotes: TextView
    private lateinit var etBirdNotes: EditText
    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbReference: DatabaseReference
    private val locationPermissionCode = 100
    private lateinit var selectedImage: ImageView
    private var selectedImageBitmap: Bitmap? = null
    private lateinit var observationTypeSpinner: Spinner
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var etLocation: TextView
    private lateinit var placesClient: PlacesClient
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var autocompleteSessionToken: AutocompleteSessionToken? = null

    private var selectedPlace: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("NewObservationActivity", "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_observation)

        // Initialize UI elements
        tvTitle = findViewById(R.id.tvTitle)
        etSpeciesName = findViewById(R.id.etSpeciesName)
        tvDate = findViewById(R.id.tvDate)
        etDateInput = findViewById(R.id.etDateInput)
        tvLocation = findViewById(R.id.tvLocation)
        etLocation = findViewById(R.id.etLocation)
        tvNotes = findViewById(R.id.tvNotes)
        etBirdNotes = findViewById(R.id.etBirdNotes)
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        selectedImage = findViewById(R.id.IvselectedImage)
        sharedPreferences = getSharedPreferences(AccountSettingsActivity.PREFERENCES_NAME, MODE_PRIVATE)
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("Observations")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        observationTypeSpinner = findViewById(R.id.observationTypeSpinner)
        val observationTypes = resources.getStringArray(R.array.observation_types)

        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, observationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        observationTypeSpinner.adapter = adapter


        observationTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    // Handle the selected item here
                    val selectedType = parentView?.getItemAtPosition(position).toString()
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // Do nothing here
                }
            }



        //image
        val imageButton: Button = findViewById(R.id.imageButton)
        imageButton.setOnClickListener {
            openImagePicker()
        }

        // Button click listeners
        btnBack.setOnClickListener {
            val intent = Intent(this, ObservationsActivity::class.java)
            startActivity(intent)
        }
        btnSave.setOnClickListener {
            saveObservation()
        }
        etDateInput.setOnClickListener {
            showDateTimePicker()
        }


        etLocation.isEnabled = false
        fetchLocation { location ->
            Log.d("NewObservationActivity", "fetchLocation callback called")
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    val addresses: List<Address>? =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val streetAddress = address.getAddressLine(0)
                        runOnUiThread {
//                            etLocation.text = streetAddress
                        }
                    } else {
                        Log.d("NewObservationActivity", "Location is null")
                        runOnUiThread {
//                            etLocation.text = "Location not found"
                        }
                    }
                } catch (e: IOException) {
                    Log.e("NewObservationActivity", "Exception in onCreate: ${e.message}")
                    e.printStackTrace()
                    runOnUiThread {
//                        etLocation.text = "Location not found"
                    }
                }
            } else {
                runOnUiThread {
//                    etLocation.text = "Location not found"
                }
            }
        }
//        // Check for location permissions and request them if necessary
//        if (!hasLocationPermission()) {
//            requestLocationPermission()
//        } else {
//            // Location permission is granted; fetch user's last location and eBird hotspots
//            fetchLocation { location ->
//                location?.let {
//                    initAutocompleteFragment((LatLng(location.latitude, location.longitude)))
//                }
//            }
//        }
        initAutocompleteFragment()
        Places.initializeWithNewPlacesApiEnabled(applicationContext, getString(R.string.MAPS_API_KEY))
        placesClient = Places.createClient(this)

    }
    private fun initAutocompleteFragment() {
        // Initialize Places API
        Places.initializeWithNewPlacesApiEnabled(applicationContext, getString(R.string.MAPS_API_KEY))
//        AutocompleteSessionToken.newInstance()
        // Set up AutocompleteSupportFragment
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment

        // Configure AutocompleteFragment settings...
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
        )

        val placeTypes = listOf(
            "natural_feature",
            "park",
            "route",
            "colloquial_area",
            "street_address",

            )

        autocompleteFragment.setTypesFilter(placeTypes)
        autocompleteFragment.setCountries("ZA")
//        autocompleteFragment.setSessionToken(AutocompleteSessionToken.newInstance())


        // Specify the callback for when a place is selected
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            // Handle place selection...

            override fun onError(p0: Status) {
                Log.e("PlaceSelection", "Error: ${p0.statusMessage}")
                Log.d("PlaceSelection", "Error Code: ${p0.statusCode}")
                Log.d("PlaceSelection", "Error Status: ${p0}")

            }

            override fun onPlaceSelected(place: Place) {
                selectedPlace = place
                // Handle the selected place
                val selectedLocation = place.address
                Log.d("PlaceSelection", "Selected Location: $selectedLocation")

                etLocation.text = selectedLocation
                // Update the map or UI with the selected location if needed
                // You can add other UI-related operations here

                // Log to verify that the UI is updated
                Log.d("PlaceSelection", "UI Updated with selected location: $selectedLocation")
            }
        })
    }


    // Separate function to update UI with the selected location


    private fun saveObservation() {
        val species = etSpeciesName.text.toString().trim()
        val notes = etBirdNotes.text.toString().trim()

        if (species.isEmpty()) {
            etSpeciesName.error = "Species Name cannot be empty"
            return
        }

        val date = etDateInput.text.toString().trim()
        if (date.isEmpty()) {
            etDateInput.error = "Date cannot be empty"
            return
        }

        // Check if the user has selected an image, otherwise use the default dove icon
        val selectedImageString = if (selectedImageBitmap != null) {
            encodeBitmapToBase64(selectedImageBitmap!!)
        } else {
            // Use the default dove icon
            val doveIcon = BitmapFactory.decodeResource(resources, R.drawable.dove)
            encodeBitmapToBase64(doveIcon)
        }

        // Get the selected location from the stored Place
        val selectedPlace = selectedPlace

        val location = etLocation.text.toString()

        // Check if a location is selected
        if (selectedPlace != null) {
            val observationType = observationTypeSpinner.selectedItem.toString()

            // Create a LatLng object from the selectedPlace's latitude and longitude
            val selectedLocation = LatLng(selectedPlace.latLng?.latitude ?: 0.0, selectedPlace.latLng?.longitude ?: 0.0)

            // Generate a unique ID for the observation
            val observationId = UUID.randomUUID().toString()


            // Format the ID string as "Observation: #012345"
            val formattedId = String.format("%07d", observationId.hashCode() and 0xffffff)

            val sighting = BirdObservation(
                formattedId,
                selectedImageString,
                species,
                date,
                selectedLocation,
                notes,
                observationType
            )
            saveObservationToFirebase(sighting)
        } else {
            // Handle the case where no location is selected
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveObservationToFirebase(sighting: BirdObservation) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            val userId = currentUser.uid
            val dbReference = database.getReference("Observations").child(userId).child(sighting.observationId)

            dbReference.setValue(sighting).addOnSuccessListener {
                val intent = Intent(this, ObservationsActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Bird observation added successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add bird observation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun deserializeLocation(locationString: String): LatLng {
        val locationParts = locationString.split(",")
        val latitude = locationParts[0].toDoubleOrNull() ?: 0.0
        val longitude = locationParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        return LatLng(latitude, longitude)
    }



    // Function to fetch the location
    private fun fetchLocation(callback: (LatLng?) -> Unit) {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val latLng = location?.run { LatLng(latitude, longitude) }
                    callback(latLng)
                }
            } catch (securityException: SecurityException) {
                securityException.printStackTrace()
                Toast.makeText(this, "SecurityException: Location permission denied.", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        } else {
            requestLocationPermission()
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show()
            callback(null)
        }
    }

    // Function to request location permission
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    // Function to check if the location permission is granted
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

    // This was taken from YouTube
    // Author: Android Coding
    // Link: https://www.youtube.com/watch?v=Xf5K2Ls07cs
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            val bitmap = getBitmapFromUri(imageUri)
            selectedImageBitmap = bitmap
            selectedImage.setImageBitmap(selectedImageBitmap)
        }
    }

    private fun getBitmapFromUri(uri: Uri?): Bitmap? {
        uri?.let {
            return try {
                val inputStream = contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
        return null
    }

    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 100
    }

    // Function to show date and time picker
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        // Create a DatePickerDialog to pick the date
        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                // The selected date is stored in the calendar instance
                calendar.set(year, month, dayOfMonth)

                // Create a TimePickerDialog to pick the time
                val timePickerDialog = TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                        // Set the selected time in the calendar instance
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Format the selected date and time
                        val dateFormat = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US)
                        val selectedDateTime = dateFormat.format(calendar.time)

                        // Set the formatted date and time to the dateInput EditText
                        etDateInput.setText(selectedDateTime)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

}
