package com.example.mineteh.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.model.repository.NotificationsRepository

/**
 * Background worker that performs automatic notification cleanup
 * Runs periodically to clean up old notifications and maintain performance
 */
class NotificationCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationCleanupWorker"
        const val WORK_NAME = "notification_cleanup_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting notification cleanup work")
            
            val tokenManager = TokenManager(applicationContext)
            val userId = tokenManager.getUserId()
            
            if (userId == -1) {
                Log.w(TAG, "No user ID found, skipping cleanup")
                return Result.success()
            }
            
            val repository = NotificationsRepository(applicationContext)
            
            // Perform automatic cleanup
            val cleanupResult = repository.performAutomaticCleanup(userId)
            
            when (cleanupResult) {
                is Resource.Success -> {
                    val (ageDeleted, countDeleted) = cleanupResult.data!!
                    Log.d(TAG, "Cleanup completed successfully: $ageDeleted by age, $countDeleted by count")
                    Result.success()
                }
                is Resource.Error -> {
                    Log.e(TAG, "Cleanup failed: ${cleanupResult.message}")
                    Result.retry()
                }
                else -> {
                    Log.w(TAG, "Cleanup returned unexpected result")
                    Result.failure()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during notification cleanup", e)
            Result.retry()
        }
    }
}