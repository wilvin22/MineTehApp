package com.example.mineteh.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Categories
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.EditListingViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EditListingActivity : AppCompatActivity() {

    private val viewModel: EditListingViewModel by viewModels()
    private lateinit var photoAdapter: EditPhotoAdapter

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var radioListingType: RadioGroup
    private lateinit var etEndTime: TextInputEditText
    private lateinit var layoutEndTime: View
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnAddPhoto: View
    private lateinit var progressBar: ProgressBar

    private var listingId: Int = -1
    private var currentListing: Listing? = null
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
        setupCategoryDropdown()
        setupPhotoRecycler()
        setupListeners()
        setupObservers()

        viewModel.loadListing(listingId)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etLocation = findViewById(R.id.etLocation)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        radioListingType = findViewById(R.id.radioListingType)
        etEndTime = findViewById(R.id.etEndTime)
        layoutEndTime = findViewById(R.id.layoutEndTime)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Categories.ALL_CATEGORIES)
        spinnerCategory.setAdapter(adapter)
    }

    private fun setupPhotoRecycler() {
        photoAdapter = EditPhotoAdapter { position -> photoAdapter.removeAt(position) }
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPhotos)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = photoAdapter
    }

    private fun setupListeners() {
        btnAddPhoto.setOnClickListener { pickImages.launch("image/*") }
        etEndTime.setOnClickListener { showDatePicker() }
        btnSaveChanges.setOnClickListener { submitEdit() }

        radioListingType.setOnCheckedChangeListener { _, checkedId ->
            layoutEndTime.visibility = if (checkedId == R.id.rbBid) View.VISIBLE else View.GONE
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
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Listing updated successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    viewModel.resetEditResult()
                    btnSaveChanges.postDelayed({ finish() }, 1000)
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
        etLocation.setText(listing.location)
        spinnerCategory.setText(listing.category, false)

        // Set listing type radio
        if (listing.listingType == "BID") {
            radioListingType.check(R.id.rbBid)
            layoutEndTime.visibility = View.VISIBLE
            listing.endTime?.let { endTime ->
                try {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = isoFormat.parse(endTime)
                    if (date != null) {
                        etEndTime.setText(SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(date))
                    } else etEndTime.setText(endTime)
                } catch (e: Exception) {
                    try {
                        val date2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(endTime)
                        if (date2 != null) {
                            etEndTime.setText(SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(date2))
                        } else etEndTime.setText(endTime)
                    } catch (e2: Exception) {
                        etEndTime.setText(endTime)
                    }
                }
            }
        } else {
            radioListingType.check(R.id.rbFixed)
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
        val location = etLocation.text.toString().trim()
        val category = spinnerCategory.text.toString().trim()
        val listingType = if (radioListingType.checkedRadioButtonId == R.id.rbBid) "BID" else "FIXED"

        if (title.isEmpty()) { etTitle.error = "Title is required"; return }
        if (location.isEmpty()) { etLocation.error = "Location is required"; return }
        if (category.isEmpty()) { spinnerCategory.error = "Category is required"; return }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) { etPrice.error = "Enter a valid price greater than 0"; return }

        var endTime: String? = currentListing?.endTime
        if (listingType == "BID") {
            val endTimeStr = etEndTime.text.toString().trim()
            if (endTimeStr.isEmpty()) { etEndTime.error = "End time is required for auctions"; return }
            val parsedEndTime = parseEndTime(endTimeStr)
            if (parsedEndTime == null) { etEndTime.error = "Invalid date format"; return }
            if (parsedEndTime.time <= System.currentTimeMillis()) { etEndTime.error = "End time must be a future date"; return }
            endTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(parsedEndTime)
        } else {
            endTime = null
        }

        val currentExistingUrls = photoAdapter.getExistingUrls()
        val removedPaths = originalImageUrls.filter { it !in currentExistingUrls }
        val newUris = photoAdapter.getNewUris()

        viewModel.editListing(
            id = listingId,
            title = title,
            description = description,
            price = price,
            location = location,
            category = category,
            listingType = listingType,
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
                val date = fmt.parse(dateStr) ?: continue
                val cal = Calendar.getInstance()
                cal.time = date
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                return cal.time
            } catch (e: Exception) { /* try next */ }
        }
        return null
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(this, { _, year, month, day ->
            etEndTime.setText("${month + 1}/$day/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        dialog.datePicker.minDate = calendar.timeInMillis
        dialog.show()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSaveChanges.isEnabled = !loading
    }
}
