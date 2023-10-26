package com.example.birding.Settings

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.widget.TooltipCompat
import com.example.birding.Authentication.LoginActivity
import com.example.birding.Home.HomeActivity
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    // Declare UI elements and variables
    private lateinit var radioGroup: RadioGroup
    private lateinit var distanceSeekBar: SeekBar
    private lateinit var distanceTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var profileButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        radioGroup = findViewById(R.id.rgMeasurementUnits)
        distanceTextView = findViewById(R.id.tvDistanceValue)
        distanceSeekBar = findViewById(R.id.sbDistanceSeekBar)
        logoutButton = findViewById(R.id.btnLogout)
        profileButton = findViewById(R.id.btnProfile)

        auth = FirebaseAuth.getInstance()

        val savedDistance = sharedPreferences.getInt("selectedDistance", 0)
        distanceSeekBar.progress = savedDistance
        distanceTextView.text = "$savedDistance ${getSelectedUnit()}"

        // SeekBar listener
        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                distanceTextView.text = "$progress ${getSelectedUnit()}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Store the selected distance
                sharedPreferences.edit().putInt("selectedDistance", seekBar?.progress ?: 0).apply()
            }
        })

        // Set the default unit and distance
        val savedUnit = getSelectedUnit()
        val radioButtonId = when (savedUnit) {
            "Kilometers" -> R.id.rbMetric
            "Miles" -> R.id.rbImperial
            else -> R.id.rbMetric // Default to Metric
        }
        radioGroup.check(radioButtonId)

        // RadioGroup listener for unit selection
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedUnit = when (checkedId) {
                R.id.rbMetric -> "Kilometers"
                R.id.rbImperial -> "Miles"
                else -> "Kilometers" // Default to Kilometers
            }

            if (selectedUnit == "Kilometers") {
                distanceSeekBar.max = 400
            } else {
                distanceSeekBar.max = 248
            }

            val currentValue = distanceSeekBar.progress
            val currentUnit = getSelectedUnit()

            if (selectedUnit != currentUnit) {
                // Unit changed, convert the current value to the new unit
                val newValue = if (selectedUnit == "Kilometers") {
                    milesToKilometers(currentValue)
                } else {
                    kilometersToMiles(currentValue)
                }
                distanceSeekBar.progress = newValue
                distanceTextView.text = "$newValue $selectedUnit"
                setUnit(selectedUnit)
            }
            sharedPreferences.edit().putInt(MAX_DISTANCE_PREFERENCE_KEY, distanceSeekBar?.progress ?: 0).apply()

        }

        // Set up bottom navigation
        setupBottomNavigation()

        // Handle the logout button click
        logoutButton.setOnClickListener {
            logout()
        }

        // Handle the profile button click
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    // Function to set the selected unit in SharedPreferences
    private fun setUnit(unit: String) {
        sharedPreferences.edit().putString("selectedUnit", unit).apply()
    }
    // Function to get the selected unit from SharedPreferences
    private fun getSelectedUnit(): String {
        return sharedPreferences.getString("selectedUnit", "Kilometers") ?: "Kilometers"
    }

    // Conversion functions for distance units
    private fun milesToKilometers(miles: Int): Int {
        return (miles * 1.60934).toInt()
    }

    private fun kilometersToMiles(kilometers: Int): Int {
        return (kilometers / 1.60934).toInt()
    }

    // Function to handle user logout
    private fun logout() {
        auth.signOut() // Sign the user out from Firebase

        // Start an intent to open the login activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_settings

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
                    startActivity(Intent(this, HotspotsActivity::class.java))
                    true
                }
                R.id.menu_observations -> {
                    startActivity(Intent(this, ObservationsActivity::class.java))
                    true
                }
                R.id.menu_settings -> {
                    true
                }
                else -> false
            }
        }
    }
    companion object {
        const val PREFERENCES_NAME = "userPreferences"
        const val IS_METRIC_PREFERENCE_KEY = "selectedUnit"
        const val MAX_DISTANCE_PREFERENCE_KEY = "selectedDistance"
    }
}
