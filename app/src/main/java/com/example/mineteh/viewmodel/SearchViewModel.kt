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

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ListingsRepository(application)

    private val _searchResults = MutableLiveData<Resource<List<Listing>>>()
    val searchResults: LiveData<Resource<List<Listing>>> = _searchResults

    fun searchListings(
        query: String? = null,
        category: String? = null,
        type: String? = null
    ) {
        viewModelScope.launch {
            _searchResults.value = Resource.Loading()
            
            val result = repository.getListings(
                category = category,
                type = type,
                search = query?.takeIf { it.isNotEmpty() }
            )
            
            _searchResults.value = result
        }
    }
}