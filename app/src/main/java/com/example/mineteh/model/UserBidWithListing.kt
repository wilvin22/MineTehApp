package com.example.mineteh.model

data class UserBidWithListing(
    val bid: UserBidData,
    val listing: ItemModel.Listing,
    val highestBid: Double
)
