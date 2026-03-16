package com.example.mineteh.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.TextView
import com.example.mineteh.R
import com.example.mineteh.utils.Categories
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    interface FilterListener {
        fun onFiltersApplied(filters: FilterOptions)
    }

    data class FilterOptions(
        val category: String? = null,
        val listingType: String? = null,
        val minPrice: Double? = null,
        val maxPrice: Double? = null,
        val location: String? = null
    )

    private var filterListener: FilterListener? = null
    private var currentFilters = FilterOptions()

    // UI Components
    private lateinit var btnClearFilters: TextView
    private lateinit var categoryFilter: AutoCompleteTextView
    private lateinit var listingTypeFilter: RadioGroup
    private lateinit var minPriceInput: TextInputEditText
    private lateinit var maxPriceInput: TextInputEditText
    private lateinit var locationFilter: TextInputEditText
    private lateinit var btnApplyFilters: MaterialButton

    companion object {
        fun newInstance(
            currentFilters: FilterOptions = FilterOptions(),
            listener: FilterListener
        ): FilterBottomSheetFragment {
            return FilterBottomSheetFragment().apply {
                this.currentFilters = currentFilters
                this.filterListener = listener
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupCategorySpinner()
        setupListeners()
        populateCurrentFilters()
    }

    private fun initViews(view: View) {
        btnClearFilters = view.findViewById(R.id.btnClearFilters)
        categoryFilter = view.findViewById(R.id.categoryFilter)
        listingTypeFilter = view.findViewById(R.id.listingTypeFilter)
        minPriceInput = view.findViewById(R.id.minPriceInput)
        maxPriceInput = view.findViewById(R.id.maxPriceInput)
        locationFilter = view.findViewById(R.id.locationFilter)
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters)
    }

    private fun setupCategorySpinner() {
        categoryFilter.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, Categories.CATEGORIES_WITH_ALL)
        )
    }

    private fun setupListeners() {
        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        btnApplyFilters.setOnClickListener {
            applyFilters()
        }
    }

    private fun populateCurrentFilters() {
        // Set current filter values
        categoryFilter.setText(Categories.getCategoryDisplayName(currentFilters.category), false)
        
        when (currentFilters.listingType) {
            "FIXED" -> listingTypeFilter.check(R.id.rbFixed)
            "BID" -> listingTypeFilter.check(R.id.rbAuction)
            else -> listingTypeFilter.check(R.id.rbAll)
        }

        currentFilters.minPrice?.let {
            minPriceInput.setText(it.toString())
        }

        currentFilters.maxPrice?.let {
            maxPriceInput.setText(it.toString())
        }

        locationFilter.setText(currentFilters.location ?: "")
    }

    private fun clearAllFilters() {
        categoryFilter.setText("All", false)
        listingTypeFilter.check(R.id.rbAll)
        minPriceInput.setText("")
        maxPriceInput.setText("")
        locationFilter.setText("")
    }

    private fun applyFilters() {
        val category = Categories.getCategoryForApi(categoryFilter.text.toString())

        val listingType = when (listingTypeFilter.checkedRadioButtonId) {
            R.id.rbFixed -> "FIXED"
            R.id.rbAuction -> "BID"
            else -> null
        }

        val minPrice = minPriceInput.text.toString().toDoubleOrNull()
        val maxPrice = maxPriceInput.text.toString().toDoubleOrNull()
        val location = locationFilter.text.toString().takeIf { it.isNotEmpty() }

        val filters = FilterOptions(
            category = category,
            listingType = listingType,
            minPrice = minPrice,
            maxPrice = maxPrice,
            location = location
        )

        filterListener?.onFiltersApplied(filters)
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()
        filterListener = null
    }
}