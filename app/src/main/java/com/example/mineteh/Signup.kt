package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class Signup : AppCompatActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var createAccountButton: MaterialButton
    private lateinit var backButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        usernameEditText = findViewById(R.id.username_signup)
        firstNameEditText = findViewById(R.id.firstname_signup)
        lastNameEditText = findViewById(R.id.lastname_signup)
        emailEditText = findViewById(R.id.email_signup)
        passwordEditText = findViewById(R.id.password_signup)
        confirmPasswordEditText = findViewById(R.id.confirmpassword_signup)
        createAccountButton = findViewById(R.id.create_account_btn)
        backButton = findViewById(R.id.back_btn)

        // Filter to prevent typing numbers in name fields
        val nameFilter = InputFilter { source, start, end, _, _, _ ->
            for (i in start until end) {
                if (Character.isDigit(source[i])) {
                    return@InputFilter ""
                }
            }
            null
        }
        firstNameEditText.filters = arrayOf(nameFilter)
        lastNameEditText.filters = arrayOf(nameFilter)

        createAccountButton.setOnClickListener {
            if (validateInputs()) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Login::class.java))
                finish()
            }
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {

        val username = usernameEditText.text.toString().trim()
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        var isValid = true

        // Username validation
        val usernamePattern = Regex("^[a-zA-Z0-9]{6,12}$")
        if (!usernamePattern.matches(username)) {
            usernameEditText.error = "Username must be 6–12 letters or numbers"
            isValid = false
        } else {
            usernameEditText.error = null
        }

        // First Name validation
        if (firstName.isEmpty()) {
            firstNameEditText.error = "First name is required"
            isValid = false
        } else if (firstName.length < 2) {
            firstNameEditText.error = "Minimum 2 characters"
            isValid = false
        } else if (!Regex("^[a-zA-Z.\\-\\' ]+$").matches(firstName)) {
            firstNameEditText.error = "Only letters and . - ' allowed"
            isValid = false
        } else {
            firstNameEditText.error = null
        }

        // Last Name validation
        if (lastName.isEmpty()) {
            lastNameEditText.error = "Last name is required"
            isValid = false
        } else if (lastName.length < 2) {
            lastNameEditText.error = "Minimum 2 characters"
            isValid = false
        } else if (!Regex("^[a-zA-Z.\\-\\' ]+$").matches(lastName)) {
            lastNameEditText.error = "Only letters and . - ' allowed"
            isValid = false
        } else {
            lastNameEditText.error = null
        }

        // Email validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Enter a valid email address"
            isValid = false
        } else {
            emailEditText.error = null
        }

        // Improved Password validation
        val errorMsg = getPasswordError(password)
        if (errorMsg != null) {
            passwordEditText.error = errorMsg
            isValid = false
        } else {
            passwordEditText.error = null
        }

        // Confirm password
        if (confirmPassword != password) {
            confirmPasswordEditText.error = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordEditText.error = null
        }

        return isValid
    }

    private fun getPasswordError(password: String): String? {
        if (password.length < 8 || password.length > 16) return "8–16 characters required"
        if (!password.any { it.isUpperCase() }) return "At least one uppercase letter required"
        if (!password.any { it.isLowerCase() }) return "At least one lowercase letter required"
        if (!password.any { it.isDigit() }) return "At least one number required"
        if (!password.any { "!@#$%^&*()_=+{}|;:',.<>/?~`".contains(it) }) return "At least one special character required"
        if (password.contains(" ")) return "Spaces are not allowed"
        return null
    }
}
