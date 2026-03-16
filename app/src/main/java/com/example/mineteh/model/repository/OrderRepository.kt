package com.example.mineteh.model.repository

import android.app.Application
import android.util.Log
import com.example.mineteh.models.CreateOrderRequest
import com.example.mineteh.models.Order
import com.example.mineteh.models.OrderItem
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(private val application: Application) {
    
    private val supabase = SupabaseClient.client
    
    suspend fun createOrder(request: CreateOrderRequest): Resource<Order> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Creating order for user: ${request.userId}")
                
                // Create the order
                val orderData = mapOf(
                    "user_id" to request.userId,
                    "address_id" to request.addressId,
                    "total_amount" to request.totalAmount,
                    "shipping_fee" to request.shippingFee,
                    "payment_method" to request.paymentMethod,
                    "notes" to request.notes
                )
                
                val createdOrder = supabase
                    .from("orders")
                    .insert(orderData)
                    .decodeSingle<Order>()
                
                Log.d("OrderRepository", "Order created with ID: ${createdOrder.orderId}")
                
                // Create order items
                val orderItems = request.items.map { item ->
                    mapOf(
                        "order_id" to createdOrder.orderId,
                        "listing_id" to item.listingId,
                        "quantity" to item.quantity,
                        "price" to item.price
                    )
                }
                
                supabase
                    .from("order_items")
                    .insert(orderItems)
                
                Log.d("OrderRepository", "Created ${orderItems.size} order items")
                Resource.Success(createdOrder)
                
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error creating order", e)
                Resource.Error("Failed to create order: ${e.message}")
            }
        }
    }
    
    suspend fun getUserOrders(userId: Int): Resource<List<Order>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Fetching orders for user: $userId")
                
                val orders = supabase
                    .from("orders")
                    .select()
                    .filter {
                        eq("user_id", userId)
                    }
                    .decodeList<Order>()
                
                Log.d("OrderRepository", "Fetched ${orders.size} orders")
                Resource.Success(orders.sortedByDescending { it.createdAt })
                
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error fetching orders", e)
                Resource.Error("Failed to load orders: ${e.message}")
            }
        }
    }
    
    suspend fun getOrderItems(orderId: Int): Resource<List<OrderItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Fetching items for order: $orderId")
                
                val items = supabase
                    .from("order_items")
                    .select()
                    .filter {
                        eq("order_id", orderId)
                    }
                    .decodeList<OrderItem>()
                
                Log.d("OrderRepository", "Fetched ${items.size} order items")
                Resource.Success(items)
                
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error fetching order items", e)
                Resource.Error("Failed to load order items: ${e.message}")
            }
        }
    }
    
    suspend fun updateOrderStatus(orderId: Int, status: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Updating order status: $orderId to $status")
                
                supabase
                    .from("orders")
                    .update(mapOf("order_status" to status))
                    .filter {
                        eq("order_id", orderId)
                    }
                
                Log.d("OrderRepository", "Order status updated: $orderId")
                Resource.Success(true)
                
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error updating order status", e)
                Resource.Error("Failed to update order status: ${e.message}")
            }
        }
    }
}