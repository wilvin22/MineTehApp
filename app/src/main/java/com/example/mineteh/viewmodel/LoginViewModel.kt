package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.AuthRepository
import com.example.mineteh.models.LoginResponse
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application.applicationContext)

    private val _loginState = MutableLiveData<Resource<LoginResponse>?>()
    val loginState: LiveData<Resource<LoginResponse>?> = _loginState

    fun login(identifier: String, password: String) {
        // Validate input
        if (identifier.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Please fill in all fields")
            return
        }

        // Show loading
        _loginState.value = Resource.Loading()

        // Make API call
        viewModelScope.launch {
            val result = repository.login(identifier, password)
            _loginState.value = result
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}
