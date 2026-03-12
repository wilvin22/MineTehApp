package com.example.mineteh.utils

object CurrencyUtils {
    fun formatCurrency(amount: Double): String {
        return String.format("₱%.2f", amount)
    }
}
