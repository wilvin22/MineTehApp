package com.example.mineteh.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.EditListingViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EditListingActivity : AppCompatActivity() {

    private val viewModel: EditListingViewModel by viewModels()
    private lateinit var photoAdapter: EditPhotoAdapter

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var layoutEndTime: View
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnAddPhoto: View
    private lateinit var progressBar: ProgressBar

    private var listingId: Int = -1
    private var currentListing: Listing? = null

    // Tracks which existing URLs were present when the listing loaded
    private val originalImageUrls = mutableListOf<String>()

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.forEach { photoAdapter.addNewUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_listing)

        listingId = intent.getIntExtra("listing_id", -1)
        if (listingId == -1) {
            Toast.makeText(this, "Invalid listing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        initViews()
        setupPhotoRecycler()
        setupListeners()
        setupObservers()

        viewModel.loadListing(listingId)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etEndTime = findViewById(R.id.etEndTime)
        layoutEndTime = findViewById(R.id.layoutEndTime)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupPhotoRecycler() {
        photoAdapter = EditPhotoAdapter { position ->
            photoAdapter.removeAt(position)
        }
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPhotos)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = photoAdapter
    }

    private fun setupListeners() {
        btnAddPhoto.setOnClickListener {
            pickImages.launch("image/*")
        }

        etEndTime.setOnClickListener {
            showDatePicker()
        }

        btnSaveChanges.setOnClickListener {
            submitEdit()
        }
    }

    private fun setupObservers() {
        viewModel.listing.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    resource.data?.let { populateFields(it) }
                }
                is Resource.Error -> {
                    setLoading(false)
                    Toast.makeText(this, resource.message ?: "Failed to load listing", Toast.LENGTH_LONG).show()
                    finish()
                }
                null -> {}
            }
        }

        viewModel.editResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnSaveChanges.isEnabled = false
                    progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    btnSaveChanges.isEnabled = true
                    Toast.makeText(this, "Listing updated successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetEditResult()
                    finish()
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    btnSaveChanges.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Failed to update listing", Toast.LENGTH_LONG).show()
                    viewModel.resetEditResult()
                }
                null -> {
                    progressBar.visibility = View.GONE
                    btnSaveChanges.isEnabled = true
                }
            }
        }
    }

    private fun populateFields(listing: Listing) {
        currentListing = listing

        etTitle.setText(listing.title)
        etDescription.setText(listing.description)
        etPrice.setText(listing.price.toString())

        // Show/hide end time based on listing type
        if (listing.listingType == "BID") {
            layoutEndTime.visibility = View.VISIBLE
            listing.endTime?.let { endTime ->
                // Convert ISO format to display format
                try {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = isoFormat.parse(endTime)
                    if (date != null) {
                        val displayFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
                        etEndTime.setText(displayFormat.format(date))
                    } else {
                        etEndTime.setText(endTime)
                    }
                } catch (e: Exception) {
                    // Try alternate format
                    try {
                        val altFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date2 = altFormat.parse(endTime)
                        if (date2 != null) {
                            val displayFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
                            etEndTime.setText(displayFormat.format(date2))
                        } else {
                            etEndTime.setText(endTime)
                        }
                    } catch (e2: Exception) {
                        etEndTime.setText(endTime)
                    }
                }
            }
        } else {
            layoutEndTime.visibility = View.GONE
        }

        // Load existing photos
        val existingUrls = listing.images.ifEmpty {
            listing.image?.let { listOf(it) } ?: emptyList()
        }
        originalImageUrls.clear()
        originalImageUrls.addAll(existingUrls)
        photoAdapter.setExistingPhotos(existingUrls)
    }

    private fun submitEdit() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val priceStr = etPrice.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Title is required"
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            etPrice.error = "Enter a valid price greater than 0"
            return
        }

        var endTime: String? = currentListing?.endTime

        if (currentListing?.listingType == "BID") {
            val endTimeStr = etEndTime.text.toString().trim()
            if (endTimeStr.isEmpty()) {
                etEndTime.error = "End time is required for auctions"
                return
            }
            // Parse and validate end time is in the future
            val parsedEndTime = parseEndTime(endTimeStr)
            if (parsedEndTime == null) {
                etEndTime.error = "Invalid date format"
                return
            }
            if (parsedEndTime.time <= System.currentTimeMillis()) {
                etEndTime.error = "End time must be a future date"
                return
            }
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            endTime = outputFormat.format(parsedEndTime)
        }

        // Compute removed image paths: original URLs that are no longer in the adapter
        val currentExistingUrls = photoAdapter.getExistingUrls()
        val removedPaths = originalImageUrls.filter { it !in currentExistingUrls }
        val newUris = photoAdapter.getNewUris()

        viewModel.editListing(
            id = listingId,
            title = title,
            description = description,
            price = price,
            endTime = endTime,
            newImageUris = newUris,
            removedImagePaths = removedPaths
        )
    }

    private fun parseEndTime(dateStr: String): Date? {
        val formats = listOf(
            SimpleDateFormat("M/d/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        )
        for (fmt in formats) {
            try {
                val date = fmt.parse(dateStr)
                if (date != null) {
                    // Set to end of day
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    return cal.time
                }
            } catch (e: Exception) {
                // try next format
            }
        }
        return null
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formatted = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            etEndTime.setText(formatted)
        }, year, month, day)

        // Minimum date is tomorrow
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        dialog.datePicker.minDate = calendar.timeInMillis
        dialog.show()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSaveChanges.isEnabled = !loading
    }
}
