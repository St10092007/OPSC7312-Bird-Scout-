package com.example.birding.Settings

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.example.birding.Authentication.LoginActivity
import com.example.birding.Core.User
import com.example.birding.Home.HomeActivity
import com.example.birding.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    // Declare UI elements and variables
//    private lateinit var myObservationsButton: AppCompatButton
    private lateinit var accountSettingsButton: AppCompatButton
    private lateinit var personalInformationButton: AppCompatButton
    private lateinit var privacyPolicyButton: AppCompatButton
    private lateinit var returnButton: AppCompatButton
    private lateinit var logoutButton: AppCompatButton
    private lateinit var profileImageView: ImageView
    private lateinit var changeProfilePictureButton: Button

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

        profileImageView = findViewById(R.id.profileImageView)
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton)

        changeProfilePictureButton.setOnClickListener { onChangeProfilePictureClicked() }

        // Set click listeners for each button
//        myObservationsButton.setOnClickListener { onMyObservationsClicked() }
        accountSettingsButton.setOnClickListener { onAccountSettingsClicked() }
        personalInformationButton.setOnClickListener { onPersonalInformationClicked() }
        privacyPolicyButton.setOnClickListener { onPrivacyPolicyClicked() }
        returnButton.setOnClickListener { onReturnClicked() }
        logoutButton.setOnClickListener { onLogoutClicked() }


        setUserInfoFromFirebase()
    }
    // Add this function to handle the profile picture change logic
    private fun onChangeProfilePictureClicked() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    // Override this function to handle the result of picking an image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imagePath = data.data
            try {
                val imageStream = imagePath?.let { contentResolver.openInputStream(it) }
                val byteArray = imageStream?.readBytes()
                imageStream?.close()

                if (byteArray != null) {
                    val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

                    // Update the profile picture in the Firebase Realtime Database
                    updateProfilePicture(base64Image)

                    loadCircularImage(base64Image)

                    // Set the profile picture in the ImageView
                    val decodedString: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    profileImageView.setImageBitmap(decodedBitmap)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun loadCircularImage(base64Image: String) {
        val decodedString: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        // Apply circular transformation using Glide
        Glide.with(this)
            .load(decodedBitmap)
            .circleCrop()  // Apply circular transformation
            .into(profileImageView)
    }
    private fun updateProfilePicture(base64Image: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            val userReference = database.reference.child("Users").child(userId)

            userReference.child("profilePictureBase64").setValue(base64Image)
                .addOnSuccessListener {
                    // Profile picture updated successfully
                    currentUser.reload()
                }
                .addOnFailureListener { e ->
                    // Handle the failure
                }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
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

                    // Set the profile picture from base64 string
                    if (!it.profilePictureBase64.isNullOrEmpty()) {
                        val decodedString: ByteArray = Base64.decode(it.profilePictureBase64, Base64.DEFAULT)

                        // Apply circular transformation using Glide
                        Glide.with(this)
                            .asBitmap()
                            .load(decodedString)
                            .circleCrop()  // Apply circular transformation
                            .into(profileImageView)
                    }
//                    if (!it.profilePictureBase64.isNullOrEmpty()) {
//                        val decodedString: ByteArray = Base64.decode(it.profilePictureBase64, Base64.DEFAULT)
//                        val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
//                        profileImageView.setImageBitmap(decodedBitmap)
//                    }

                }
            }
        }
    }

    private fun onReturnClicked() {
        val intent = Intent(this, HomeActivity::class.java)
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
