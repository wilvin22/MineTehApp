package com.example.mineteh.network

import android.content.Context
import com.example.mineteh.model.ApiService
import com.example.mineteh.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // IMPORTANT: Change this to your actual server URL
    // For emulator: http://10.0.2.2/MineTeh/api/v1/
    // For physical device on same WiFi: http://YOUR_PC_IP/MineTeh/api/v1/
    // For production: https://yourdomain.com/api/v1/
    private const val BASE_URL = "http://192.168.18.4/MineTeh/api/v1/"

    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = context?.let { TokenManager.getToken(it) }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
