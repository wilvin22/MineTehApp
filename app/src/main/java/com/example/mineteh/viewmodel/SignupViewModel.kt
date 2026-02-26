package com.example.mineteh.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repositories.AuthRepository
import com.example.mineteh.models.LoginData
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _signupStatus = MutableLiveData<Resource<LoginData>?>()
    val signupStatus: LiveData<Resource<LoginData>?> = _signupStatus

    fun onSignupClicked(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPass: String
    ) {
        val error = validateInputs(username, firstName, lastName, email, password, confirmPass)
        if (error != null) {
            _signupStatus.value = Resource.Error(error)
            return
        }

        // Call API
        _signupStatus.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.register(
                username = username,
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName
            )
            _signupStatus.value = result
        }
    }

    private fun validateInputs(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPass: String
    ): String? {
        if (!Regex("^[a-zA-Z0-9]{6,12}$").matches(username)) {
            return "Username must be 6–12 letters or numbers"
        }

        if (firstName.length < 2 || !Regex("^[a-zA-Z.\\-\\' ]+$").matches(firstName)) {
            return "Invalid First Name"
        }

        if (lastName.length < 2 || !Regex("^[a-zA-Z.\\-\\' ]+$").matches(lastName)) {
            return "Invalid Last Name"
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Enter a valid email address"
        }

        val passError = getPasswordError(password)
        if (passError != null) return passError

        if (password != confirmPass) {
            return "Passwords do not match"
        }

        return null
    }

    private fun getPasswordError(password: String): String? {
        if (password.length < 8 || password.length > 16) return "Password: 8–16 characters required"
        if (!password.any { it.isUpperCase() }) return "Password: At least one uppercase letter required"
        if (!password.any { it.isLowerCase() }) return "Password: At least one lowercase letter required"
        if (!password.any { it.isDigit() }) return "Password: At least one number required"
        if (!password.any { "!@#$%^&*()_=+{}|;:',.<>/?~`".contains(it) }) return "Password: At least one special character required"
        if (password.contains(" ")) return "Password: Spaces are not allowed"
        return null
    }

    fun resetStatus() {
        _signupStatus.value = null
    }
}