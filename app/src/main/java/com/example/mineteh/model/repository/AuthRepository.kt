package com.example.mineteh.model.repository

import android.content.Context
import com.example.mineteh.models.LoginResponse
import com.example.mineteh.models.RegisterResponse
import com.example.mineteh.models.User
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepository(context: Context) {
    private val supabaseClient = com.example.mineteh.supabase.SupabaseClient.client
    private val database = com.example.mineteh.supabase.SupabaseClient.database
    private val tokenManager = TokenManager(context)

    suspend fun login(identifier: String, password: String) = withContext(Dispatchers.IO) {
        try {
            // Use RPC to verify password and get user data
            // The database should have a function like: login_user(p_identifier text, p_password text)
            val responseBody = database.rpc(
                function = "login_user",
                parameters = mapOf(
                    "p_identifier" to identifier,
                    "p_password" to password
                )
            ).decodeList<Map<String, Any>>().firstOrNull() ?: emptyMap()
            
            // Check if login was successful
            val success = responseBody["success"] as? Boolean ?: false
            if (success) {
                val userData = responseBody["data"] as? Map<String, Any>
                if (userData != null) {
                    val accountId = (userData["account_id"] as? Number)?.toInt() ?: -1
                    val username = userData["username"] as? String ?: ""
                    val email = userData["email"] as? String ?: ""
                    val firstName = userData["first_name"] as? String ?: ""
                    val lastName = userData["last_name"] as? String ?: ""
                    val token = userData["token"] as? String ?: ""
                    val expiresAt = userData["expires_at"] as? String ?: ""
                    
                    // Create User object
                    val user = User(
                        accountId = accountId,
                        username = username,
                        email = email,
                        firstName = firstName,
                        lastName = lastName
                    )
                    
                    // Create LoginResponse object (for ViewModel compatibility)
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
                } else {
                    android.util.Log.e("AuthRepository", "Invalid response data from server")
                    Resource.Error("Invalid response from server")
                }
            } else {
                val message = responseBody["message"] as? String ?: "Invalid email or password"
                android.util.Log.e("AuthRepository", "Login failed: $message")
                Resource.Error(message)
            }
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            android.util.Log.e("AuthRepository", "Supabase REST error", e)
            when {
                e.message?.contains("invalid_credentials") == true -> 
                    Resource.Error("Invalid email or password")
                e.message?.contains("function") == true && e.message?.contains("does not exist") == true ->
                    Resource.Error("Server configuration error. Please contact support.")
                else -> Resource.Error(e.message ?: "Authentication failed")
            }
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
            // Use RPC to register user with password hashing
            // The database should have a function like: register_user(p_username text, p_email text, p_password text, p_first_name text, p_last_name text)
            val responseBody = database.rpc(
                function = "register_user",
                parameters = mapOf(
                    "p_username" to username,
                    "p_email" to email,
                    "p_password" to password,
                    "p_first_name" to firstName,
                    "p_last_name" to lastName
                )
            ).decodeList<Map<String, Any>>().firstOrNull() ?: emptyMap()
            
            // Check if registration was successful
            val success = responseBody["success"] as? Boolean ?: false
            if (success) {
                val userData = responseBody["data"] as? Map<String, Any>
                if (userData != null) {
                    val accountId = (userData["account_id"] as? Number)?.toInt() ?: -1
                    val userUsername = userData["username"] as? String ?: ""
                    val userEmail = userData["email"] as? String ?: ""
                    val userFirstName = userData["first_name"] as? String ?: ""
                    val userLastName = userData["last_name"] as? String ?: ""
                    val token = userData["token"] as? String ?: ""
                    
                    // Create User object
                    val user = User(
                        accountId = accountId,
                        username = userUsername,
                        email = userEmail,
                        firstName = userFirstName,
                        lastName = userLastName
                    )
                    
                    // Create RegisterResponse object
                    val registerResponse = RegisterResponse(
                        token = token,
                        user = user
                    )
                    
                    // Save session data using TokenManager
                    tokenManager.saveToken(token)
                    tokenManager.saveUserId(accountId)
                    tokenManager.saveUserName(userUsername)
                    tokenManager.saveUserEmail(userEmail)
                    
                    android.util.Log.d("AuthRepository", "Registration successful for user: $userUsername")
                    Resource.Success(registerResponse)
                } else {
                    android.util.Log.e("AuthRepository", "Invalid response data from server")
                    Resource.Error("Invalid response from server")
                }
            } else {
                val message = responseBody["message"] as? String ?: "Registration failed"
                android.util.Log.e("AuthRepository", "Registration failed: $message")
                Resource.Error(message)
            }
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            android.util.Log.e("AuthRepository", "Supabase REST error during registration", e)
            when {
                e.message?.contains("duplicate key") == true || 
                e.message?.contains("already exists") == true ||
                e.message?.contains("unique constraint") == true -> 
                    Resource.Error("An account with this email or username already exists")
                e.message?.contains("function") == true && e.message?.contains("does not exist") == true ->
                    Resource.Error("Server configuration error. Please contact support.")
                else -> Resource.Error(e.message ?: "Registration failed")
            }
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
                .execute()
            
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
}
