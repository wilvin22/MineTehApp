package com.example.mineteh.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.utils.Categories
import com.example.mineteh.view.PhotoPreviewAdapter
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.CreateListingViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class SellActivity : AppCompatActivity() {

    private val viewModel: CreateListingViewModel by viewModels()
    private lateinit var photoAdapter: PhotoPreviewAdapter

    private val MAX_PHOTOS = 5

    // Photo picker launcher
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            val currentPhotoCount = photoAdapter.getPhotos().size
            val availableSlots = MAX_PHOTOS - currentPhotoCount

            if (availableSlots <= 0) {
                Toast.makeText(this, "Maximum $MAX_PHOTOS photos allowed", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val photosToAdd = uris.take(availableSlots)
            photosToAdd.forEach { uri ->
                photoAdapter.addPhoto(uri)
            }

            if (uris.size > availableSlots) {
                Toast.makeText(
                    this,
                    "Only $availableSlots more photos can be added",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Camera launcher removed - using gallery only

    // UI Components
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var radioListingType: RadioGroup
    private lateinit var rbFixed: RadioButton
    private lateinit var rbBid: RadioButton
    private lateinit var layoutAuctionDuration: LinearLayout
    private lateinit var tvPriceLabel: TextView
    private lateinit var radioDuration: RadioGroup
    private lateinit var rb1Day: RadioButton
    private lateinit var rb3Days: RadioButton
    private lateinit var rb7Days: RadioButton
    private lateinit var rbCustom: RadioButton
    private lateinit var layoutCustomDate: TextInputLayout
    private lateinit var etCustomDate: EditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var btnSubmit: Button
    private lateinit var btnAddPhoto: Button
    private lateinit var recyclerPhotos: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.sell)

            initViews()
            setupPhotoRecyclerView()
            setupNavigation()
            setupCategoryDropdown()
            setupListeners()
            observeCreateListingState()
        } catch (e: Exception) {
            Log.e("SellActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error loading page: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etLocation = findViewById(R.id.etLocation)
        radioListingType = findViewById(R.id.radioListingType)
        rbFixed = findViewById(R.id.rbFixed)
        rbBid = findViewById(R.id.rbBid)
        layoutAuctionDuration = findViewById(R.id.layoutAuctionDuration)
        tvPriceLabel = findViewById(R.id.tvPriceLabel)
        radioDuration = findViewById(R.id.radioDuration)
        rb1Day = findViewById(R.id.rb1Day)
        rb3Days = findViewById(R.id.rb3Days)
        rb7Days = findViewById(R.id.rb7Days)
        rbCustom = findViewById(R.id.rbCustom)
        layoutCustomDate = findViewById(R.id.layoutCustomDate)
        etCustomDate = findViewById(R.id.etCustomDate)
        categoryDropdown = findViewById(R.id.spinnerCategory)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        recyclerPhotos = findViewById(R.id.recyclerPhotos)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupPhotoRecyclerView() {
        photoAdapter = PhotoPreviewAdapter { position ->
            photoAdapter.removePhoto(position)
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }

        recyclerPhotos.apply {
            layoutManager = GridLayoutManager(this@SellActivity, 3)
            adapter = photoAdapter
        }
    }

    private fun setupCategoryDropdown() {
        categoryDropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Categories.ALL_CATEGORIES)
        )
    }

    private fun setupListeners() {
        // Listing Type logic
        radioListingType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbBid) {
                layoutAuctionDuration.visibility = View.VISIBLE
                tvPriceLabel.text = "Starting Price *"
            } else {
                layoutAuctionDuration.visibility = View.GONE
                tvPriceLabel.text = "Price *"
            }
        }

        // Auction Duration logic
        radioDuration.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbCustom) {
                layoutCustomDate.visibility = View.VISIBLE
            } else {
                layoutCustomDate.visibility = View.GONE
            }
        }

        // Custom Date Picker
        etCustomDate.setOnClickListener {
            showDatePicker(etCustomDate)
        }

        // Add Photo Button
        btnAddPhoto.setOnClickListener {
            val currentPhotoCount = photoAdapter.getPhotos().size
            if (currentPhotoCount >= MAX_PHOTOS) {
                Toast.makeText(this, "Maximum $MAX_PHOTOS photos allowed", Toast.LENGTH_SHORT).show()
            } else {
                pickImages.launch("image/*")
            }
        }

        // Submit Button
        btnSubmit.setOnClickListener {
            submitListing()
        }
    }

    private fun submitListing() {
        // Validate photos
        val photoUris = photoAdapter.getPhotos()
        if (photoUris.isEmpty()) {
            Toast.makeText(this, "Please add at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val priceStr = etPrice.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val category = categoryDropdown.text.toString().trim()

        // Validation
        if (title.isEmpty() || description.isEmpty() || priceStr.isEmpty() ||
            location.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }

        val listingType = if (rbBid.isChecked) "BID" else "FIXED"

        var endTime: String? = null
        var minBidIncrement: Double? = null

        if (listingType == "BID") {
            endTime = calculateEndTime()
            if (endTime == null) {
                Toast.makeText(this, "Please select auction duration", Toast.LENGTH_SHORT).show()
                return
            }
            minBidIncrement = 1.0
        }

        // Create the listing
        viewModel.createListing(
            title = title,
            description = description,
            price = price,
            location = location,
            category = category,
            listingType = listingType,
            endTime = endTime,
            minBidIncrement = minBidIncrement,
            imageUris = photoUris
        )
    }

    private fun calculateEndTime(): String? {
        val selectedDurationId = radioDuration.checkedRadioButtonId
        if (selectedDurationId == -1) return null

        val calendar = Calendar.getInstance()

        when (selectedDurationId) {
            R.id.rb1Day -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            R.id.rb3Days -> calendar.add(Calendar.DAY_OF_MONTH, 3)
            R.id.rb7Days -> calendar.add(Calendar.DAY_OF_MONTH, 7)
            R.id.rbCustom -> {
                val customDate = etCustomDate.text.toString()
                if (customDate.isBlank()) return null

                try {
                    val inputFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(customDate)
                    if (date != null) {
                        calendar.time = date
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                    }
                } catch (e: Exception) {
                    Log.e("SellActivity", "Error parsing custom date", e)
                    return null
                }
            }
        }

        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return outputFormat.format(calendar.time)
    }

    private fun observeCreateListingState() {
        viewModel.createStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    setLoading(true)
                }
                is Resource.Success -> {
                    setLoading(false)
                    Toast.makeText(this, "Listing created successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("SellActivity", "Listing created: ${resource.data}")

                    // Navigate to home
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    setLoading(false)
                    Toast.makeText(this, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                    Log.e("SellActivity", "Create listing error: ${resource.message}")
                }
                null -> {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSubmit.isEnabled = !loading
        btnAddPhoto.isEnabled = !loading
        etTitle.isEnabled = !loading
        etDescription.isEnabled = !loading
        etPrice.isEnabled = !loading
        etLocation.isEnabled = !loading
        categoryDropdown.isEnabled = !loading
        radioListingType.isEnabled = !loading

        if (::progressBar.isInitialized) {
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        btnSubmit.text = if (loading) "Creating..." else "Post Listing"
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            editText.setText(formattedDate)
        }, year, month, day)

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_notifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_inbox)?.setOnClickListener {
            startActivity(Intent(this, InboxActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_profile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
