package com.example.mineteh.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun getRelativeTimeString(date: Date): String {
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < 0) return "Just now"
        
        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 10 -> "Just now"
            seconds < 60 -> "${seconds}s ago"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                format.format(date)
            }
        }
    }

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
