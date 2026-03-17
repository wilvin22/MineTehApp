package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.view.HomeActivity
import com.example.mineteh.viewmodel.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class Login : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupButton: MaterialButton
    private lateinit var forgotPasswordText: TextView
    private lateinit var rememberMeCheckbox: CheckBox

    // ViewModel
    private val viewModel: LoginViewModel by viewModels()

    // TokenManager
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        // Initialize TokenManager
        tokenManager = TokenManager(this)

        // Initialize views
        emailEditText = findViewById(R.id.email_login)
        passwordEditText = findViewById(R.id.password_login)
        loginButton = findViewById(R.id.login_btn)
        signupButton = findViewById(R.id.signup_btn)
        forgotPasswordText = findViewById(R.id.forgotPassword)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)

        // Check if already logged in (Auto-login)
        if (tokenManager.isLoggedIn()) {
            if (tokenManager.isTokenExpired()) {
                tokenManager.clearAll()
                Toast.makeText(this, "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show()
            } else {
                navigateToHome()
                return
            }
        }

        // Load saved email if Remember Me was previously checked
        if (tokenManager.isRememberMeEnabled()) {
            emailEditText.setText(tokenManager.getSavedEmail())
            rememberMeCheckbox.isChecked = true
        }

        // Observe login state
        observeLoginState()

        // Login button click
        loginButton.setOnClickListener {
            val identifier = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            // Save Remember Me state
            tokenManager.setRememberMe(rememberMeCheckbox.isChecked)
            if (rememberMeCheckbox.isChecked) {
                tokenManager.saveSavedEmail(identifier)
            }

            viewModel.login(identifier, password)
        }

        // Sign up button click
        signupButton.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        // Forgot password click
        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }
    }

    private fun observeLoginState() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    setLoading(true)
                }
                is Resource.Success -> {
                    setLoading(false)
                    resource.data?.let { loginData ->
                        // Save token and user info
                        tokenManager.saveToken(loginData.token)
                        tokenManager.saveUserId(loginData.user.accountId)
                        tokenManager.saveUserName(loginData.user.username)
                        tokenManager.saveUserEmail(loginData.user.email)

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                }
                is Resource.Error -> {
                    setLoading(false)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {
                    // Initial state
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        signupButton.isEnabled = !loading
        emailEditText.isEnabled = !loading
        passwordEditText.isEnabled = !loading
        rememberMeCheckbox.isEnabled = !loading

        loginButton.text = if (loading) "Logging in..." else "Login"
    }

    private fun navigateToHome() {
        android.util.Log.d("Login", "navigateToHome() called")
        try {
            val intent = Intent(this, HomeActivity::class.java)
            android.util.Log.d("Login", "Starting HomeActivity with intent: $intent")
            startActivity(intent)
            android.util.Log.d("Login", "startActivity(HomeActivity) completed")
            finish()
        } catch (e: Exception) {
            android.util.Log.e("Login", "Error starting HomeActivity", e)
            Toast.makeText(this, "Error navigating to home: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetState()
    }
}
