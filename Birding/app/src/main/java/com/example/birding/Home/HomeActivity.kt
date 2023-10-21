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

        exploreButton = findViewById(R.id.exploreButton)
        myObservationsButton = findViewById(R.id.myObservationsButton)

        exploreButton.setOnClickListener {
            startActivity(Intent(this, HotspotsActivity::class.java))
        }

        myObservationsButton.setOnClickListener {
            startActivity(Intent(this, ObservationsActivity::class.java))
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_home

        val observationMenuItem = bottomNavigationView.menu.findItem(R.id.menu_observations)
        val hotspotsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_hotspots)
        val homeMenuItem = bottomNavigationView.menu.findItem(R.id.menu_home)
        val settingsMenuItem = bottomNavigationView.menu.findItem(R.id.menu_settings)

        observationMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Observations") }
        hotspotsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Hotspots") }
        homeMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Home") }
        settingsMenuItem.actionView?.let { TooltipCompat.setTooltipText(it, "Settings") }

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    // already in home
                    true
                }
                R.id.menu_hotspots -> {
                    // Handle hotspots menu item
                    startActivity(Intent(this, HotspotsActivity::class.java))
                    true
                }
                R.id.menu_observations -> {
                    // Handle observations menu item
                    startActivity(Intent(this, ObservationsActivity::class.java))
                    true
                }
                R.id.menu_settings -> {
                    // Handle Goals menu item
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

}