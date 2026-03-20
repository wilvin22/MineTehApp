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

class EditListingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ListingsRepository(application.applicationContext)

    private val _listing = MutableLiveData<Resource<Listing>>()
    val listing: LiveData<Resource<Listing>> = _listing

    private val _editResult = MutableLiveData<Resource<Unit>?>()
    val editResult: LiveData<Resource<Unit>?> = _editResult

    fun loadListing(id: Int) {
        _listing.value = Resource.Loading()
        viewModelScope.launch {
            _listing.value = repository.getListing(id)
        }
    }

    fun editListing(
        id: Int,
        title: String,
        description: String,
        price: Double,
        location: String,
        category: String,
        listingType: String,
        endTime: String?,
        newImageUris: List<Uri>,
        removedImagePaths: List<String>
    ) {
        _editResult.value = Resource.Loading()
        viewModelScope.launch {
            _editResult.value = repository.updateListing(
                listingId = id,
                title = title,
                description = description,
                price = price,
                location = location,
                category = category,
                listingType = listingType,
                endTime = endTime,
                newImageUris = newImageUris,
                removedImagePaths = removedImagePaths
            )
        }
    }

    fun resetEditResult() {
        _editResult.value = null
    }
}
