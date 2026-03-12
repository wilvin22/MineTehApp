package com.example.mineteh.model

import com.example.mineteh.models.Listing

data class UserBidWithListing(
    val bid: UserBidData,
    val listing: Listing,
    val highestBid: Double
)
