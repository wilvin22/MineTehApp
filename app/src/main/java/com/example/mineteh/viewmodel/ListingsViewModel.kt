package com.example.mineteh.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repositories.ListingsRepository
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class ListingsViewModel : ViewModel() {
    private val repository = ListingsRepository()

    private val _listings = MutableLiveData<Resource<List<Listing>>>()
    val listings: LiveData<Resource<List<Listing>>> = _listings

    private val _selectedCategory = MutableLiveData<String?>()
    val selectedCategory: LiveData<String?> = _selectedCategory

    private val _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> = _searchQuery

    init {
        loadListings()
    }

    fun loadListings(
        category: String? = _selectedCategory.value,
        search: String? = _searchQuery.value
    ) {
        _listings.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.getListings(
                category = category,
                search = search
            )
            _listings.value = result
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        loadListings(category = category)
    }

    fun search(query: String?) {
        _searchQuery.value = query
        loadListings(search = query)
    }

    fun refresh() {
        loadListings()
    }
}