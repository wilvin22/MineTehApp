package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotPassword : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        // Initialize views
        val emailEditText = findViewById<TextInputEditText>(R.id.email_forgot)
        val sendResetButton = findViewById<MaterialButton>(R.id.send_reset_btn)
        val backToLoginButton = findViewById<MaterialButton>(R.id.back_to_login_btn)

        // Send reset link button click
        sendResetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            // Validate input
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Add password reset logic here (Firebase/Auth server)
            Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_SHORT).show()

            // Navigate back to Login
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        // Back to Login button click
        backToLoginButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }
}