package com.example.mineteh

import android.app.Application
import com.example.mineteh.network.ApiClient
import com.example.mineteh.supabase.SupabaseClient

class MineTehApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
        SupabaseClient.initialize()
    }
}
