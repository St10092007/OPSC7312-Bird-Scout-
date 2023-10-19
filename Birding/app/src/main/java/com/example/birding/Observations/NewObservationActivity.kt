package com.example.birding.Observations

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.birding.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

private lateinit var tvTitle: TextView
private lateinit var speciesName: EditText
private lateinit var date: TextView
private lateinit var dateInput: EditText
private lateinit var locationTv: TextView
private lateinit var location: EditText
private lateinit var notes: TextView
private lateinit var birdNotes: EditText
private lateinit var backBtn: Button
private lateinit var saveBtn: Button


private lateinit var database: FirebaseDatabase
private lateinit var fusedLocationClient: FusedLocationProviderClient
private lateinit var dbReference: DatabaseReference

class NewObservationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_observation)

        tvTitle = findViewById(R.id.tvTitle)
        speciesName = findViewById(R.id.speciesName)
        date = findViewById(R.id.date)
        dateInput = findViewById(R.id.dateInput)
        locationTv = findViewById(R.id.locationTv)
        location = findViewById(R.id.location)
        notes = findViewById(R.id.notes)
        birdNotes = findViewById(R.id.bird_notes_)
        backBtn = findViewById(R.id.backBtn)
        saveBtn = findViewById(R.id.saveBtn)

        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("Observations")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        backBtn.setOnClickListener {
            val intent = Intent(this, ObservationsActivity::class.java)
            startActivity(intent)
        }
        saveBtn.setOnClickListener {
            // Get user inputs
            val species = speciesName.text.toString()
            val formattedDate = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US).format(Date())
            val notes = birdNotes.text.toString()

            // Fetch the user's current location
            fetchLocation { location ->
                val latitude = location?.latitude ?: 0.0
                val longitude = location?.longitude ?: 0.0


                val birdLocation = LatLng(latitude, longitude)


                val sighting = BirdObservation(species, Date(), birdLocation, notes)

                // Check if the user is authenticated
                val user = FirebaseAuth.getInstance().currentUser
                user?.let { currentUser ->
                    // Use the user's UID as the child key
                    val userId = currentUser.uid
                    val dbReference = dbReference.child(userId)


                    val sightingRef = dbReference.push()
                    sightingRef.setValue(sighting).addOnSuccessListener {
                        // Successfully added
                        val intent = Intent(this, ObservationsActivity::class.java)
                        startActivity(intent)
                        Toast.makeText(this, "bird observation added Successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun fetchLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                callback(location)
            }
        }
    }


}