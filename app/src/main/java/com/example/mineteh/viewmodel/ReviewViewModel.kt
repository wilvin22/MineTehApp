package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.ReviewRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReviewRepository(application.applicationContext)

    private val _submitResult = MutableLiveData<Resource<Unit>?>()
    val submitResult: LiveData<Resource<Unit>?> = _submitResult

    private val _hasReviewed = MutableLiveData<Resource<Boolean>>()
    val hasReviewed: LiveData<Resource<Boolean>> = _hasReviewed

    fun submitReview(sellerId: Int, listingId: Int, rating: Int, comment: String?) {
        _submitResult.value = Resource.Loading()
        viewModelScope.launch {
            _submitResult.postValue(repository.submitReview(sellerId, listingId, rating, comment))
        }
    }

    fun hasReviewed(listingId: Int) {
        viewModelScope.launch {
            _hasReviewed.postValue(repository.hasReviewed(listingId))
        }
    }

    fun resetSubmitResult() {
        _submitResult.value = null
    }
}
