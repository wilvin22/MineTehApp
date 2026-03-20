package com.example.mineteh

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mineteh.databinding.MyOrdersBinding
import com.example.mineteh.model.repository.ReviewRepository
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.view.RateSellerDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class SupabaseOrder(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("seller_id") val sellerId: Int = 0,
    @SerialName("listing_id") val listingId: Int = 0,
    @SerialName("listing_title") val listingTitle: String = "",
    @SerialName("listing_image") val listingImage: String? = null,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("status") val status: String = "pending",
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

class MyOrdersActivity : AppCompatActivity(), RateSellerDialogFragment.RateSellerListener {

    private lateinit var binding: MyOrdersBinding
    private lateinit var tokenManager: TokenManager
    private val allOrders = mutableListOf<SupabaseOrder>()
    private val tabStatuses = listOf(null, "pending", "to_ship", "to_receive", "completed")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MyOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }
        fetchOrders()
    }

    private fun fetchOrders() {
        val userId = tokenManager.getUserId()
        if (userId == -1) return
        lifecycleScope.launch {
            try {
                val orders = withContext(Dispatchers.IO) { fetchOrdersFromSupabase(userId) }
                allOrders.clear()
                allOrders.addAll(orders)
                setupViewPager()
                val listingId = getListingIdFromIntent()
                if (listingId != -1) binding.viewPager.currentItem = 0
            } catch (e: Exception) {
                Log.e("MyOrdersActivity", "Error fetching orders", e)
                setupViewPager()
            }
        }
    }

    private suspend fun fetchOrdersFromSupabase(userId: Int): List<SupabaseOrder> {
        return try {
            val response = SupabaseClient.database
                .from("orders")
                .select(columns = Columns.raw(
                    "order_id, user_id, seller_id, listing_id, total_amount, status, created_at, " +
                    "listings(title, listing_images(image_url)), accounts!orders_seller_id_fkey(username)"
                )) {
                    filter { eq("user_id", userId) }
                }
            parseOrdersJson(response.data)
        } catch (e: Exception) {
            Log.e("MyOrdersActivity", "Joined query failed, trying simple query", e)
            try {
                val response = SupabaseClient.database
                    .from("orders")
                    .select() { filter { eq("user_id", userId) } }
                parseSimpleOrdersJson(response.data)
            } catch (e2: Exception) {
                Log.e("MyOrdersActivity", "Fallback query also failed", e2)
                emptyList()
            }
        }
    }

    private fun parseOrdersJson(jsonData: String): List<SupabaseOrder> {
        if (jsonData.isBlank() || jsonData == "[]") return emptyList()
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val array = json.parseToJsonElement(jsonData).jsonArray
            array.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val listingObj = obj["listings"]?.jsonObject
                    val imagesArray = listingObj?.get("listing_images")?.jsonArray
                    val firstImage = imagesArray?.firstOrNull()?.jsonObject?.get("image_url")?.jsonPrimitive?.content
                    val accountObj = obj["accounts"]?.jsonObject
                    val sellerName = accountObj?.get("username")?.jsonPrimitive?.content ?: ""
                    SupabaseOrder(
                        orderId = obj["order_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        userId = obj["user_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        sellerId = obj["seller_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        listingId = obj["listing_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        listingTitle = listingObj?.get("title")?.jsonPrimitive?.content ?: "",
                        listingImage = firstImage,
                        totalAmount = obj["total_amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        status = obj["status"]?.jsonPrimitive?.content ?: "pending",
                        sellerName = sellerName,
                        createdAt = obj["created_at"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) {
                    Log.e("MyOrdersActivity", "Error parsing order element", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MyOrdersActivity", "Error parsing orders JSON", e)
            emptyList()
        }
    }

    private fun parseSimpleOrdersJson(jsonData: String): List<SupabaseOrder> {
        if (jsonData.isBlank() || jsonData == "[]") return emptyList()
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val array = json.parseToJsonElement(jsonData).jsonArray
            array.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    SupabaseOrder(
                        orderId = obj["order_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        userId = obj["user_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        sellerId = obj["seller_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        listingId = obj["listing_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        listingTitle = "Order #${obj["order_id"]?.jsonPrimitive?.content}",
                        totalAmount = obj["total_amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        status = obj["status"]?.jsonPrimitive?.content ?: "pending",
                        createdAt = obj["created_at"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun setupViewPager() {
        val adapter = OrdersPagerAdapter()
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "All"; 1 -> "To Pay"; 2 -> "To Ship"; 3 -> "To Receive"; else -> "Completed"
            }
        }.attach()
    }

    override fun onRatingSubmitted() {
        Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show()
        fetchOrders()
    }

    inner class OrdersPagerAdapter : RecyclerView.Adapter<OrdersPagerAdapter.PagerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
            val recyclerView = RecyclerView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(context)
                setPadding(0, 16, 0, 16)
                clipToPadding = false
            }
            return PagerViewHolder(recyclerView)
        }

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            val rv = holder.itemView as RecyclerView
            val statusFilter = tabStatuses[position]
            val filtered = if (statusFilter == null) allOrders.toList()
                           else allOrders.filter { it.status == statusFilter }
            rv.adapter = OrderItemAdapter(filtered)
        }

        override fun getItemCount(): Int = 5
        inner class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    inner class OrderItemAdapter(private val orders: List<SupabaseOrder>) :
        RecyclerView.Adapter<OrderItemAdapter.OrderViewHolder>() {

        inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sellerName: TextView = view.findViewById(R.id.sellerName)
            val orderStatus: TextView = view.findViewById(R.id.orderStatus)
            val productName: TextView = view.findViewById(R.id.productName)
            val productImage: ImageView = view.findViewById(R.id.productImage)
            val totalAmount: TextView = view.findViewById(R.id.totalAmount)
            val btnSecondary: MaterialButton = view.findViewById(R.id.btnSecondary)
            val btnPrimary: MaterialButton = view.findViewById(R.id.btnPrimary)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            val order = orders[position]
            holder.sellerName.text = if (order.sellerName.isNotEmpty()) order.sellerName else "Seller"
            holder.orderStatus.text = formatStatus(order.status)
            holder.productName.text = if (order.listingTitle.isNotEmpty()) order.listingTitle else "Order #${order.orderId}"
            holder.totalAmount.text = "Total: ₱ %.2f".format(order.totalAmount)

            if (!order.listingImage.isNullOrEmpty()) {
                Glide.with(holder.productImage.context).load(order.listingImage).centerCrop().into(holder.productImage)
            }

            if (order.status == "completed") {
                holder.btnSecondary.visibility = View.VISIBLE
                holder.btnSecondary.text = "Rate Seller"
                holder.btnSecondary.isEnabled = false
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        ReviewRepository(this@MyOrdersActivity).hasReviewed(order.listingId)
                    }
                    if (result is Resource.Success && result.data == false) {
                        holder.btnSecondary.isEnabled = true
                        holder.btnSecondary.setOnClickListener {
                            RateSellerDialogFragment.newInstance(
                                sellerId = order.sellerId,
                                listingId = order.listingId,
                                listener = this@MyOrdersActivity
                            ).show(supportFragmentManager, "rate_seller")
                        }
                    } else {
                        holder.btnSecondary.visibility = View.GONE
                    }
                }
            } else {
                holder.btnSecondary.visibility = View.GONE
            }
            holder.btnPrimary.visibility = View.GONE
        }

        override fun getItemCount(): Int = orders.size

        private fun formatStatus(status: String): String = when (status) {
            "pending" -> "To Pay"
            "to_ship" -> "To Ship"
            "to_receive" -> "To Receive"
            "completed" -> "Completed"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getListingIdFromIntent(): Int {
        intent.data?.let { uri ->
            if (uri.scheme == "mineteh" && uri.host == "orders") {
                val pathSegments = uri.pathSegments
                if (pathSegments.isNotEmpty()) return pathSegments[0].toIntOrNull() ?: -1
            }
        }
        return intent.getIntExtra("listing_id", -1)
    }
}
