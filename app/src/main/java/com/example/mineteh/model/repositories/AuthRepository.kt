package com.example.mineteh.model.repositories

import com.example.mineteh.models.LoginRequest
import com.example.mineteh.models.RegisterRequest
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val apiService = ApiClient.apiService

    suspend fun login(identifier: String, password: String) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequest(identifier, password))
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Login failed")
            }
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
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}