package com.example.mineteh.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

object NetworkUtils {
    private const val TAG = "NetworkUtils"
    
    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Test DNS resolution for Supabase hostname
     */
    suspend fun testSupabaseConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing DNS resolution for didpavzminvohszuuowu.supabase.co...")
            val address = InetAddress.getByName("didpavzminvohszuuowu.supabase.co")
            Log.d(TAG, "DNS resolution successful: ${address.hostAddress}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "DNS resolution failed", e)
            false
        }
    }
}
