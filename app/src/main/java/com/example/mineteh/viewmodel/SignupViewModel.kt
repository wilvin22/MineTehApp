package com.example.mineteh.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.AuthRepository
import com.example.mineteh.models.RegisterResponse
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class SignupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application.applicationContext)

    private val _signupStatus = MutableLiveData<Resource<RegisterResponse>?>()
    val signupStatus: LiveData<Resource<RegisterResponse>?> = _signupStatus

    private val _validationErrors = MutableLiveData<Map<String, String?>>()
    val validationErrors: LiveData<Map<String, String?>> = _validationErrors

    fun onSignupClicked(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPass: String
    ) {
        val errors = validateInputs(username, firstName, lastName, email, password, confirmPass)
        _validationErrors.value = errors

        if (errors.isNotEmpty()) {
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
    ): Map<String, String?> {
        val errors = mutableMapOf<String, String?>()

        if (!Regex("^[a-zA-Z0-9]{6,12}$").matches(username)) {
            errors["username"] = "Username must be 6–12 letters or numbers"
        }

        if (firstName.length < 2 || !Regex("^[a-zA-Z.\\-\\' ]+$").matches(firstName)) {
            errors["firstName"] = "Invalid First Name"
        }

        if (lastName.length < 2 || !Regex("^[a-zA-Z.\\-\\' ]+$").matches(lastName)) {
            errors["lastName"] = "Invalid Last Name"
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors["email"] = "Enter a valid email address"
        }

        val passError = getPasswordError(password)
        if (passError != null) {
            errors["password"] = passError
        }

        if (password != confirmPass) {
            errors["confirmPass"] = "Passwords do not match"
        }

        return errors
    }

    private fun getPasswordError(password: String): String? {
        if (password.length < 8 || password.length > 16) return "8–16 characters required"
        if (!password.any { it.isUpperCase() }) return "At least one uppercase letter required"
        if (!password.any { it.isLowerCase() }) return "At least one lowercase letter required"
        if (!password.any { it.isDigit() }) return "At least one number required"
        if (!password.any { "!@#$%^&*()_=+{}|;:',.<>/?~`".contains(it) }) return "At least one special character required"
        if (password.contains(" ")) return "Spaces are not allowed"
        return null
    }

    fun resetStatus() {
        _signupStatus.value = null
        _validationErrors.value = emptyMap()
    }
}
