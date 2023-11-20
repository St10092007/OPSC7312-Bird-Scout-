package com.example.birding.Authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.*
import com.example.birding.Home.HomeActivity
import com.example.birding.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var progressBar: ProgressBar

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser

        // Check if the user is already logged in
        if (currentUser != null) {
            Toast.makeText(this, "Logged in Successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    // The onCreate method is called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout for this activity
        setContentView(R.layout.activity_register)

        // Initialize Firebase Authentication
        auth = Firebase.auth

        // Initialize UI elements
        nameEditText = findViewById(R.id.etRegisterName)
        surnameEditText = findViewById(R.id.etRegisterSurname)
        emailEditText = findViewById(R.id.etRegisterEmail)
        passwordEditText = findViewById(R.id.etRegisterPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)

        loginButton = findViewById(R.id.btnLogin)
        registerButton = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)

        val togglePasswordButton: ToggleButton = findViewById(R.id.btnTogglePassword)
        val toggleConfirmPasswordButton: ToggleButton = findViewById(R.id.btnToggleConfirmPassword)

        // Set click listeners for toggle buttons
        togglePasswordButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show password
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Hide password
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Move cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.length())
        }

        toggleConfirmPasswordButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show password
                confirmPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Hide password
                confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Move cursor to the end of the text
            confirmPasswordEditText.setSelection(confirmPasswordEditText.length())
        }

        // Set a click listener for the login button
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Set a click listener for the register button
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val name = nameEditText.text.toString()
            val surname = surnameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Input validation
            if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
                emailEditText.error = "Enter a valid email address"
                return@setOnClickListener
            } else {
                emailEditText.error = null
            }

            if (TextUtils.isEmpty(name)) {
                nameEditText.error = "Enter name"
                return@setOnClickListener
            } else {
                nameEditText.error = null
            }

            if (TextUtils.isEmpty(surname)) {
                surnameEditText.error = "Enter surname"
                return@setOnClickListener
            } else {
                surnameEditText.error = null
            }

            if (TextUtils.isEmpty(password) || password.length < 8) {
                passwordEditText.error = "Password must be at least 8 characters"
                return@setOnClickListener
            } else {
                passwordEditText.error = null
            }

            // Password regex check
            val passwordRegex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#\$%^&*(),.?\":{}|<>]).{8,}$"
            if (!password.matches(passwordRegex.toRegex())) {
                // Notify the user about missing password requirements
                val errorMessage = StringBuilder("Password must contain:")
                if (!password.matches("(?=.*[0-9])".toRegex())) {
                    errorMessage.append(" at least one number")
                }
                if (!password.matches("(?=.*[A-Z])".toRegex())) {
                    errorMessage.append(" at least one capital letter")
                }
                if (!password.matches("(?=.*[!@#\$%^&*(),.?\":{}|<>])".toRegex())) {
                    errorMessage.append(" at least one special character")
                }

                passwordEditText.error = errorMessage.toString()
                return@setOnClickListener
            } else {
                passwordEditText.error = null
            }

            if (password != confirmPassword) { // Check if passwords match
                confirmPasswordEditText.error = "Passwords do not match"
                return@setOnClickListener
            } else {

                confirmPasswordEditText.error = null
            }



            // Show the progress bar
            progressBar.visibility = View.VISIBLE

            // Delay for 3 seconds before hiding the progress bar
            Handler(Looper.getMainLooper()).postDelayed({
                // Create a user account with Firebase Authentication
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressBar.visibility = View.INVISIBLE // Hide progress bar

                        if (task.isSuccessful) {
                            // Registration succeeded
                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                val userId = currentUser.uid
                                val userRef = database.reference.child("Users").child(userId)
                                userRef.child("name").setValue(name)
                                userRef.child("surname").setValue(surname)
                                userRef.child("email").setValue(email)

                                Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                            } else {
                                // Handle the case where currentUser is unexpectedly null
                                Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // If sign-up fails, handle specific exceptions
                            try {
                                throw task.exception!!
                            } catch (e: FirebaseAuthUserCollisionException) {
                                // The email address is already in use by another account.
                                Toast.makeText(this, "Email address is already registered. Please use a different email.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // Handle other exceptions here, if needed.
                                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }, 2000) // 2000 milliseconds (2 seconds)
        }

    }

    // Function to validate email format
    private fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}
