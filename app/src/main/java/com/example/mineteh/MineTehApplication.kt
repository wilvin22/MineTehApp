package com.example.mineteh

import android.app.Application
import com.example.mineteh.network.ApiClient
import com.example.mineteh.supabase.SupabaseClient

class MineTehApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Version marker to verify new code is running
        android.util.Log.d("MineTehApp", "===========================================")
        android.util.Log.d("MineTehApp", "MineTeh App Starting - VERSION 2.0 (SUPABASE)")
        android.util.Log.d("MineTehApp", "===========================================")
        
        ApiClient.initialize(this)
        SupabaseClient.initialize()
        
        android.util.Log.d("MineTehApp", "Application initialization complete")
    }
}
