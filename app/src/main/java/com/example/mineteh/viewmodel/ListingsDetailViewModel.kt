package com.example.mineteh.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mineteh.models.BidData
import com.example.mineteh.models.FavoriteData
import com.example.mineteh.models.Listing
import com.example.mineteh.model.repositories.BidsRepository
import com.example.mineteh.model.repositories.FavoritesRepository
import com.example.mineteh.model.repositories.ListingsRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class ListingDetailViewModel : ViewModel() {
    private val listingsRepository = ListingsRepository()
    private val bidsRepository = BidsRepository()
    private val favoritesRepository = FavoritesRepository()

    private val _listing = MutableLiveData<Resource<Listing>>()
    val listing: LiveData<Resource<Listing>> = _listing

    private val _bidResult = MutableLiveData<Resource<BidData>>()
    val bidResult: LiveData<Resource<BidData>> = _bidResult

    private val _favoriteResult = MutableLiveData<Resource<FavoriteData>>()
    val favoriteResult: LiveData<Resource<FavoriteData>> = _favoriteResult

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
}
