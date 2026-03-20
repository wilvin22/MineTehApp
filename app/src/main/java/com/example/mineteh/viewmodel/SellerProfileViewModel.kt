package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.repository.SellerRepository
import com.example.mineteh.models.SellerProfileData
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class SellerProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SellerRepository(application.applicationContext)

    private val _profile = MutableLiveData<Resource<SellerProfileData>>()
    val profile: LiveData<Resource<SellerProfileData>> = _profile

    fun loadProfile(sellerId: Int) {
        _profile.value = Resource.Loading()
        viewModelScope.launch {
            _profile.postValue(repository.getSellerProfile(sellerId))
        }
    }
}
