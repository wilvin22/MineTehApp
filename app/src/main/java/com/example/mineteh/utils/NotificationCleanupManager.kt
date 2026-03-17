package com.example.mineteh.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Manager for scheduling automatic notification cleanup
 * Handles periodic cleanup of old notifications to maintain performance
 */
class NotificationCleanupManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationCleanupManager"
        private const val CLEANUP_WORK_NAME = "notification_cleanup_periodic"
        
        // Cleanup frequency - run every 24 hours
        private const val CLEANUP_INTERVAL_HOURS = 24L
        
        // Cleanup during low usage hours (3 AM)
        private const val CLEANUP_HOUR = 3
    }

    /**
     * Schedule periodic notification cleanup
     * Runs daily during low-usage hours
     */
    fun schedulePeriodicCleanup() {
        try {
            Log.d(TAG, "Scheduling periodic notification cleanup")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<NotificationCleanupWorker>(
                CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("notification_cleanup")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CLEANUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            
            Log.d(TAG, "Periodic notification cleanup scheduled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic cleanup", e)
        }
    }

    /**
     * Cancel periodic notification cleanup
     */
    fun cancelPeriodicCleanup() {
        try {
            Log.d(TAG, "Cancelling periodic notification cleanup")
            
            WorkManager.getInstance(context).cancelUniqueWork(CLEANUP_WORK_NAME)
            
            Log.d(TAG, "Periodic notification cleanup cancelled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling periodic cleanup", e)
        }
    }

    /**
     * Run immediate cleanup (for testing or manual trigger)
     */
    fun runImmediateCleanup() {
        try {
            Log.d(TAG, "Running immediate notification cleanup")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateCleanupRequest = OneTimeWorkRequestBuilder<NotificationCleanupWorker>()
                .setConstraints(constraints)
                .addTag("notification_cleanup_immediate")
                .build()

            WorkManager.getInstance(context).enqueue(immediateCleanupRequest)
            
            Log.d(TAG, "Immediate notification cleanup enqueued")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running immediate cleanup", e)
        }
    }

    /**
     * Check if periodic cleanup is scheduled
     */
    fun isCleanupScheduled(): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(CLEANUP_WORK_NAME)
                .get()
            
            workInfos.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cleanup schedule status", e)
            false
        }
    }

    /**
     * Get cleanup work status
     */
    fun getCleanupStatus(): WorkInfo.State? {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(CLEANUP_WORK_NAME)
                .get()
            
            workInfos.firstOrNull()?.state
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cleanup status", e)
            null
        }
    }
}