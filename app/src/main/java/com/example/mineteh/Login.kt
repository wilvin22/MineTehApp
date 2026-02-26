package com.example.mineteh

import android.content.Intent
import android.os.Bundle
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

    // ViewModel
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        // Check if already logged in
        if (TokenManager.isLoggedIn(this)) {
            navigateToHome()
            return
        }

        // Initialize views
        emailEditText = findViewById(R.id.email_login)
        passwordEditText = findViewById(R.id.password_login)
        loginButton = findViewById(R.id.login_btn)
        signupButton = findViewById(R.id.signup_btn)
        forgotPasswordText = findViewById(R.id.forgotPassword)

        // Observe login state
        observeLoginState()

        // Login button click
        loginButton.setOnClickListener {
            val identifier = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
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
                        TokenManager.saveToken(this, loginData.token)
                        TokenManager.saveUser(
                            this,
                            loginData.user.accountId,
                            loginData.user.username
                        )

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

        loginButton.text = if (loading) "Logging in..." else "Login"
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetState()
    }
}
