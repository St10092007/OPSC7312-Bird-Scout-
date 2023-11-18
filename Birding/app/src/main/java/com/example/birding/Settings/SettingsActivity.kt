package com.example.birding.Settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import com.example.birding.Authentication.LoginActivity
import com.example.birding.Core.User
import com.example.birding.Home.HomeActivity
import com.example.birding.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    // Declare UI elements and variables
//    private lateinit var myObservationsButton: AppCompatButton
    private lateinit var accountSettingsButton: AppCompatButton
    private lateinit var personalInformationButton: AppCompatButton
    private lateinit var privacyPolicyButton: AppCompatButton
    private lateinit var returnButton: AppCompatButton
    private lateinit var logoutButton: AppCompatButton

    private lateinit var displayNameTextView: TextView
    private lateinit var emailTextView: TextView
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val userReference = database.reference.child("Users").child(auth.currentUser?.uid.toString())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize buttons
//        myObservationsButton = findViewById(R.id.btnMyObservations)
        accountSettingsButton = findViewById(R.id.btnAccountSettings)
        personalInformationButton = findViewById(R.id.btnPersonalInformation)
        privacyPolicyButton = findViewById(R.id.btnPrivacyPolicy)
        returnButton = findViewById(R.id.btnReturn)
        logoutButton = findViewById(R.id.btnlogout)

        displayNameTextView = findViewById(R.id.displayNameTextView)
        emailTextView = findViewById(R.id.emailTextView)


        // Set click listeners for each button
//        myObservationsButton.setOnClickListener { onMyObservationsClicked() }
        accountSettingsButton.setOnClickListener { onAccountSettingsClicked() }
        personalInformationButton.setOnClickListener { onPersonalInformationClicked() }
        privacyPolicyButton.setOnClickListener { onPrivacyPolicyClicked() }
        returnButton.setOnClickListener { onReturnClicked() }
        logoutButton.setOnClickListener { onLogoutClicked() }


        setUserInfoFromFirebase()
    }

    private fun onLogoutClicked() {
        auth.signOut() // Sign the user out from Firebase

        // Start an intent to open the login activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setUserInfoFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            val email = currentUser.email
            emailTextView.text = email
        }

        // Retrieve user information from Firebase Realtime Database
        userReference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val user = dataSnapshot.getValue(User::class.java)
                user?.let {

                    // User is signed in
                    val displayName = "${it.name} ${it.surname}"

                    // Set user information to TextViews
                    displayNameTextView.text = displayName

                }
            }
        }
    }

    private fun onReturnClicked() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

//    private fun onMyObservationsClicked() {
//
//
//    }

    private fun onAccountSettingsClicked() {
        val intent = Intent(this, AccountSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun onPersonalInformationClicked() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun onPrivacyPolicyClicked() {
        val intent = Intent(this, PrivacyPolicyActivity::class.java)
        startActivity(intent)
    }

}
