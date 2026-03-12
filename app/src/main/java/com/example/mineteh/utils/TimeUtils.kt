package com.example.mineteh.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun formatCountdown(millisRemaining: Long): String {
        if (millisRemaining <= 0) return "Ended"
        
        val seconds = (millisRemaining / 1000) % 60
        val minutes = (millisRemaining / (1000 * 60)) % 60
        val hours = (millisRemaining / (1000 * 60 * 60)) % 24
        val days = millisRemaining / (1000 * 60 * 60 * 24)
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    fun formatEndTime(endTime: String?): String {
        if (endTime == null) return ""
        return try {
            val date = parseIsoDate(endTime)
            val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            endTime
        }
    }
    
    fun parseIsoDate(isoString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(isoString) ?: Date()
    }
    
    fun calculateTimeRemaining(endTime: String): Long {
        return try {
            val endDate = parseIsoDate(endTime)
            endDate.time - System.currentTimeMillis()
        } catch (e: Exception) {
            0L
        }
    }
}
