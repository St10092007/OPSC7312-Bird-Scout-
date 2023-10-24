package com.example.birding.Home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.example.birding.Settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

lateinit var bottomNavigationView: BottomNavigationView
private lateinit var exploreButton : Button
private lateinit var myObservationsButton : Button

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize UI elements
        exploreButton = findViewById(R.id.btnExplore)
        myObservationsButton = findViewById(R.id.btnMyObservations)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)


        // Set click listeners for buttons
        exploreButton.setOnClickListener {
            startActivity(Intent(this, HotspotsActivity::class.java))
        }

        myObservationsButton.setOnClickListener {
            startActivity(Intent(this, ObservationsActivity::class.java))
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_home

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
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

}