package com.example.mineteh.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class UserAddress(
    @SerializedName("address_id") val addressId: Int = 0,
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("address_type") val addressType: String = "home", // home, work, other
    @SerializedName("recipient_name") val recipientName: String,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("street_address") val streetAddress: String,
    val city: String,
    val province: String,
    @SerializedName("postal_code") val postalCode: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val fullAddress: String
        get() = buildString {
            append(streetAddress)
            if (city.isNotEmpty()) append(", $city")
            if (province.isNotEmpty()) append(", $province")
            if (!postalCode.isNullOrEmpty()) append(" $postalCode")
        }
    
    val displayName: String
        get() = when (addressType.lowercase()) {
            "home" -> "🏠 Home"
            "work" -> "🏢 Work"
            else -> "📍 Other"
        }
}

@Serializable
data class CreateAddressRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("address_type") val addressType: String,
    @SerializedName("recipient_name") val recipientName: String,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("street_address") val streetAddress: String,
    val city: String,
    val province: String,
    @SerializedName("postal_code") val postalCode: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false
)

@Serializable
data class Order(
    @SerializedName("order_id") val orderId: Int = 0,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("address_id") val addressId: Int,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("shipping_fee") val shippingFee: Double = 50.0,
    @SerializedName("payment_method") val paymentMethod: String = "COD",
    @SerializedName("order_status") val orderStatus: String = "pending",
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

@Serializable
data class OrderItem(
    @SerializedName("order_item_id") val orderItemId: Int = 0,
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("listing_id") val listingId: Int,
    val quantity: Int = 1,
    val price: Double,
    @SerializedName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateOrderRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("address_id") val addressId: Int,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("shipping_fee") val shippingFee: Double = 50.0,
    @SerializedName("payment_method") val paymentMethod: String = "COD",
    val notes: String? = null,
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    @SerializedName("listing_id") val listingId: Int,
    val quantity: Int = 1,
    val price: Double
)

enum class PaymentMethod(val displayName: String, val value: String) {
    COD("Cash on Delivery", "COD"),
    GCASH("GCash", "GCASH"),
    PAYMAYA("PayMaya", "PAYMAYA"),
    BANK_TRANSFER("Bank Transfer", "BANK_TRANSFER")
}

enum class AddressType(val displayName: String, val value: String, val icon: String) {
    HOME("Home", "home", "🏠"),
    WORK("Work", "work", "🏢"),
    OTHER("Other", "other", "📍")
}