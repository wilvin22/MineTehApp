package com.example.mineteh.network

import android.content.Context
import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.TokenManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://didpavzminvohszuuowu.supabase.co"

    private var tokenManager: TokenManager? = null

    fun initialize(context: android.content.Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val authInterceptor by lazy {
        Interceptor { chain ->
            val token = tokenManager?.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson by lazy {
        GsonBuilder()
            .setLenient() // This allows Gson to ignore some malformed characters
            .registerTypeAdapter(
                object : TypeToken<ApiResponse<List<Listing>>>() {}.type,
                ApiResponseDeserializer()
            )
            .registerTypeAdapter(Listing::class.java, ListingDeserializer())
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
