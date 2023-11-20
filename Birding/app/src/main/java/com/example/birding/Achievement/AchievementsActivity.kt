package com.example.birding.Achievement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.Home.HomeActivity
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.Observation.ObservationAdapter
import com.example.birding.Observations.BirdObservation
import com.example.birding.Observations.ObservationDeleteListener
import com.example.birding.Observations.ObservationManager
import com.example.birding.Observations.ObservationManager.Companion.getObservationCount
import com.example.birding.Observations.ObservationManager.Companion.incrementObservationCount
import com.example.birding.Observations.ObservationManager.Companion.setObservationCount
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.example.birding.Settings.SettingsActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class AchievementsActivity : AppCompatActivity() , ObservationDeleteListener {
    private lateinit var recyclerView: RecyclerView
    lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var observationManager: ObservationManager
    private lateinit var observationAdapter: ObservationAdapter
    private lateinit var achievementsAdapter: AchievementsAdapter
    private val observationsList: MutableList<BirdObservation> = mutableListOf()
    private lateinit var progressBar: ProgressBar


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.example.birding.Observations.ObservationDeleted" -> {
                    // Update achievements when an observation is deleted
                    updateAchievementStatus()
                }
                "com.example.birding.Observations.ObservationAdded" -> {
                    // Update achievements when a new observation is added
                    updateAchievementStatus()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)


        progressBar = findViewById(R.id.progressBar)
        loadObservationsForAchievements()

        // Register the receiver in the onCreate method
        val filter = IntentFilter().apply {
            addAction("com.example.birding.Observations.ObservationDeleted")
            addAction("com.example.birding.Observations.ObservationAdded")
        }
        registerReceiver(receiver, filter)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Retrieve achievement status from Intent
//        val position = intent.getIntExtra("position", -1)
        val observationCount = observationsList.size

//        if (position != -1) {
//            updateAchievementStatus(position, observationCount)
//        }


        // Initialize RecyclerView
        recyclerView = findViewById(R.id.rvAchievements)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Ensure that observationAdapter is initialized (you may need to adjust this based on your data source)
        val geocoder = Geocoder(this, Locale.getDefault())
        observationAdapter = ObservationAdapter(observationsList, geocoder, this)

        achievementsAdapter = AchievementsAdapter(AchievementsAdapter.achievementsList, observationCount)
        recyclerView.adapter = achievementsAdapter

        // Check if the achievements list is empty
        if (AchievementsAdapter.achievementsList.isEmpty()) {
            showEmptyState()
        }

//        // If achievement status is received, update the achievements
//        if (position != -1) {
//            updateAchievementStatus(position, observationCount)
//        }
//        updateAchievementStatus()
//        updateAchievementStatus(observationsList.count())
        setupBottomNavigation()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver in the onDestroy method
        unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        // Fetch the latest data from Firebase or update the data as needed
        updateAchievementStatus()
    }

    private fun loadObservationsForAchievements() {
        // Show a progress indicator if needed
         progressBar.visibility = View.VISIBLE

        val userUid = getCurrentUserUid()
        val databaseReference = FirebaseDatabase.getInstance().reference
            .child("Observations")
            .child(userUid)

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Handle data changes
                val newObservationsList = mutableListOf<BirdObservation>()

                for (observationSnapshot in dataSnapshot.children) {
                    // Parse observation data from the snapshot
                    val observationId = observationSnapshot.child("observationId").getValue(String::class.java)
                    val species = observationSnapshot.child("species").getValue(String::class.java)
                    val dateTime = observationSnapshot.child("dateTime").getValue(String::class.java)
                    val latitude = observationSnapshot.child("location").child("latitude").getValue(Double::class.java)
                    val longitude = observationSnapshot.child("location").child("longitude").getValue(Double::class.java)
                    val notes = observationSnapshot.child("notes").getValue(String::class.java)
                    val fullName = observationSnapshot.child("fullName").getValue(String::class.java)
                    val observationType = observationSnapshot.child("observationType").getValue(String::class.java) ?: "Unknown"

                    if (observationId != null && species != null && dateTime != null && latitude != null && longitude != null && notes != null && observationType != null && fullName!=null) {
                        val location = LatLng(latitude, longitude)
                        val observation = BirdObservation(observationId, null, species, dateTime, location, notes, observationType,fullName)
                        newObservationsList.add(observation)
                        incrementObservationCount()
                    }
                    setObservationCount(newObservationsList.size)

                    Log.d("AchievementActivity", "Total Observations Count: ${newObservationsList.size}")

                    updateAchievementStatus(newObservationsList.size)
                    Log.d("AchievementActivity", "Updated Observations Count: ${newObservationsList.size}")

                    achievementsAdapter.notifyDataSetChanged()


                }

                // Update achievements based on the new observations list
//                updateAchievementStatus()


                // Optionally, hide a progress indicator if needed
                 progressBar.visibility = View.GONE
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors or show a message
                // progressBar.visibility = View.GONE
                Log.e("FirebaseError", databaseError.message)
                Toast.makeText(applicationContext, "Database Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getCurrentUserUid(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid ?: ""
    }
    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_achievement

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
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()

                    true
                }
                R.id.menu_hotspots -> {
                    startActivity(Intent(this, HotspotsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_observations -> {
                    startActivity(Intent(this, ObservationsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_achievement -> {
                    true
                }
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateAchievementStatus(position: Int, observationCount: Int) {
        // Update achievement status in the adapter and notify the change
        val adapter = recyclerView.adapter as AchievementsAdapter
        adapter.markAchievementAsComplete(position, observationCount)
    }

    fun updateAchievementStatus() {
        // Call the function to load observations from Firebase in AchievementsActivity
        loadObservationsForAchievements()
    }

    private fun updateAchievementStatus(observationsListCount : Int) {

        // Update achievement status based on the new observations list
        updateAchievementStatus(0, observationsListCount)
        updateAchievementStatus(1, observationsListCount)
        updateAchievementStatus(2, observationsListCount)
        updateAchievementStatus(3, observationsListCount)
        updateAchievementStatus(4, observationsListCount)
        updateAchievementStatus(5, observationsListCount)
        updateAchievementStatus(6, observationsListCount)
        updateAchievementStatus(7, observationsListCount)
        updateAchievementStatus(8, observationsListCount)
        updateAchievementStatus(9, observationsListCount)

        Log.d("AchievementsActivity", "updateAchievementStatus() called with observationCount: $observationsListCount")
    }

    // Function to show an empty state if there are no achievements
    private fun showEmptyState() {
        // You can customize this based on your design
        Toast.makeText(this, "No achievements found.", Toast.LENGTH_SHORT).show()
    }

    override fun onDelete(observationId: String) {
        TODO("Not yet implemented")
    }


}
