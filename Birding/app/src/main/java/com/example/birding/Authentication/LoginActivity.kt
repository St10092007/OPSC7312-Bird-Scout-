package com.example.birding.Authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.*
import com.example.birding.Home.HomeActivity
import com.example.birding.Home.SplashActivity
import com.example.birding.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    // Declare member variables
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerbtn: Button
    private lateinit var auth: FirebaseAuth

    public override fun onStart() {
        super.onStart()
        Log.d("LoginActivity", "onStart called")
        val currentUser = auth.currentUser
        // Check if the user is already logged in
        if (currentUser != null) {
            Log.d("LoginActivity", "User is already logged in. Showing toast and navigating to HomeActivity.")
            Toast.makeText(this, "Log in Successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        val togglePasswordButton: ToggleButton = findViewById(R.id.btnTogglePassword)

        // Initialize UI elements
        emailEditText = findViewById(R.id.etLoginEmail)
        passwordEditText = findViewById(R.id.etLoginPassword)
        loginButton =  findViewById(R.id.btnLogin)
        registerbtn = findViewById(R.id.btnRegister)

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

        // Set a click listener for the register button
        registerbtn.setOnClickListener {

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        // Set a click listener for the login button
        loginButton.setOnClickListener {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()


            if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
                emailEditText.error = "Enter a valid email address"
                return@setOnClickListener
            } else {
                emailEditText.error = null
            }
                if(TextUtils.isEmpty(password))
                {
                    passwordEditText.error = "Enter Password "
                    return@setOnClickListener;
                }
                else {
                    passwordEditText.error = null
                }

                // Sign in with Firebase Authentication
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener() { task ->
                        if (task.isSuccessful) {


                            Toast.makeText(this, "Logged in Successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, SplashActivity::class.java)
                            startActivity(intent)
                        } else {
                            // If sign in fails, display a message to the user.
                            val errorMessage = "Login failed. Please check your email and password."
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
        }
    }

    // Function to validate email format
    private fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}