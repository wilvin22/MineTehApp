package com.example.mineteh

import android.app.Application
import com.example.mineteh.network.ApiClient

class MineTehApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
