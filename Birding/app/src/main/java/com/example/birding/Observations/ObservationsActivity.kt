package com.example.birding.Observations

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.Achievement.AchievementsActivity
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.Observation.ObservationAdapter
import com.example.birding.R
import com.example.birding.Settings.SettingsActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.Serializable
import java.util.*

//Some Portions of this code are modifications based on work created and shared by geeksforgeeks
//Author: geeksforgeeks
//link : https://www.geeksforgeeks.org/android-recyclerview-in-kotlin/

class ObservationsActivity : AppCompatActivity() , ObservationDeleteListener{

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var totalObservationsTextView: TextView


    private lateinit var observationAdapter: ObservationAdapter
    private val observationsList: MutableList<BirdObservation> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_observations)

        val geocoder = Geocoder(this, Locale.getDefault())

        recyclerView = findViewById(R.id.rvObservations)
        progressBar = findViewById(R.id.progressBar)
        observationAdapter = ObservationAdapter(observationsList, geocoder,this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = observationAdapter
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        totalObservationsTextView = findViewById(R.id.totalObservationsTextView)




        val fabAddObservation: FloatingActionButton = findViewById(R.id.fabAddObservation)
        fabAddObservation.setOnClickListener {
            // Handle the click event for adding new observations
            startActivity(Intent(this, NewObservationActivity::class.java))
        }
        Log.d("ObservationsActivity", "onCreate: Start loading observations")
        loadObservationsFromFirebase();

//        // Find the button and set a click listener
//        val btnOpenMap: AppCompatButton = findViewById(R.id.btnOpenMap)
//        btnOpenMap.setOnClickListener {
//            openObservationMapsActivity()
//        }

//        // Access observation counts
//        val observationCount = ObservationManager.getObservationCount()
//        val uniqueLocationsCount = ObservationManager.getUniqueLocationsCount()
//        val uniqueSpeciesCount = ObservationManager.getUniqueSpeciesCount()

        // navigation
        setupBottomNavigation()


        }

    private fun openObservationMapsActivity() {
        // Check if there are observations to show on the map
        if (observationsList.isNotEmpty()) {
            // Open the ObservationMapsActivity with the list of observations
            val intent = Intent(this@ObservationsActivity, ObservationMapsActivity::class.java)
            intent.putExtra("observationsList", observationsList as Serializable)
            startActivity(intent)
        } else {
            // Show a message or handle the case when there are no observations
            Toast.makeText(this, "No observations to show on the map", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDelete(observationId: String) {
        // Show delete confirmation dialog if needed
        showDeleteConfirmationDialog(this, -1, observationId)
    }

    // Add this method to delete observation from Firebase
    private fun deleteObservationFromFirebase(observationId: String) {
        val userUid = getCurrentUserUid()
        val dbReference = FirebaseDatabase.getInstance().getReference("Observations").child(userUid).child(observationId)


        dbReference.removeValue()
            .addOnSuccessListener {
                // Update the ObservationManager when an observation is removed
//                ObservationManager.removeObservation(observationsList.find { it.observationId == observationId } ?: return@addOnSuccessListener)

//                 Send a broadcast to notify achievements activity
                val intent = Intent("com.example.birding.Observations.ObservationDeleted")
                sendBroadcast(intent)

                // Update achievements
                AchievementsActivity().updateAchievementStatus()
                Toast.makeText(applicationContext, "Observation deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, "Failed to delete observation", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_observations

        val observationMenuItem = bottomNavigationView.menu.findItem(R.id.menu_observations)
        val hotspotsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_hotspots)
        val homeMenuItem = bottomNavigationView.menu.findItem(R.id.menu_home)
        val settingsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_settings)
        val achievementMenuItem = bottomNavigationView.menu.findItem(R.id.menu_achievement)


        // Set tooltips for navigation items
        observationMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Observations") }
        hotspotsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Hotspots") }
        homeMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Home") }
        settingsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Settings") }
        achievementMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Achievements") }


        // Bottom navigation item click listener
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    // Home is already selected, do nothing.
                    true
                }
                R.id.menu_hotspots -> {
                    startActivity(Intent(this, HotspotsActivity::class.java))
                    true
                }
                R.id.menu_observations -> {
                    startActivity(Intent(this, ObservationsActivity::class.java))
                    true
                }
                R.id.menu_achievement -> {
                    val observationCount = observationsList.size
//                    ObservationManager.setObservationCount(observationCount)
                    startActivity(Intent(this, AchievementsActivity::class.java))
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


    private fun loadObservationsFromFirebase() {
        progressBar.visibility = View.VISIBLE

        val userUid = getCurrentUserUid()
        val databaseReference = FirebaseDatabase.getInstance().reference
            .child("Observations")
            .child(userUid)

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("ObservationsActivity", "onDataChange called")
                val newObservationsList = mutableListOf<BirdObservation>()

                for (observationSnapshot in dataSnapshot.children) {
                    val observationId = observationSnapshot.child("observationId").getValue(String::class.java)
                    val image = observationSnapshot.child("image").getValue(String::class.java)
                    val species = observationSnapshot.child("species").getValue(String::class.java)
                    val dateTime = observationSnapshot.child("dateTime").getValue(String::class.java)
                    val latitude = observationSnapshot.child("location").child("latitude").getValue(Double::class.java)
                    val longitude = observationSnapshot.child("location").child("longitude").getValue(Double::class.java)
                    val notes = observationSnapshot.child("notes").getValue(String::class.java)
                    val fullName = observationSnapshot.child("fullName").getValue(String::class.java)
                    // Example: Handle null observationType
                    val observationType = observationSnapshot.child("observationType").getValue(String::class.java) ?: "Unknown"

                    if (observationId != null && species != null && dateTime != null && latitude != null && longitude != null && notes != null && observationType != null && fullName!=null) {
                        val location = LatLng(latitude, longitude)
                        val observation = BirdObservation(observationId, image, species, dateTime, location, notes, observationType,fullName)
                        newObservationsList.add(observation)
                    }
                    totalObservationsTextView.text = "Total Observations :${newObservationsList.size}"
                    Log.d("ObservationsActivity", "Observation Data: $species, $dateTime, $latitude, $longitude, $notes, $observationType")
                }

                observationsList.clear()
                observationsList.addAll(newObservationsList)
                observationAdapter.notifyDataSetChanged()

                Log.d("ObservationsActivity", "Observations List Size: ${observationsList.size}")

                if (observationsList.isEmpty()) {
                    showEmptyListCardView()
                    Log.d("ObservationsActivity", "Observations List Size: ${observationsList.size}")

                }
                else{
                    hideEmptyListCardView()
                }
                progressBar.visibility = View.GONE
                Log.d("ObservationsActivity", "onDataChange: End loading observations")
            }


            override fun onCancelled(databaseError: DatabaseError) {
                progressBar.visibility = View.GONE
                Log.e("FirebaseError", databaseError.message)
                Toast.makeText(applicationContext, "Database Error: $databaseError.message", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEmptyListCardView() {
        val emptyListCardView: CardView = findViewById(R.id.emptyListCardView)
        emptyListCardView.visibility = View.VISIBLE
    }

    private fun hideEmptyListCardView() {
        val emptyListCardView: CardView = findViewById(R.id.emptyListCardView)
        emptyListCardView.visibility = View.GONE
    }

    fun showDeleteConfirmationDialog(context: Context, position: Int, observationId: String) {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("Delete Observation")
        alertDialog.setMessage("Are you sure you want to delete this observation?")
        alertDialog.setPositiveButton("Yes") { _, _ ->
            // Remove the item from the adapter when the user confirms deletion
            observationAdapter.removeItem(position)
            // If needed, also delete from the Firebase database
            observationAdapter.deleteObservation(observationId)
            deleteObservationFromFirebase(observationId)
        }
        alertDialog.setNegativeButton("No", null)
        alertDialog.show()
    }


    private fun getCurrentUserUid(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid ?: ""
    }
}
interface ObservationDeleteListener {
    fun onDelete(observationId: String)
}
