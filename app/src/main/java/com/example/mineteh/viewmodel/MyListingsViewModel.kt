package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.models.Listing
import com.example.mineteh.model.repository.ListingsRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class MyListingsViewModel(application: Application) : AndroidViewModel(application) {
    private val listingsRepository = ListingsRepository(application.applicationContext)

    private val _listings = MutableLiveData<Resource<List<Listing>>>()
    val listings: LiveData<Resource<List<Listing>>> = _listings

    private val _deleteResult = MutableLiveData<Resource<Boolean>?>()
    val deleteResult: LiveData<Resource<Boolean>?> = _deleteResult

    private val _statusUpdateResult = MutableLiveData<Resource<Boolean>?>()
    val statusUpdateResult: LiveData<Resource<Boolean>?> = _statusUpdateResult

    fun loadUserListings() {
        _listings.value = Resource.Loading()

        viewModelScope.launch {
            val result = listingsRepository.getUserListings()
            _listings.value = result
        }
    }

    fun deleteListing(listingId: Int) {
        _deleteResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = listingsRepository.deleteListing(listingId)
            _deleteResult.value = result

            // Reload listings after successful delete
            if (result is Resource.Success) {
                loadUserListings()
            }
        }
    }

    fun updateListingStatus(listingId: Int, status: String) {
        _statusUpdateResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = listingsRepository.updateListingStatus(listingId, status)
            _statusUpdateResult.value = result

            // Reload listings after successful update
            if (result is Resource.Success) {
                loadUserListings()
            }
        }
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }

    fun resetStatusUpdateResult() {
        _statusUpdateResult.value = null
    }
}
