package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mineteh.models.CartItem
import com.example.mineteh.model.repository.CartRepository

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val cartRepository = CartRepository(application.applicationContext)
    
    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> = _cartItems
    
    private val _cartTotal = MutableLiveData<Double>()
    val cartTotal: LiveData<Double> = _cartTotal
    
    private val _cartCount = MutableLiveData<Int>()
    val cartCount: LiveData<Int> = _cartCount
    
    init {
        loadCart()
    }
    
    fun loadCart() {
        val items = cartRepository.getCartItems()
        _cartItems.value = items
        _cartTotal.value = cartRepository.getCartTotal()
        _cartCount.value = cartRepository.getCartCount()
    }
    
    fun addToCart(item: CartItem) {
        cartRepository.addToCart(item)
        loadCart()
    }
    
    fun removeFromCart(listingId: Int) {
        cartRepository.removeFromCart(listingId)
        loadCart()
    }
    
    fun updateQuantity(listingId: Int, quantity: Int) {
        cartRepository.updateQuantity(listingId, quantity)
        loadCart()
    }
    
    fun clearCart() {
        cartRepository.clearCart()
        loadCart()
    }
}
