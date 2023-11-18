package com.example.birding.Settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.birding.R
import android.content.Intent
import android.content.SharedPreferences
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.TooltipCompat
import com.example.birding.Authentication.LoginActivity
import com.example.birding.Home.HomeActivity
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.Observations.ObservationsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var distanceSeekBar: SeekBar
    private lateinit var distanceTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var returnButton: AppCompatButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        // Initialize UI elements
        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        radioGroup = findViewById(R.id.rgMeasurementUnits)
        distanceTextView = findViewById(R.id.tvDistanceValue)
        distanceSeekBar = findViewById(R.id.sbDistanceSeekBar)
        returnButton = findViewById(R.id.btnReturn)


        auth = FirebaseAuth.getInstance()

        val savedDistance = sharedPreferences.getInt("selectedDistance", 10)
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

        returnButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
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

    companion object {
        const val PREFERENCES_NAME = "userPreferences"
        const val IS_METRIC_PREFERENCE_KEY = "selectedUnit"
        const val MAX_DISTANCE_PREFERENCE_KEY = "selectedDistance"
    }
}