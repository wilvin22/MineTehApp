package com.example.mineteh.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mineteh.models.Listing
import com.example.mineteh.model.repositories.FavoritesRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    private val repository = FavoritesRepository()

    private val _favorites = MutableLiveData<Resource<List<Listing>>>()
    val favorites: LiveData<Resource<List<Listing>>> = _favorites

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        _favorites.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.getFavorites()
            _favorites.value = result
        }
    }

    fun removeFavorite(listingId: Int) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(listingId)
            if (result is Resource.Success) {
                // Reload favorites list
                loadFavorites()
            }
        }
    }

    fun refresh() {
        loadFavorites()
    }
}