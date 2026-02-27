package com.example.mineteh.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.ListingsRepository
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class CreateListingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ListingsRepository(application.applicationContext)

    private val _createStatus = MutableLiveData<Resource<Listing>?>()
    val createStatus: LiveData<Resource<Listing>?> = _createStatus

    fun createListing(
        title: String,
        description: String,
        price: Double,
        location: String,
        category: String,
        listingType: String,
        endTime: String? = null,
        minBidIncrement: Double? = null,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            _createStatus.value = Resource.Loading()

            val result = repository.createListing(
                title = title,
                description = description,
                price = price,
                location = location,
                category = category,
                listingType = listingType,
                endTime = endTime,
                minBidIncrement = minBidIncrement,
                imageUris = imageUris
            )
            _createStatus.value = result
        }
    }

    fun resetStatus() {
        _createStatus.value = null
    }
}
