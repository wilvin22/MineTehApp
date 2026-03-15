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
    private const val BASE_URL = "https://mineteh.infinityfree.me/"

    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = context?.let { TokenManager(it).getToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()
        } else {
            chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()
        }
        chain.proceed(request)
    }

    private val cookieJar = object : okhttp3.CookieJar {
        private val cookieStore = mutableMapOf<String, List<okhttp3.Cookie>>()

        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(
            object : TypeToken<ApiResponse<List<Listing>>>() {}.type,
            ApiResponseDeserializer()
        )
        .registerTypeAdapter(Listing::class.java, ListingDeserializer())
        .create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
