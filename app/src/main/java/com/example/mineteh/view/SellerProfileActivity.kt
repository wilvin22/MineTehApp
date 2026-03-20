package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.repository.ListingsRepository
import com.example.mineteh.models.SellerProfileData
import com.example.mineteh.utils.AvatarUtils
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.viewmodel.SellerProfileViewModel
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ReviewItem(
    val reviewerName: String,
    val rating: Float,
    val comment: String,
    val date: String
)

class SellerProfileActivity : AppCompatActivity() {

    private val viewModel: SellerProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_profile)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val sellerId = intent.getIntExtra("seller_id", -1)
        if (sellerId == -1) {
            Toast.makeText(this, "Invalid seller", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.profile.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading()
                is Resource.Success -> resource.data?.let { bindProfile(it, sellerId) } ?: showError("No data")
                is Resource.Error -> showError(resource.message ?: "Failed to load profile")
                null -> {}
            }
        }

        viewModel.loadProfile(sellerId)
    }

    private fun bindProfile(data: SellerProfileData, sellerId: Int) {
        showContent()

        val fullName = "${data.firstName} ${data.lastName}".trim()

        findViewById<TextView>(R.id.tvFullName).text =
            if (fullName.isNotEmpty()) fullName else data.username
        findViewById<TextView>(R.id.tvUsername).text = "@${data.username}"

        // Member since
        val memberSince = data.createdAt?.let {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(it)
                val out = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                "Member since ${out.format(date!!)}"
            } catch (e: Exception) { "Member since —" }
        } ?: "Member since —"
        findViewById<TextView>(R.id.tvMemberSince).text = memberSince

        // Rating
        val rating = data.averageRating.toFloat()
        findViewById<android.widget.RatingBar>(R.id.ratingBar).rating = rating
        val reviewCount = data.reviewCount ?: 0
        findViewById<TextView>(R.id.tvRating).text =
            if (data.averageRating > 0) "${String.format("%.0f", data.averageRating)} ($reviewCount reviews)"
            else "No ratings yet"

        // Stats
        val totalListings = (data.activeListingCount) + (data.soldCount)
        findViewById<TextView>(R.id.tvTotalListings).text = totalListings.toString()
        findViewById<TextView>(R.id.tvSoldCount).text = data.soldCount.toString()
        findViewById<TextView>(R.id.tvReviewCount).text = reviewCount.toString()
        findViewById<TextView>(R.id.tvAvgRating).text =
            if (data.averageRating > 0) String.format("%.0f", data.averageRating) else "—"

        // Avatar
        AvatarUtils.bindAvatar(
            view = findViewById(R.id.ivAvatar),
            firstName = data.firstName,
            lastName = data.lastName,
            accountId = data.accountId,
            avatarUrl = data.avatarUrl,
            context = this
        )

        // Message button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMessage).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("other_user_id", sellerId)
                putExtra("other_user_name", if (fullName.isNotEmpty()) fullName else data.username)
            })
        }

        // Load listings
        loadSellerListings(sellerId, data.activeListingCount)

        // Load dummy reviews (replace with real data later)
        loadDummyReviews(reviewCount)

        // Already reviewed banner — show if current user has reviewed
        val currentUserId = TokenManager(this).getUserId()
        // For now hidden; wire up real check later
        findViewById<View>(R.id.bannerAlreadyReviewed).visibility = View.GONE
    }

    private fun loadSellerListings(sellerId: Int, activeCount: Int) {
        val tvHeader = findViewById<TextView>(R.id.tvListingsHeader)
        val rv = findViewById<RecyclerView>(R.id.rvListings)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ListingsRepository(this@SellerProfileActivity).getListings()
                }
                if (result is Resource.Success) {
                    val sellerListings = result.data?.filter {
                        it.seller?.accountId == sellerId && it.status.equals("active", ignoreCase = true)
                    } ?: emptyList()
                    tvHeader.text = "Active Listings (${sellerListings.size})"
                    rv.layoutManager = GridLayoutManager(this@SellerProfileActivity, 2)
                    rv.adapter = SellerItemAdapter(sellerListings)
                }
            } catch (e: Exception) {
                android.util.Log.e("SellerProfileActivity", "Error loading listings", e)
            }
        }
    }

    private fun loadDummyReviews(reviewCount: Int) {
        val tvHeader = findViewById<TextView>(R.id.tvReviewsHeader)
        val rv = findViewById<RecyclerView>(R.id.rvReviews)
        tvHeader.text = "Reviews ($reviewCount)"

        // Dummy data until real reviews are implemented
        val dummies = List(reviewCount.coerceAtMost(5)) {
            ReviewItem("Dummy", 5f, "", "Mar 19, 2026")
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ReviewsAdapter(dummies)
    }

    private fun showLoading() {
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<View>(R.id.contentLayout).visibility = View.GONE
        findViewById<View>(R.id.errorLayout).visibility = View.GONE
    }

    private fun showContent() {
        findViewById<View>(R.id.progressBar).visibility = View.GONE
        findViewById<View>(R.id.contentLayout).visibility = View.VISIBLE
        findViewById<View>(R.id.errorLayout).visibility = View.GONE
    }

    private fun showError(message: String) {
        findViewById<View>(R.id.progressBar).visibility = View.GONE
        findViewById<View>(R.id.contentLayout).visibility = View.GONE
        findViewById<View>(R.id.errorLayout).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvError).text = message
    }

    inner class ReviewsAdapter(private val items: List<ReviewItem>) :
        RecyclerView.Adapter<ReviewsAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvReviewerName)
            val tvDate: TextView = view.findViewById(R.id.tvReviewDate)
            val ratingBar: RatingBar = view.findViewById(R.id.ratingBarReview)
            val tvComment: TextView = view.findViewById(R.id.tvReviewComment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvName.text = item.reviewerName
            holder.tvDate.text = item.date
            holder.ratingBar.rating = item.rating
            if (item.comment.isNotEmpty()) {
                holder.tvComment.text = item.comment
                holder.tvComment.visibility = View.VISIBLE
            } else {
                holder.tvComment.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size
    }
}
