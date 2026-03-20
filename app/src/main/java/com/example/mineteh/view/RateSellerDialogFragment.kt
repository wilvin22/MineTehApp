package com.example.mineteh.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.mineteh.R
import com.example.mineteh.databinding.DialogRateSellerBinding
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.ReviewViewModel

class RateSellerDialogFragment : DialogFragment() {

    interface RateSellerListener {
        fun onRatingSubmitted()
    }

    private var _binding: DialogRateSellerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ReviewViewModel
    private var listener: RateSellerListener? = null

    private var sellerId: Int = -1
    private var listingId: Int = -1

    companion object {
        private const val ARG_SELLER_ID = "seller_id"
        private const val ARG_LISTING_ID = "listing_id"

        fun newInstance(
            sellerId: Int,
            listingId: Int,
            listener: RateSellerListener
        ): RateSellerDialogFragment {
            return RateSellerDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SELLER_ID, sellerId)
                    putInt(ARG_LISTING_ID, listingId)
                }
                this.listener = listener
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sellerId = arguments?.getInt(ARG_SELLER_ID, -1) ?: -1
        listingId = arguments?.getInt(ARG_LISTING_ID, -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRateSellerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ReviewViewModel::class.java]

        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            if (rating == 0) {
                Toast.makeText(requireContext(), "Please select a star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val comment = binding.etComment.text?.toString()
            viewModel.submitReview(sellerId, listingId, rating, comment)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        viewModel.submitResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSubmitRating.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitRating.isEnabled = true
                    listener?.onRatingSubmitted()
                    dismiss()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitRating.isEnabled = true
                    Toast.makeText(requireContext(), result.message ?: "Failed to submit rating", Toast.LENGTH_SHORT).show()
                }
                null -> { /* initial state, do nothing */ }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
