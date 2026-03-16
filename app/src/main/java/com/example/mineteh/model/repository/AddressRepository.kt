package com.example.mineteh.model.repository

import android.app.Application
import android.util.Log
import com.example.mineteh.models.CreateAddressRequest
import com.example.mineteh.models.UserAddress
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AddressRepository(private val application: Application) {
    
    companion object {
        private const val TAG = "AddressRepository"
    }
    
    /**
     * Parses addresses JSON response from Supabase
     */
    private fun parseAddressesResponse(jsonData: String): List<UserAddress> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    UserAddress(
                        addressId = obj["address_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        userId = obj["user_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        addressType = obj["address_type"]?.jsonPrimitive?.content ?: "home",
                        recipientName = obj["recipient_name"]?.jsonPrimitive?.content ?: "",
                        phoneNumber = obj["phone_number"]?.jsonPrimitive?.content,
                        streetAddress = obj["street_address"]?.jsonPrimitive?.content ?: "",
                        city = obj["city"]?.jsonPrimitive?.content ?: "",
                        province = obj["province"]?.jsonPrimitive?.content ?: "",
                        postalCode = obj["postal_code"]?.jsonPrimitive?.content,
                        isDefault = obj["is_default"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                        createdAt = obj["created_at"]?.jsonPrimitive?.content,
                        updatedAt = obj["updated_at"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing address", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseAddressesResponse", e)
            emptyList()
        }
    }
    
    suspend fun getUserAddresses(userId: Int): Resource<List<UserAddress>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AddressRepository", "Fetching addresses for user: $userId")
                
                val response = SupabaseClient.database
                    .from("user_addresses")
                    .select()
                
                if (response.data.isEmpty() || response.data == "[]") {
                    Log.d("AddressRepository", "No addresses found")
                    return@withContext Resource.Success(emptyList())
                }
                
                // Parse and filter addresses by user_id
                val addresses = parseAddressesResponse(response.data)
                    .filter { it.userId == userId }
                    .sortedByDescending { it.isDefault }
                
                Log.d("AddressRepository", "Fetched ${addresses.size} addresses")
                Resource.Success(addresses)
                
            } catch (e: Exception) {
                Log.e("AddressRepository", "Error fetching addresses", e)
                Resource.Error("Failed to load addresses: ${e.message}")
            }
        }
    }
    
    suspend fun getDefaultAddress(userId: Int): Resource<UserAddress?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AddressRepository", "Fetching default address for user: $userId")
                
                val response = SupabaseClient.database
                    .from("user_addresses")
                    .select()
                
                if (response.data.isEmpty() || response.data == "[]") {
                    Log.d("AddressRepository", "No addresses found")
                    return@withContext Resource.Success(null)
                }
                
                // Parse and find default address for user
                val addresses = parseAddressesResponse(response.data)
                    .filter { it.userId == userId && it.isDefault }
                
                val defaultAddress = addresses.firstOrNull()
                Log.d("AddressRepository", "Default address: ${defaultAddress?.recipientName}")
                
                Resource.Success(defaultAddress)
                
            } catch (e: Exception) {
                Log.e("AddressRepository", "Error fetching default address", e)
                Resource.Error("Failed to load default address: ${e.message}")
            }
        }
    }
    
    suspend fun createAddress(request: CreateAddressRequest): Resource<UserAddress> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating address for user: ${request.userId}")
                
                // Note: This would require a different API endpoint or newer Supabase client
                // For now, return an error indicating the operation is not supported
                Resource.Error("Address creation not supported with current Supabase client version")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating address", e)
                Resource.Error("Failed to create address: ${e.message}")
            }
        }
    }
    
    suspend fun updateAddress(addressId: Int, request: CreateAddressRequest): Resource<UserAddress> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating address: $addressId")
                
                // Note: This would require a different API endpoint or newer Supabase client
                // For now, return an error indicating the operation is not supported
                Resource.Error("Address update not supported with current Supabase client version")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating address", e)
                Resource.Error("Failed to update address: ${e.message}")
            }
        }
    }
    
    suspend fun deleteAddress(addressId: Int): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting address: $addressId")
                
                // Note: This would require a different API endpoint or newer Supabase client
                // For now, return an error indicating the operation is not supported
                Resource.Error("Address deletion not supported with current Supabase client version")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting address", e)
                Resource.Error("Failed to delete address: ${e.message}")
            }
        }
    }
    
    suspend fun setDefaultAddress(userId: Int, addressId: Int): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Setting default address: $addressId for user: $userId")
                
                // Note: This would require a different API endpoint or newer Supabase client
                // For now, return an error indicating the operation is not supported
                Resource.Error("Setting default address not supported with current Supabase client version")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting default address", e)
                Resource.Error("Failed to set default address: ${e.message}")
            }
        }
    }
}