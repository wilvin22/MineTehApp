package com.example.mineteh.model.repository

import android.app.Application
import android.util.Log
import com.example.mineteh.models.CreateOrderRequest
import com.example.mineteh.models.Order
import com.example.mineteh.models.OrderItem
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OrderRepository(private val application: Application) {
    
    companion object {
        private const val TAG = "OrderRepository"
    }
    
    /**
     * Parses orders JSON response from Supabase
     */
    private fun parseOrdersResponse(jsonData: String): List<Order> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    Order(
                        orderId = obj["order_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        userId = obj["user_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        addressId = obj["address_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        totalAmount = obj["total_amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        shippingFee = obj["shipping_fee"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 50.0,
                        paymentMethod = obj["payment_method"]?.jsonPrimitive?.content ?: "COD",
                        orderStatus = obj["order_status"]?.jsonPrimitive?.content ?: "pending",
                        notes = obj["notes"]?.jsonPrimitive?.content,
                        createdAt = obj["created_at"]?.jsonPrimitive?.content,
                        updatedAt = obj["updated_at"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing order", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseOrdersResponse", e)
            emptyList()
        }
    }
    
    /**
     * Parses order items JSON response from Supabase
     */
    private fun parseOrderItemsResponse(jsonData: String): List<OrderItem> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    OrderItem(
                        orderItemId = obj["order_item_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        orderId = obj["order_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        listingId = obj["listing_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        quantity = obj["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                        price = obj["price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        createdAt = obj["created_at"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing order item", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseOrderItemsResponse", e)
            emptyList()
        }
    }
    
    suspend fun createOrder(request: CreateOrderRequest): Resource<Order> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating order for user: ${request.userId}")
                
                // Note: This would require a different API endpoint or newer Supabase client
                // For now, return an error indicating the operation is not supported
                Resource.Error("Order creation not supported with current Supabase client version")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order", e)
                Resource.Error("Failed to create order: ${e.message}")
            }
        }
    }
    
    suspend fun getUserOrders(userId: Int): Resource<List<Order>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching orders for user: $userId")
                
                val response = SupabaseClient.database
                    .from("orders")
                    .select()
                
                if (response.data.isEmpty() || response.data == "[]") {
                    Log.d(TAG, "No orders found")
                    return@withContext Resource.Success(emptyList())
                }
                
                // Parse and filter orders by user_id
                val orders = parseOrdersResponse(response.data)
                    .filter { it.userId == userId }
                    .sortedByDescending { it.createdAt }
                
                Log.d(TAG, "Fetched ${orders.size} orders")
                Resource.Success(orders)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching orders", e)
                Resource.Error("Failed to load orders: ${e.message}")
            }
        }
    }
    
    suspend fun getOrderItems(orderId: Int): Resource<List<OrderItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching items for order: $orderId")
                
                val response = SupabaseClient.database
                    .from("order_items")
                    .select()
                
                if (response.data.isEmpty() || response.data == "[]") {
                    Log.d(TAG, "No order items found")
                    return@withContext Resource.Success(emptyList())
                }
                
                // Parse and filter order items by order_id
                val items = parseOrderItemsResponse(response.data)
                    .filter { it.orderId == orderId }
                
                Log.d(TAG, "Fetched ${items.size} order items")
                Resource.Success(items)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching order items", e)
                Resource.Error("Failed to load order items: ${e.message}")
            }
        }
    }
    
    suspend fun updateOrderStatus(orderId: Int, status: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating order status: $orderId to $status")
                
                // Note: This would require a different API endpoint or newer Supabase client
                // For now, return an error indicating the operation is not supported
                Resource.Error("Order status update not supported with current Supabase client version")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating order status", e)
                Resource.Error("Failed to update order status: ${e.message}")
            }
        }
    }
}