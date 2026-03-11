package com.example.mineteh.model.repository

import android.content.Context
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.mineteh.models.LoginResponse
import com.example.mineteh.models.RegisterResponse
import com.example.mineteh.models.User
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest

class AuthRepository(context: Context) {
    private val supabaseClient = com.example.mineteh.supabase.SupabaseClient.client
    private val database = com.example.mineteh.supabase.SupabaseClient.database
    private val tokenManager = TokenManager(context)

    suspend fun login(identifier: String, password: String) = withContext(Dispatchers.IO) {
        try {
            // Query accounts table to find user by email or username
            val response = database.from("accounts")
                .select(columns = Columns.list("account_id", "username", "email", "first_name", "last_name", "password_hash"))
            
            // Parse JSON response
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(response.data).jsonArray
            
            // Find user by identifier (email or username)
            val userData = jsonArray
                .map { it.jsonObject }
                .firstOrNull { obj ->
                    val email = obj["email"]?.jsonPrimitive?.content ?: ""
                    val username = obj["username"]?.jsonPrimitive?.content ?: ""
                    email.equals(identifier, ignoreCase = true) || username.equals(identifier, ignoreCase = true)
                }
            
            if (userData == null) {
                android.util.Log.e("AuthRepository", "User not found for identifier: $identifier")
                return@withContext Resource.Error<LoginResponse>("Invalid email or password")
            }
            
            // Verify password using BCrypt
            val storedPasswordHash = userData["password_hash"]?.jsonPrimitive?.content ?: ""
            android.util.Log.d("AuthRepository", "Verifying password for user: $identifier")
            
            // Use BCrypt to verify the password
            val verifyResult = BCrypt.verifyer().verify(password.toCharArray(), storedPasswordHash)
            
            if (!verifyResult.verified) {
                android.util.Log.e("AuthRepository", "Invalid password for user: $identifier")
                return@withContext Resource.Error<LoginResponse>("Invalid email or password")
            }
            
            android.util.Log.d("AuthRepository", "Password verified successfully for user: $identifier")
            
            // Extract user data
            val accountId = userData["account_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            val username = userData["username"]?.jsonPrimitive?.content ?: ""
            val email = userData["email"]?.jsonPrimitive?.content ?: ""
            val firstName = userData["first_name"]?.jsonPrimitive?.content ?: ""
            val lastName = userData["last_name"]?.jsonPrimitive?.content ?: ""
            
            // Generate a simple token (In production, use JWT or proper token generation)
            val token = generateToken(accountId, username)
            
            // Create User object
            val user = User(
                accountId = accountId,
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName
            )
            
            // Create LoginResponse object
            val loginResponse = LoginResponse(
                token = token,
                user = user
            )
            
            // Save session data using TokenManager
            tokenManager.saveToken(token)
            tokenManager.saveUserId(accountId)
            tokenManager.saveUserName(username)
            tokenManager.saveUserEmail(email)
            
            android.util.Log.d("AuthRepository", "Login successful for user: $username")
            Resource.Success(loginResponse)
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            android.util.Log.e("AuthRepository", "Supabase REST error", e)
            Resource.Error("Authentication failed: ${e.message}")
        } catch (e: io.github.jan.supabase.exceptions.HttpRequestException) {
            android.util.Log.e("AuthRepository", "Network error", e)
            Resource.Error("Network error. Please check your connection.")
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("AuthRepository", "No internet connection", e)
            Resource.Error("No internet connection")
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("AuthRepository", "Connection timeout", e)
            Resource.Error("Connection timeout. Please try again.")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Login error", e)
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AuthRepository", "Starting registration for username: $username, email: $email")
            
            // Use Ktor HttpClient to call the RPC function directly
            val httpClient = HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
            }
            
            val rpcUrl = "https://didpavzminvohszuuowu.supabase.co/rest/v1/rpc/register_user"
            val requestBody = buildJsonObject {
                put("p_username", JsonPrimitive(username))
                put("p_email", JsonPrimitive(email))
                put("p_password", JsonPrimitive(password))
                put("p_first_name", JsonPrimitive(firstName))
                put("p_last_name", JsonPrimitive(lastName))
            }
            
            val response = httpClient.post(rpcUrl) {
                header("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRpZHBhdnptaW52b2hzenV1b3d1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMTYwNDgsImV4cCI6MjA4NzU5MjA0OH0.iueZB9z5Z5YvKM98Gsy-ll--kLipCKXtmT0V7jHBA0Y")
                header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRpZHBhdnptaW52b2hzenV1b3d1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMTYwNDgsImV4cCI6MjA4NzU5MjA0OH0.iueZB9z5Z5YvKM98Gsy-ll--kLipCKXtmT0V7jHBA0Y")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            val responseBody = response.bodyAsText()
            httpClient.close()
            
            android.util.Log.d("AuthRepository", "RPC response: $responseBody")
            
            // Parse the JSON response
            val json = Json { ignoreUnknownKeys = true }
            val result = json.parseToJsonElement(responseBody).jsonObject
            
            val success = result["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
            val message = result["message"]?.jsonPrimitive?.content ?: "Unknown error"
            
            if (!success) {
                android.util.Log.e("AuthRepository", "Registration failed: $message")
                return@withContext Resource.Error<RegisterResponse>(message)
            }
            
            // Extract user data from the response
            val userData = result["data"]?.jsonObject
            if (userData == null) {
                android.util.Log.e("AuthRepository", "No user data in response")
                return@withContext Resource.Error<RegisterResponse>("Registration failed: No user data returned")
            }
            
            val accountId = userData["account_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            val returnedUsername = userData["username"]?.jsonPrimitive?.content ?: username
            val returnedEmail = userData["email"]?.jsonPrimitive?.content ?: email
            val returnedFirstName = userData["first_name"]?.jsonPrimitive?.content ?: firstName
            val returnedLastName = userData["last_name"]?.jsonPrimitive?.content ?: lastName
            
            // Generate a token for the new user
            val token = generateToken(accountId, returnedUsername)
            
            // Create User object
            val user = User(
                accountId = accountId,
                username = returnedUsername,
                email = returnedEmail,
                firstName = returnedFirstName,
                lastName = returnedLastName
            )
            
            // Create RegisterResponse object
            val registerResponse = RegisterResponse(
                token = token,
                user = user
            )
            
            // Save session data using TokenManager
            tokenManager.saveToken(token)
            tokenManager.saveUserId(accountId)
            tokenManager.saveUserName(returnedUsername)
            tokenManager.saveUserEmail(returnedEmail)
            
            android.util.Log.d("AuthRepository", "Registration successful for user: $returnedUsername")
            Resource.Success(registerResponse)
            
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            android.util.Log.e("AuthRepository", "Supabase REST error during registration: ${e.message}", e)
            Resource.Error("Registration failed: ${e.message}")
        } catch (e: io.github.jan.supabase.exceptions.HttpRequestException) {
            android.util.Log.e("AuthRepository", "Network error during registration", e)
            Resource.Error("Network error. Please check your connection.")
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("AuthRepository", "No internet connection", e)
            Resource.Error("No internet connection")
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("AuthRepository", "Connection timeout", e)
            Resource.Error("Connection timeout. Please try again.")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Registration error", e)
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Logs out the current user by clearing all stored session data.
     * Since we're using custom authentication (not Supabase Auth module),
     * logout only needs to clear local session data.
     * 
     * @return Resource.Success on successful logout
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            // Clear all session data using TokenManager
            tokenManager.clearAll()
            
            android.util.Log.d("AuthRepository", "Logout successful - session cleared")
            Resource.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Logout error", e)
            Resource.Error(e.message ?: "An error occurred during logout")
        }
    }

    /**
     * Gets the current authenticated user's profile data.
     * Since we're using custom authentication (not Supabase Auth module),
     * we check TokenManager.isLoggedIn() and use TokenManager.getUserId() to get the user ID,
     * then query the accounts table using Postgrest to get full user profile data.
     * 
     * @return Resource.Success with User object if authenticated, Resource.Error otherwise
     */
    suspend fun getCurrentUser() = withContext(Dispatchers.IO) {
        try {
            // Check if user is logged in
            if (!tokenManager.isLoggedIn()) {
                android.util.Log.e("AuthRepository", "User not authenticated")
                return@withContext Resource.Error("User not authenticated. Please login.")
            }
            
            // Get user ID from TokenManager
            val userId = tokenManager.getUserId()
            if (userId == -1) {
                android.util.Log.e("AuthRepository", "Invalid user ID")
                return@withContext Resource.Error("Invalid session. Please login again.")
            }
            
            // Query accounts table to get full user profile data
            val response = database
                .from("accounts")
                .select()
            
            // Parse JSON response
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(response.data).jsonArray
            
            val userData = jsonArray
                .map { it.jsonObject }
                .firstOrNull { obj ->
                    (obj["account_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1) == userId
                }
                ?: throw Exception("User not found")
            
            val accountId = userData["account_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            val username = userData["username"]?.jsonPrimitive?.content ?: ""
            val email = userData["email"]?.jsonPrimitive?.content ?: ""
            val firstName = userData["first_name"]?.jsonPrimitive?.content ?: ""
            val lastName = userData["last_name"]?.jsonPrimitive?.content ?: ""
            
            // Create User object
            val user = User(
                accountId = accountId,
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName
            )
            
            android.util.Log.d("AuthRepository", "Successfully retrieved user profile for: $username")
            Resource.Success(user)
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            android.util.Log.e("AuthRepository", "Supabase REST error getting current user", e)
            when {
                e.message?.contains("No rows found") == true || 
                e.message?.contains("not found") == true ->
                    Resource.Error("User profile not found. Please login again.")
                else -> Resource.Error(e.message ?: "Failed to retrieve user profile")
            }
        } catch (e: io.github.jan.supabase.exceptions.HttpRequestException) {
            android.util.Log.e("AuthRepository", "Network error getting current user", e)
            Resource.Error("Network error. Please check your connection.")
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("AuthRepository", "No internet connection", e)
            Resource.Error("No internet connection")
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("AuthRepository", "Connection timeout", e)
            Resource.Error("Connection timeout. Please try again.")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error getting current user", e)
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }
    
    /**
     * Hashes a password using SHA-256.
     * Note: In production, use a proper password hashing library like BCrypt or Argon2.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Generates a simple token for the user session.
     * Note: In production, use JWT or a proper token generation mechanism.
     */
    private fun generateToken(accountId: Int, username: String): String {
        val timestamp = System.currentTimeMillis()
        val data = "$accountId:$username:$timestamp"
        return hashPassword(data)
    }
}
