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
        fetchListings()
    }

    fun fetchListings(category: String? = null, type: String? = null, search: String? = null) {
        _listings.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getListings(category, type, search)
            _listings.value = result
        }
    }
}
