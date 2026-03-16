package com.example.mineteh.model.repository

import android.app.Application
import android.util.Log
import com.example.mineteh.models.CreateAddressRequest
import com.example.mineteh.models.UserAddress
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressRepository(private val application: Application) {
    
    private val supabase = SupabaseClient.client
    
    suspend fun getUserAddresses(userId: Int): Resource<List<UserAddress>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AddressRepository", "Fetching addresses for user: $userId")
                
                val addresses = supabase
                    .from("user_addresses")
                    .select()
                    .filter {
                        eq("user_id", userId)
                    }
                    .decodeList<UserAddress>()
                
                Log.d("AddressRepository", "Fetched ${addresses.size} addresses")
                Resource.Success(addresses.sortedByDescending { it.isDefault })
                
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
                
                val addresses = supabase
                    .from("user_addresses")
                    .select()
                    .filter {
                        eq("user_id", userId)
                        eq("is_default", true)
                    }
                    .decodeList<UserAddress>()
                
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
                Log.d("AddressRepository", "Creating address for user: ${request.userId}")
                
                val createdAddress = supabase
                    .from("user_addresses")
                    .insert(request)
                    .decodeSingle<UserAddress>()
                
                Log.d("AddressRepository", "Address created with ID: ${createdAddress.addressId}")
                Resource.Success(createdAddress)
                
            } catch (e: Exception) {
                Log.e("AddressRepository", "Error creating address", e)
                Resource.Error("Failed to create address: ${e.message}")
            }
        }
    }
    
    suspend fun updateAddress(addressId: Int, request: CreateAddressRequest): Resource<UserAddress> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AddressRepository", "Updating address: $addressId")
                
                val updatedAddress = supabase
                    .from("user_addresses")
                    .update(request)
                    .filter {
                        eq("address_id", addressId)
                    }
                    .decodeSingle<UserAddress>()
                
                Log.d("AddressRepository", "Address updated: $addressId")
                Resource.Success(updatedAddress)
                
            } catch (e: Exception) {
                Log.e("AddressRepository", "Error updating address", e)
                Resource.Error("Failed to update address: ${e.message}")
            }
        }
    }
    
    suspend fun deleteAddress(addressId: Int): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AddressRepository", "Deleting address: $addressId")
                
                supabase
                    .from("user_addresses")
                    .delete()
                    .filter {
                        eq("address_id", addressId)
                    }
                
                Log.d("AddressRepository", "Address deleted: $addressId")
                Resource.Success(true)
                
            } catch (e: Exception) {
                Log.e("AddressRepository", "Error deleting address", e)
                Resource.Error("Failed to delete address: ${e.message}")
            }
        }
    }
    
    suspend fun setDefaultAddress(userId: Int, addressId: Int): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AddressRepository", "Setting default address: $addressId for user: $userId")
                
                // First, set all addresses to non-default
                supabase
                    .from("user_addresses")
                    .update(mapOf("is_default" to false))
                    .filter {
                        eq("user_id", userId)
                    }
                
                // Then set the selected address as default
                supabase
                    .from("user_addresses")
                    .update(mapOf("is_default" to true))
                    .filter {
                        eq("address_id", addressId)
                        eq("user_id", userId)
                    }
                
                Log.d("AddressRepository", "Default address set: $addressId")
                Resource.Success(true)
                
            } catch (e: Exception) {
                Log.e("AddressRepository", "Error setting default address", e)
                Resource.Error("Failed to set default address: ${e.message}")
            }
        }
    }
}