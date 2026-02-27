package com.example.mineteh.model.repository

import android.content.Context
import android.net.Uri
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ListingsRepository(private val context: Context) {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(context)

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getListings(category, type, search, limit, offset)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to load listings")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getListing(id: Int) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getListing(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to load listing")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun createListing(
        title: String,
        description: String,
        price: Double,
        location: String,
        category: String,
        listingType: String,
        endTime: String?,
        minBidIncrement: Double?,
        imageUris: List<Uri>
    ): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            // Check if logged in
            if (!tokenManager.isLoggedIn()) {
                return@withContext Resource.Error("Not authenticated")
            }

            val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val pricePart = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val locationPart = location.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryPart = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val typePart = listingType.toRequestBody("text/plain".toMediaTypeOrNull())
            val endTimePart = endTime?.toRequestBody("text/plain".toMediaTypeOrNull())
            val incrementPart = minBidIncrement?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val imageParts = imageUris.mapNotNull { uri ->
                prepareImagePart(uri)
            }

            if (imageParts.isEmpty()) {
                return@withContext Resource.Error("Please select at least one image")
            }

            val response = apiService.createListing(
                titlePart,
                descriptionPart,
                pricePart,
                locationPart,
                categoryPart,
                typePart,
                endTimePart,
                incrementPart,
                imageParts
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to create listing")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    private fun prepareImagePart(uri: Uri): MultipartBody.Part? {
        val file = getFileFromUri(uri) ?: return null
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images[]", file.name, requestFile)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}_${(0..1000).random()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}
