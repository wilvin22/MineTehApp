package com.example.mineteh.model.repository

import android.content.Context
import com.example.mineteh.models.LoginRequest
import com.example.mineteh.models.RegisterRequest
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(context)

    suspend fun login(identifier: String, password: String) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequest(identifier, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    tokenManager.saveToken(data.token)
                    tokenManager.saveUserId(data.user.accountId)
                    tokenManager.saveUserName(data.user.username)
                    tokenManager.saveUserEmail(data.user.email)
                }
                Resource.Success(data)
            } else {
                Resource.Error(response.body()?.message ?: "Login failed")
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            Resource.Error("Server response error. Please try again.")
        } catch (e: java.net.UnknownHostException) {
            Resource.Error("No internet connection")
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("Connection timeout. Please try again.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
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
            val response = apiService.register(
                RegisterRequest(username, email, password, firstName, lastName)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    tokenManager.saveToken(data.token)
                    tokenManager.saveUserId(data.user.accountId)
                    tokenManager.saveUserName(data.user.username)
                    tokenManager.saveUserEmail(data.user.email)
                }
                Resource.Success(data)
            } else {
                Resource.Error(response.body()?.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}