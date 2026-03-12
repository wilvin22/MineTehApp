package com.example.mineteh.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.model.BidsUiState
import com.example.mineteh.model.UserBidWithListing
import com.example.mineteh.model.repository.BidsRepository
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BidsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BidsRepository(application)
    
    private val _bidsState = MutableLiveData<BidsUiState>()
    val bidsState: LiveData<BidsUiState> = _bidsState
    
    private var autoRefreshJob: Job? = null
    
    companion object {
        private const val TAG = "BidsViewModel"
        private const val AUTO_REFRESH_INTERVAL = 30000L // 30 seconds
    }
    
    /**
     * Fetches and categorizes user bids
     */
    fun fetchBids() {
        Log.d(TAG, "fetchBids() called")
        _bidsState.value = BidsUiState.Loading
        
        viewModelScope.launch {
            try {
                val result = repository.getUserBids()
                
                when (result) {
                    is Resource.Success -> {
                        val bids = result.data ?: emptyList()
                        Log.d(TAG, "Fetched ${bids.size} bids")
                        
                        val (live, won, lost) = categorizeBids(bids)
                        Log.d(TAG, "Categorized: ${live.size} live, ${won.size} won, ${lost.size} lost")
                        
                        _bidsState.value = BidsUiState.Success(
                            liveBids = live,
                            wonBids = won,
                            lostBids = lost
                        )
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error fetching bids: ${result.message}")
                        _bidsState.value = BidsUiState.Error(result.message ?: "Failed to fetch bids")
                    }
                    is Resource.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchBids", e)
                _bidsState.value = BidsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Categorizes bids into Live/Won/Lost based on auction status
     */
    private fun categorizeBids(bids: List<UserBidWithListing>): Triple<List<UserBidWithListing>, List<UserBidWithListing>, List<UserBidWithListing>> {
        val liveBids = mutableListOf<UserBidWithListing>()
        val wonBids = mutableListOf<UserBidWithListing>()
        val lostBids = mutableListOf<UserBidWithListing>()
        
        for (bid in bids) {
            val isLive = isAuctionLive(bid.listing.endTime, bid.listing.status)
            
            if (isLive) {
                liveBids.add(bid)
            } else {
                // Auction ended
                if (didUserWin(bid.bid.bidAmount, bid.highestBid) && bid.listing.status.equals("sold", ignoreCase = true)) {
                    wonBids.add(bid)
                } else {
                    lostBids.add(bid)
                }
            }
        }
        
        return Triple(liveBids, wonBids, lostBids)
    }
    
    /**
     * Determines if an auction is still live
     */
    private fun isAuctionLive(endTime: String?, status: String): Boolean {
        if (endTime == null) return false
        
        val timeRemaining = TimeUtils.calculateTimeRemaining(endTime)
        val isActive = status.equals("active", ignoreCase = true)
        
        return timeRemaining > 0 && isActive
    }
    
    /**
     * Determines if user won the auction
     */
    private fun didUserWin(userBid: Double, highestBid: Double): Boolean {
        return userBid >= highestBid
    }
    
    /**
     * Starts auto-refresh for live auctions (every 30 seconds)
     */
    fun startAutoRefresh() {
        Log.d(TAG, "Starting auto-refresh")
        stopAutoRefresh() // Cancel any existing job
        
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL)
                Log.d(TAG, "Auto-refresh triggered")
                fetchBids()
            }
        }
    }
    
    /**
     * Stops auto-refresh when activity is paused
     */
    fun stopAutoRefresh() {
        Log.d(TAG, "Stopping auto-refresh")
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
