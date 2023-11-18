package com.example.birding.Settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.example.birding.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var tvPrivacyPolicyHeader: TextView
    private lateinit var tvPrivacyPolicyContent: TextView
    private lateinit var btnViewFullPrivacyPolicy: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        tvPrivacyPolicyHeader = findViewById(R.id.tvPrivacyPolicyHeader)
        tvPrivacyPolicyContent = findViewById(R.id.tvPrivacyPolicyContent)
        btnViewFullPrivacyPolicy = findViewById(R.id.btnViewFullPrivacyPolicy)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loadPrivacyPolicyContent()

        btnViewFullPrivacyPolicy.setOnClickListener {
            val privacyPolicyUrl = Uri.parse("https://www.termsfeed.com/live/ecc9dad9-96d4-4f60-bb7c-29ba940b10aa")

            val browserIntent = Intent(Intent.ACTION_VIEW, privacyPolicyUrl)

            try {
                startActivity(browserIntent)
            } catch (e: ActivityNotFoundException) {
                // Handle the case where no browser is installed
                Toast.makeText(this, "No browser installed", Toast.LENGTH_SHORT).show()
            }
        }

    }
    override fun onSupportNavigateUp(): Boolean {
        // Handle the back button press
        onBackPressed()
        return true
    }

    private fun loadPrivacyPolicyContent() {
        try {
            // Read the content of the Privacy Policy file
            val inputStream: InputStream = resources.openRawResource(R.raw.privacy_policy)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                content.append(line).append("\n")
            }

            // Set the content to the TextView
            tvPrivacyPolicyContent.text = content.toString()

            // Close the reader and the input stream
            reader.close()
            inputStream.close()

        } catch (e: IOException) {
            e.printStackTrace()
            // Handle exceptions or errors loading the content
        }
    }
}