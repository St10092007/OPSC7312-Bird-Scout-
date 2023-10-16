package com.example.birding.Home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.birding.Hotspot.HotspotsActivity
import com.example.birding.R
import com.google.android.material.bottomnavigation.BottomNavigationView

lateinit var bottomNavigationView: BottomNavigationView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_home

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
//                    startActivity(Intent(this, observations::class.java))
                    true
                }
                R.id.menu_settings -> {
                    // Handle Goals menu item
//                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

}