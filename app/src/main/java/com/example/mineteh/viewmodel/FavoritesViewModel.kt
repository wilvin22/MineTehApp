package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.models.Listing
import com.example.mineteh.model.repository.FavoritesRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FavoritesRepository(application.applicationContext)

    private val _favorites = MutableLiveData<Resource<List<Listing>>>()
    val favorites: LiveData<Resource<List<Listing>>> = _favorites

    private val _toggleResult = MutableLiveData<Resource<Boolean>?>()
    val toggleResult: LiveData<Resource<Boolean>?> = _toggleResult

    fun loadFavorites() {
        _favorites.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.getFavorites()
            _favorites.value = result
        }
    }

    fun toggleFavorite(listingId: Int) {
        _toggleResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.toggleFavorite(listingId)
            _toggleResult.value = result

            if (result is Resource.Success) {
                loadFavorites()
            }
        }
    }

    fun resetToggleResult() {
        _toggleResult.value = null
    }
}
