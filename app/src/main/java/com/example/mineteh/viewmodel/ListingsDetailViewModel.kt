package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.models.BidData
import com.example.mineteh.models.Listing
import com.example.mineteh.model.repository.BidsRepository
import com.example.mineteh.model.repository.FavoritesRepository
import com.example.mineteh.model.repository.ListingsRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class ListingDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val listingsRepository = ListingsRepository(application.applicationContext)
    private val bidsRepository = BidsRepository(application.applicationContext)
    private val favoritesRepository = FavoritesRepository(application.applicationContext)

    private val _listing = MutableLiveData<Resource<Listing>>()
    val listing: LiveData<Resource<Listing>> = _listing

    private val _bidResult = MutableLiveData<Resource<BidData>?>()
    val bidResult: LiveData<Resource<BidData>?> = _bidResult

    private val _favoriteResult = MutableLiveData<Resource<Boolean>?>()
    val favoriteResult: LiveData<Resource<Boolean>?> = _favoriteResult
    
    private val _statusUpdateResult = MutableLiveData<Resource<Boolean>?>()
    val statusUpdateResult: LiveData<Resource<Boolean>?> = _statusUpdateResult

    fun loadListing(listingId: Int) {
        _listing.value = Resource.Loading()

        viewModelScope.launch {
            val result = listingsRepository.getListing(listingId)
            _listing.value = result
        }
    }

    fun placeBid(listingId: Int, bidAmount: Double) {
        // Validate bid amount
        val currentListing = _listing.value?.data
        if (currentListing == null) {
            _bidResult.value = Resource.Error("Listing not loaded")
            return
        }

        if (bidAmount <= 0) {
            _bidResult.value = Resource.Error("Bid amount must be greater than 0")
            return
        }

        _bidResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = bidsRepository.placeBid(listingId, bidAmount)
            _bidResult.value = result

            // Reload listing to get updated bid info
            if (result is Resource.Success) {
                loadListing(listingId)
            }
        }
    }

    fun toggleFavorite(listingId: Int) {
        _favoriteResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = favoritesRepository.toggleFavorite(listingId)
            _favoriteResult.value = result

            // Reload listing to get updated favorite status
            if (result is Resource.Success) {
                loadListing(listingId)
            }
        }
    }

    fun resetBidResult() {
        _bidResult.value = null
    }

    fun resetFavoriteResult() {
        _favoriteResult.value = null
    }
    
    fun updateListingStatus(listingId: Int, status: String) {
        _statusUpdateResult.value = Resource.Loading()
        
        viewModelScope.launch {
            val result = listingsRepository.updateListingStatus(listingId, status)
            _statusUpdateResult.value = result
            
            // Reload listing to get updated status
            if (result is Resource.Success) {
                loadListing(listingId)
            }
        }
    }
    
    fun resetStatusUpdateResult() {
        _statusUpdateResult.value = null
    }
}
