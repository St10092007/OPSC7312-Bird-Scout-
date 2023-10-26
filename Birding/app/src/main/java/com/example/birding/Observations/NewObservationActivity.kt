package com.example.birding.Observations

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.birding.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class NewObservationActivity : AppCompatActivity() {
    private lateinit var tvTitle: TextView
    private lateinit var etSpeciesName: EditText
    private lateinit var tvDate: TextView
    private lateinit var etDateInput: EditText
    private lateinit var tvLocation: TextView
    private lateinit var etLocation: EditText
    private lateinit var tvNotes: TextView
    private lateinit var etBirdNotes: EditText
    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbReference: DatabaseReference
    private val locationPermissionCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
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

        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("Observations")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Button click listeners
        btnBack.setOnClickListener {
            val intent = Intent(this, ObservationsActivity::class.java)
            startActivity(intent)
        }
        btnSave.setOnClickListener {
            saveObservation()
        }

        val currentDateTime = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US).format(Calendar.getInstance().time)
        etDateInput.setText(currentDateTime)

        etLocation.isEnabled = false
        fetchLocation { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val streetAddress = address.getAddressLine(0)
                        runOnUiThread {
                            etLocation.setText(streetAddress)
                        }
                    } else {
                        runOnUiThread {
                            etLocation.setText("Location not found")
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        etLocation.setText("Location not found")
                    }
                }
            } else {
                runOnUiThread {
                    etLocation.setText("Location not found")
                }
            }
        }
    }

    // Function to save the bird observation
    private fun saveObservation() {
        val species = etSpeciesName.text.toString()
        val notes = etBirdNotes.text.toString()

        if (species.isEmpty()) {
            etSpeciesName.error = "Species Name cannot be empty"
            return
        }

        // Check if the date is not empty
        val date = etDateInput.text.toString()
        if (date.isEmpty()) {
            etDateInput.error = "Date cannot be empty"
            return
        }

        fetchLocation { location ->
            val birdLocation = location ?: LatLng(0.0, 0.0)
            val date = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US).format(Date())

            // Serialize LatLng to a string
            val locationString = "${birdLocation.latitude},${birdLocation.longitude}"

            // Deserialize location string back to LatLng
            val locationParts = locationString.split(",")
            val latitude = locationParts[0].toDoubleOrNull() ?: 0.0
            val longitude = locationParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val deserializedLocation = LatLng(latitude, longitude)

            val sighting = BirdObservation(species, date, deserializedLocation, notes)

            val user = FirebaseAuth.getInstance().currentUser
            user?.let { currentUser ->
                val userId = currentUser.uid
                val dbReference = database.getReference("Observations").child(userId)

                val sightingRef = dbReference.push()
                sightingRef.setValue(sighting).addOnSuccessListener {
                    val intent = Intent(this, ObservationsActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this, "Bird observation added successfully", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    // Function to show date and time picker
    fun showDateTimePicker(view: View) {
        val calendar = Calendar.getInstance()

        // Create a DatePickerDialog to pick the date
        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                // The selected date is stored in the calendar instance
                calendar.set(year, month, dayOfMonth)

                // Create a TimePickerDialog to pick the time
                val timePickerDialog = TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                        // Set the selected time in the calendar instance
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Format the selected date and time
                        val selectedDateTime = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US).format(calendar.time)

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
