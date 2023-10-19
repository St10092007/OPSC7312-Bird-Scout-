package com.example.birding.Settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.birding.Core.User
import com.example.birding.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class ProfileActivity : AppCompatActivity() {

    private lateinit var editProfileName: EditText
    private lateinit var editProfileSurname: EditText
    private lateinit var editProfileEmail: EditText
    private lateinit var saveProfileBtn: Button
    private lateinit var backBtn: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val userReference = database.reference.child("Users").child(auth.currentUser?.uid.toString())
    private var isEditing = false
    private var isChangesSaved = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        editProfileName = findViewById(R.id.editProfileName)
        editProfileSurname = findViewById(R.id.editProfileSurname)
        editProfileEmail = findViewById(R.id.editProfileEmail)
        saveProfileBtn = findViewById(R.id.saveProfileBtn)
        backBtn = findViewById(R.id.backBtn)

        // Fetch and display user data
        fetchUserData()

        // change the button text to "Edit"
        saveProfileBtn.text = "Edit"

        saveProfileBtn.setOnClickListener {
            if (isEditing) {

                updateUserProfile()
                isChangesSaved = true
            } else {

                saveProfileBtn.text = "Save"
            }

            // Toggle the editing mode
            isEditing = !isEditing
        }

        backBtn.setOnClickListener {
            if (isEditing && !isChangesSaved) {
                showConfirmationDialog()
            } else {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun fetchUserData() {
        // Retrieve user information from Firebase Realtime Database
        userReference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val user = dataSnapshot.getValue(User::class.java)
                user?.let {
                    // Populate the EditText fields with user data
                    editProfileName.setText(it.name)
                    editProfileSurname.setText(it.surname)
                    editProfileEmail.setText(it.email)

                }
            }
        }
    }

    private fun updateUserProfile() {
        val name = editProfileName.text.toString()
        val surname = editProfileSurname.text.toString()
        val email = editProfileEmail.text.toString()
        val password = ""

        // Update user information in Realtime Database
        val updatedUser = User(name, surname, email, password)
        userReference.setValue(updatedUser)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Profile update failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        saveProfileBtn.text = "Edit"

        isEditing = !isEditing
    }

    private fun showConfirmationDialog() {

        AlertDialog.Builder(this)
            .setTitle("Discard Changes")
            .setMessage("You have unsaved changes. Are you sure you want to leave edit mode without saving?")
            .setPositiveButton("Yes") { _, _ ->

                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->

                dialog.dismiss()
            }
            .show()
    }
}
