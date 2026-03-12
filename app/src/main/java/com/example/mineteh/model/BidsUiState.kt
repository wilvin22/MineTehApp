package com.example.mineteh.model

sealed class BidsUiState {
    object Loading : BidsUiState()
    data class Success(
        val liveBids: List<UserBidWithListing>,
        val wonBids: List<UserBidWithListing>,
        val lostBids: List<UserBidWithListing>
    ) : BidsUiState()
    data class Error(val message: String) : BidsUiState()
}
