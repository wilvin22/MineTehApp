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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
            // Check if user already exists
            val checkResponse = database.from("accounts")
                .select(columns = Columns.list("email", "username"))
            
            val json = Json { ignoreUnknownKeys = true }
            val existingUsers = json.parseToJsonElement(checkResponse.data).jsonArray
            
            val emailExists = existingUsers.any { 
                it.jsonObject["email"]?.jsonPrimitive?.content?.equals(email, ignoreCase = true) == true
            }
            val usernameExists = existingUsers.any {
                it.jsonObject["username"]?.jsonPrimitive?.content?.equals(username, ignoreCase = true) == true
            }
            
            if (emailExists || usernameExists) {
                return@withContext Resource.Error<RegisterResponse>("An account with this email or username already exists")
            }
            
            // Hash the password
            val passwordHash = hashPassword(password)
            
            // Insert new user (Note: This is a simplified approach. In production, use proper insert with Supabase)
            // For now, we'll return an error asking the user to create the account through Supabase dashboard
            // or implement a proper server-side function
            
            android.util.Log.e("AuthRepository", "Registration not fully implemented - requires server-side function")
            Resource.Error<RegisterResponse>("Registration is not available. Please contact support.")
            
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            android.util.Log.e("AuthRepository", "Supabase REST error during registration", e)
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
