package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.SellerRepository
import com.example.mineteh.model.repository.SellerStats
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.launch

class SellingDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SellerRepository(application.applicationContext)
    private val tokenManager = TokenManager(application.applicationContext)

    private val _stats = MutableLiveData<Resource<SellerStats>>()
    val stats: LiveData<Resource<SellerStats>> = _stats

    fun loadStats() {
        val userId = tokenManager.getUserId()
        if (userId == -1) {
            _stats.value = Resource.Error("User not logged in")
            return
        }
        _stats.value = Resource.Loading()
        viewModelScope.launch {
            _stats.postValue(repository.getMyStats(userId))
        }
    }
}
