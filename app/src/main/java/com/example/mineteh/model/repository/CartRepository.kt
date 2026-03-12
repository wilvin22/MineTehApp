package com.example.mineteh.model.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.mineteh.models.CartItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CartRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val CART_KEY = "cart_items"
    }
    
    fun getCartItems(): List<CartItem> {
        val json = prefs.getString(CART_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun addToCart(item: CartItem) {
        val items = getCartItems().toMutableList()
        
        // Check if item already exists
        val existingIndex = items.indexOfFirst { it.listingId == item.listingId }
        if (existingIndex != -1) {
            // Update quantity
            items[existingIndex].quantity += item.quantity
        } else {
            // Add new item
            items.add(item)
        }
        
        saveCart(items)
    }
    
    fun removeFromCart(listingId: Int) {
        val items = getCartItems().toMutableList()
        items.removeAll { it.listingId == listingId }
        saveCart(items)
    }
    
    fun updateQuantity(listingId: Int, quantity: Int) {
        val items = getCartItems().toMutableList()
        val index = items.indexOfFirst { it.listingId == listingId }
        if (index != -1) {
            if (quantity <= 0) {
                items.removeAt(index)
            } else {
                items[index].quantity = quantity
            }
            saveCart(items)
        }
    }
    
    fun clearCart() {
        prefs.edit().remove(CART_KEY).apply()
    }
    
    fun getCartCount(): Int {
        return getCartItems().sumOf { it.quantity }
    }
    
    fun getCartTotal(): Double {
        return getCartItems().sumOf { it.price * it.quantity }
    }
    
    private fun saveCart(items: List<CartItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(CART_KEY, json).apply()
    }
}
