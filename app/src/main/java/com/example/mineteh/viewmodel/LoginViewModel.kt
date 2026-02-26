package com.example.mineteh.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repositories.AuthRepository
import com.example.mineteh.models.LoginData
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<Resource<LoginData>>()
    val loginState: LiveData<Resource<LoginData>> = _loginState

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