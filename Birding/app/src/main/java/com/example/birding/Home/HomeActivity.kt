package com.example.birding.Home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import com.example.birding.Achievement.AchievementsActivity
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.Observations.BirdObservation
import com.example.birding.Observations.ObservationManager
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.example.birding.Settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var exploreButton : Button
    private lateinit var observationsButton : Button
    private lateinit var settingsButton : Button
    private val observationsList: MutableList<BirdObservation> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize UI elements
        exploreButton = findViewById(R.id.btnExploreHotspots)
        observationsButton = findViewById(R.id.btnObservations)
        settingsButton = findViewById(R.id.btnSettings)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set click listeners for buttons
        exploreButton.setOnClickListener {
            startActivity(Intent(this, HotspotsActivity::class.java))
        }

        observationsButton.setOnClickListener {
            startActivity(Intent(this, ObservationsActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_home

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
                    val intent = Intent(this, AchievementsActivity::class.java)
//                    intent.putExtra("observationCount", ObservationManager.getObservationCount())
                    startActivity(intent)
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
}
