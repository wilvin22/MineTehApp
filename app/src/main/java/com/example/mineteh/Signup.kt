package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.SignupViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Signup : AppCompatActivity() {

    private lateinit var usernameLayout: TextInputLayout
    private lateinit var firstNameLayout: TextInputLayout
    private lateinit var lastNameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var createAccountButton: MaterialButton
    private lateinit var backButton: MaterialButton

    // ViewModel
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        // Layouts
        usernameLayout = findViewById(R.id.username_layout)
        firstNameLayout = findViewById(R.id.firstname_layout)
        lastNameLayout = findViewById(R.id.lastname_layout)
        emailLayout = findViewById(R.id.email_layout)
        passwordLayout = findViewById(R.id.password_layout)
        confirmPasswordLayout = findViewById(R.id.confirmpassword_layout)

        // EditTexts
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

        // Observe signup status and validation errors
        observeSignupStatus()
        observeValidationErrors()

        createAccountButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPass = confirmPasswordEditText.text.toString()

            viewModel.onSignupClicked(
                username = username,
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                confirmPass = confirmPass
            )
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun observeSignupStatus() {
        viewModel.signupStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    setLoading(true)
                }
                is Resource.Success -> {
                    setLoading(false)
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Login::class.java))
                    finish()
                }
                is Resource.Error -> {
                    setLoading(false)
                    // If it's a general error (not validation), show a toast
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {
                    // Initial state
                }
            }
        }
    }

    private fun observeValidationErrors() {
        viewModel.validationErrors.observe(this) { errors ->
            usernameLayout.error = errors["username"]
            firstNameLayout.error = errors["firstName"]
            lastNameLayout.error = errors["lastName"]
            emailLayout.error = errors["email"]
            passwordLayout.error = errors["password"]
            confirmPasswordLayout.error = errors["confirmPass"]
        }
    }

    private fun setLoading(loading: Boolean) {
        createAccountButton.isEnabled = !loading
        backButton.isEnabled = !loading
        createAccountButton.text = if (loading) "Creating..." else "Create Account"
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetStatus()
    }
}
