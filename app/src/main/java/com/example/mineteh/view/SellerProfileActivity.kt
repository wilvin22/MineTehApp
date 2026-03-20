package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.repository.ListingsRepository
import com.example.mineteh.models.SellerProfileData
import com.example.mineteh.utils.AvatarUtils
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.SellerProfileViewModel
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SellerProfileActivity : AppCompatActivity() {

    private val viewModel: SellerProfileViewModel by viewModels()

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: View
    private lateinit var errorLayout: View
    private lateinit var tvError: TextView
    private lateinit var ivAvatar: ShapeableImageView
    private lateinit var tvUsername: TextView
    private lateinit var ratingBar: android.widget.RatingBar
    private lateinit var tvRating: TextView
    private lateinit var tvActiveListings: TextView
    private lateinit var tvSoldCount: TextView
    private lateinit var rvListings: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_profile)

        bindViews()
        setupToolbar()

        val sellerId = intent.getIntExtra("seller_id", -1)
        if (sellerId == -1) {
            Toast.makeText(this, "Invalid seller", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupObservers(sellerId)
        viewModel.loadProfile(sellerId)
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)
        errorLayout = findViewById(R.id.errorLayout)
        tvError = findViewById(R.id.tvError)
        ivAvatar = findViewById(R.id.ivAvatar)
        tvUsername = findViewById(R.id.tvUsername)
        ratingBar = findViewById(R.id.ratingBar)
        tvRating = findViewById(R.id.tvRating)
        tvActiveListings = findViewById(R.id.tvActiveListings)
        tvSoldCount = findViewById(R.id.tvSoldCount)
        rvListings = findViewById(R.id.rvListings)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Seller Profile"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers(sellerId: Int) {
        viewModel.profile.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading()
                is Resource.Success -> {
                    resource.data?.let { bindProfile(it, sellerId) } ?: showError("No data")
                }
                is Resource.Error -> showError(resource.message ?: "Failed to load profile")
                null -> {}
            }
        }
    }

    private fun bindProfile(data: SellerProfileData, sellerId: Int) {
        showContent()

        val fullName = "${data.firstName} ${data.lastName}".trim()
        tvUsername.text = if (fullName.isNotEmpty()) fullName else data.username
        tvActiveListings.text = data.activeListingCount.toString()
        tvSoldCount.text = data.soldCount.toString()

        val rating = data.averageRating.toFloat()
        ratingBar.rating = rating
        tvRating.text = if (data.averageRating > 0)
            String.format("%.1f / 5.0", data.averageRating)
        else
            "No ratings yet"

        // Avatar: load photo or show initials
        AvatarUtils.bindAvatar(
            view = ivAvatar,
            firstName = data.firstName,
            lastName = data.lastName,
            accountId = data.accountId,
            avatarUrl = data.avatarUrl,
            context = this
        )

        // Load seller's active listings
        loadSellerListings(sellerId)
    }

    private fun loadSellerListings(sellerId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ListingsRepository(this@SellerProfileActivity).getListings(
                        type = null,
                        category = null,
                        search = null
                    )
                }
                if (result is Resource.Success) {
                    val sellerListings = result.data?.filter {
                        it.seller?.accountId == sellerId && it.status.equals("active", ignoreCase = true)
                    } ?: emptyList()

                    val adapter = ItemAdapter(sellerListings)
                    rvListings.layoutManager = GridLayoutManager(this@SellerProfileActivity, 2)
                    rvListings.adapter = adapter
                }
            } catch (e: Exception) {
                android.util.Log.e("SellerProfileActivity", "Error loading listings", e)
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
        errorLayout.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
        tvError.text = message
    }
}
