package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.ListingsRepository
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ListingsRepository(application)
    
    private val _listings = MutableLiveData<Resource<List<Listing>>?>()
    val listings: LiveData<Resource<List<Listing>>?> = _listings

    init {
        android.util.Log.d("HomeViewModel", "Constructor called with application: $application")
        android.util.Log.d("HomeViewModel", "ListingsRepository created: $repository")
        
        try {
            android.util.Log.d("HomeViewModel", "init block executing")
            android.util.Log.d("HomeViewModel", "About to call fetchListings()")
            fetchListings()
            android.util.Log.d("HomeViewModel", "fetchListings() call completed")
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error in init block", e)
            _listings.value = Resource.Error("Failed to initialize: ${e.message}")
        }
    }

    fun fetchListings(category: String? = null, type: String? = null, search: String? = null) {
        android.util.Log.d("HomeViewModel", "fetchListings() called with category=$category, type=$type, search=$search")
        _listings.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getListings(category, type, search)
                _listings.value = result
                android.util.Log.d("HomeViewModel", "fetchListings() completed with result: $result")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error in fetchListings()", e)
                _listings.value = Resource.Error("Failed to fetch listings: ${e.message}")
            }
        }
    }

    private val _toggleResult = MutableLiveData<Resource<Boolean>?>()
    val toggleResult: LiveData<Resource<Boolean>?> = _toggleResult

    fun toggleFavorite(listingId: Int) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(listingId)
            _toggleResult.value = result
        }
    }

    fun resetToggleResult() {
        _toggleResult.value = null
    }
}
