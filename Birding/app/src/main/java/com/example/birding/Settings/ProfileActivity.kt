package com.example.birding.Settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.birding.Core.User
import com.example.birding.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class ProfileActivity : AppCompatActivity() {

    // UI elements
    private lateinit var editProfileName: EditText
    private lateinit var editProfileSurname: EditText
    private lateinit var editOldPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var profileName: TextView
    private lateinit var profileSurname: TextView
    private lateinit var saveProfileBtn: Button
    private lateinit var backBtn: Button
    private lateinit var progressBar: ProgressBar

    // Firebase and data references
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val userReference = database.reference.child("Users").child(auth.currentUser?.uid.toString())
    private var isEditing = false
    private var isChangesSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize UI elements
        editProfileName = findViewById(R.id.etProfileName)
        editProfileSurname = findViewById(R.id.etProfileSurname)
        profileName = findViewById(R.id.tvProfileName)
        profileSurname = findViewById(R.id.tvProfileSurname)
        editOldPassword = findViewById(R.id.etOldPassword)
        editNewPassword = findViewById(R.id.etNewPassword)
        saveProfileBtn = findViewById(R.id.btnSave)
        backBtn = findViewById(R.id.btnBack)

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        // Initially, edit fields are invisible
        setEditTextVisibility(false)

        // Fetch and display user data
        fetchUserData()

        // change the button text to "Edit"
        saveProfileBtn.text = "Edit"

        saveProfileBtn.setOnClickListener {
            if (isEditing) {
                val oldPassword = editOldPassword.text.toString()
                val newPassword = editNewPassword.text.toString()

                if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(this, "Please fill in both old and new passwords", Toast.LENGTH_SHORT).show()
                } else {
                    // Call a function to update the password
                    updatePassword(oldPassword, newPassword)
                }
                isChangesSaved = true
                fetchUserData()
                setEditTextVisibility(false)
            } else {
                setEditTextVisibility(true)
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

    private fun updatePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        val email = user?.email

        if (email != null) {
            val credential = EmailAuthProvider.getCredential(email, oldPassword)

            user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            // Clear the password fields
                            editOldPassword.text.clear()
                            editNewPassword.text.clear()
                            // ...
                        } else {
                            Toast.makeText(this, "Password update failed: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setEditTextVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.INVISIBLE
        editProfileName.visibility = visibility
        editProfileSurname.visibility = visibility
        editOldPassword.visibility = visibility
        editNewPassword.visibility = visibility

    }

    private fun fetchUserData() {

        progressBar.visibility = View.VISIBLE
        // Retrieve user information from Firebase Realtime Database
        userReference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val user = dataSnapshot.getValue(User::class.java)
                user?.let {
                    // Populate the textviews Text fields with user data
                    profileName.text = "Profile Name: ${it.name}"
                    profileSurname.text = "Profile Surname: ${it.surname}"

                    progressBar.visibility = View.GONE

                    editProfileName.setText(it.name)
                    editProfileSurname.setText(it.surname)
                }
            }
        }
    }

    private fun showConfirmationDialog() {

        AlertDialog.Builder(this)
            .setTitle("Discard Changes")
            .setMessage("You have unsaved changes. Are you sure you want to leave edit mode without saving?")
            .setPositiveButton("Yes") { _, _ ->

                setEditTextVisibility(false)
                saveProfileBtn.text = "Edit"
                isEditing = false
                isChangesSaved = false
                fetchUserData()
            }
            .setNegativeButton("Cancel") { dialog, _ ->

                dialog.dismiss()
            }
            .show()
    }
}
