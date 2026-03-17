package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ForgotPassword : AppCompatActivity() {

    private val supabaseUrl = "https://didpavzminvohszuuowu.supabase.co"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRpZHBhdnptaW52b2hzenV1b3d1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMTYwNDgsImV4cCI6MjA4NzU5MjA0OH0.iueZB9z5Z5YvKM98Gsy-ll--kLipCKXtmT0V7jHBA0Y"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        val emailEditText = findViewById<TextInputEditText>(R.id.email_forgot)
        val sendResetButton = findViewById<MaterialButton>(R.id.send_reset_btn)
        val backToLoginButton = findViewById<MaterialButton>(R.id.back_to_login_btn)

        sendResetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendResetButton.isEnabled = false
            sendResetButton.text = "Sending..."

            lifecycleScope.launch {
                val success = sendPasswordReset(email)
                sendResetButton.isEnabled = true
                sendResetButton.text = "Send Reset Link"

                if (success) {
                    Toast.makeText(this@ForgotPassword, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ForgotPassword, Login::class.java))
                    finish()
                } else {
                    Toast.makeText(this@ForgotPassword, "Failed to send reset email. Please check the address and try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private suspend fun sendPasswordReset(email: String): Boolean {
        val client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return try {
            val response = client.post("$supabaseUrl/auth/v1/recover") {
                header("apikey", supabaseAnonKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("email", email) }.toString())
            }
            android.util.Log.d("ForgotPassword", "Reset response status: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) {
            android.util.Log.e("ForgotPassword", "Error sending reset email", e)
            false
        } finally {
            client.close()
        }
    }
}