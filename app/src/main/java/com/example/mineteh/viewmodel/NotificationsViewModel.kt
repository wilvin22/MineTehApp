package com.example.mineteh.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.model.Notification
import com.example.mineteh.model.NotificationPreferences
import com.example.mineteh.model.repository.NotificationsRepository
import com.example.mineteh.utils.NotificationRealtimeManager
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationsRepository(application)
    private val tokenManager = TokenManager(application)
    private val realtimeManager = NotificationRealtimeManager(application)
    
    companion object {
        private const val TAG = "NotificationsViewModel"
    }

    // LiveData for notifications list
    private val _notifications = MutableLiveData<Resource<List<Notification>>>()
    val notifications: LiveData<Resource<List<Notification>>> = _notifications

    // LiveData for unread count
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    // LiveData for notification preferences
    private val _preferences = MutableLiveData<Resource<NotificationPreferences>>()
    val preferences: LiveData<Resource<NotificationPreferences>> = _preferences

    // LiveData for loading states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // Pagination state
    private var currentPage = 0
    private var hasMorePages = true
    private val pageSize = 20
    private val allNotifications = mutableListOf<Notification>()

    init {
        loadNotifications()
        loadUnreadCount()
        setupRealtimeSubscription()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Loading notifications for user: $userId")
                    val result = repository.getNotifications(userId, limit = 50)
                    _notifications.value = result
                    if (result is Resource.Success) {
                        allNotifications.clear()
                        allNotifications.addAll(result.data ?: emptyList())
                    }
                } else {
                    _notifications.value = Resource.Error("User not authenticated")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
                _notifications.value = Resource.Error("Failed to load notifications: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreNotifications() {
        if (!hasMorePages || _isLoading.value == true) return
        
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Loading more notifications for user: $userId, page: ${currentPage + 1}")
                    
                    val result = repository.getNotificationsPaginated(userId, currentPage + 1, pageSize)
                    
                    if (result is Resource.Success) {
                        val (newNotifications, hasMore) = result.data!!
                        allNotifications.addAll(newNotifications)
                        hasMorePages = hasMore
                        currentPage++
                        
                        _notifications.value = Resource.Success(allNotifications.toList())
                        Log.d(TAG, "Successfully loaded ${newNotifications.size} more notifications")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more notifications", e)
            }
        }
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                currentPage = 0
                hasMorePages = true
                allNotifications.clear()
                
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Refreshing notifications for user: $userId")
                    
                    val result = repository.getNotificationsPaginated(userId, 0, pageSize)
                    
                    if (result is Resource.Success) {
                        val (newNotifications, hasMore) = result.data!!
                        allNotifications.addAll(newNotifications)
                        hasMorePages = hasMore
                        
                        _notifications.value = Resource.Success(allNotifications.toList())
                        Log.d(TAG, "Successfully refreshed ${newNotifications.size} notifications")
                    } else {
                        _notifications.value = Resource.Error(result.message ?: "Failed to refresh notifications")
                    }
                    
                    // Also refresh unread count
                    loadUnreadCount()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing notifications", e)
                _notifications.value = Resource.Error("Failed to refresh notifications: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Marking notification as read: $notificationId")
                
                val result = repository.markAsRead(notificationId)
                
                if (result is Resource.Success) {
                    // Update local list
                    val updatedNotifications = allNotifications.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    allNotifications.clear()
                    allNotifications.addAll(updatedNotifications)
                    _notifications.value = Resource.Success(allNotifications.toList())
                    
                    // Update unread count
                    loadUnreadCount()
                    
                    Log.d(TAG, "Successfully marked notification $notificationId as read")
                } else {
                    Log.e(TAG, "Failed to mark notification as read: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Marking all notifications as read for user: $userId")
                    
                    val result = repository.markAllAsRead(userId)
                    
                    if (result is Resource.Success) {
                        // Update local list
                        val updatedNotifications = allNotifications.map { notification ->
                            notification.copy(isRead = true)
                        }
                        allNotifications.clear()
                        allNotifications.addAll(updatedNotifications)
                        _notifications.value = Resource.Success(allNotifications.toList())
                        
                        // Update unread count
                        _unreadCount.value = 0
                        
                        Log.d(TAG, "Successfully marked all notifications as read")
                    } else {
                        Log.e(TAG, "Failed to mark all notifications as read: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all notifications as read", e)
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Loading unread count for user: $userId")
                    
                    val result = repository.getUnreadCount(userId)
                    
                    if (result is Resource.Success) {
                        _unreadCount.value = result.data ?: 0
                        Log.d(TAG, "Unread count: ${result.data}")
                    } else {
                        Log.e(TAG, "Failed to load unread count: ${result.message}")
                        _unreadCount.value = 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading unread count", e)
                _unreadCount.value = 0
            }
        }
    }

    fun filterNotifications(type: String? = null, isRead: Boolean? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Filtering notifications for user: $userId, type: $type, isRead: $isRead")
                    
                    val result = repository.getFilteredNotifications(userId, type, isRead)
                    _notifications.value = result
                    
                    if (result is Resource.Success) {
                        allNotifications.clear()
                        allNotifications.addAll(result.data ?: emptyList())
                        Log.d(TAG, "Successfully filtered ${result.data?.size ?: 0} notifications")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering notifications", e)
                _notifications.value = Resource.Error("Failed to filter notifications: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchNotifications(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Searching notifications for user: $userId, query: $query")
                    
                    val result = repository.searchNotifications(userId, query)
                    _notifications.value = result
                    
                    if (result is Resource.Success) {
                        allNotifications.clear()
                        allNotifications.addAll(result.data ?: emptyList())
                        Log.d(TAG, "Successfully found ${result.data?.size ?: 0} notifications")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching notifications", e)
                _notifications.value = Resource.Error("Failed to search notifications: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Loading notification preferences for user: $userId")
                    
                    val result = repository.getPreferences(userId)
                    _preferences.value = result
                    
                    if (result is Resource.Success) {
                        Log.d(TAG, "Successfully loaded notification preferences")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notification preferences", e)
                _preferences.value = Resource.Error("Failed to load preferences: ${e.message}")
            }
        }
    }

    fun updatePreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "Updating notification preferences for user: $userId")
                    
                    val result = repository.updatePreferences(userId, preferences)
                    
                    if (result is Resource.Success) {
                        _preferences.value = Resource.Success(preferences)
                        Log.d(TAG, "Successfully updated notification preferences")
                    } else {
                        Log.e(TAG, "Failed to update preferences: ${result.message}")
                        _preferences.value = Resource.Error(result.message ?: "Failed to update preferences")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification preferences", e)
                _preferences.value = Resource.Error("Failed to update preferences: ${e.message}")
            }
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting notification: $notificationId")
                
                val result = repository.deleteNotification(notificationId)
                
                if (result is Resource.Success) {
                    // Remove from local list
                    allNotifications.removeAll { it.id == notificationId }
                    _notifications.value = Resource.Success(allNotifications.toList())
                    
                    // Update unread count
                    loadUnreadCount()
                    
                    Log.d(TAG, "Successfully deleted notification $notificationId")
                } else {
                    Log.e(TAG, "Failed to delete notification: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting notification", e)
            }
        }
    }

    fun getNotificationById(notificationId: Int): Notification? {
        return allNotifications.find { it.id == notificationId }
    }

    fun getUnreadNotifications(): List<Notification> {
        return allNotifications.filter { !it.isRead }
    }

    fun getNotificationsByType(type: String): List<Notification> {
        return allNotifications.filter { it.type.name == type }
    }

    fun hasUnreadNotifications(): Boolean {
        return (_unreadCount.value ?: 0) > 0
    }

    fun clearNotifications() {
        allNotifications.clear()
        _notifications.value = Resource.Success(emptyList())
        _unreadCount.value = 0
        currentPage = 0
        hasMorePages = true
    }

    fun retryLastOperation() {
        when {
            _notifications.value is Resource.Error -> loadNotifications()
            _preferences.value is Resource.Error -> loadPreferences()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "NotificationsViewModel cleared")
        realtimeManager.cleanup()
    }

    /**
     * Setup real-time subscription for notifications
     */
    private fun setupRealtimeSubscription() {
        val userId = tokenManager.getUserId()
        if (userId != -1) {
            Log.d(TAG, "Setting up real-time subscription for user: $userId")
            
            // Start real-time subscription
            realtimeManager.startSubscription(userId)
            
            // Listen for new notifications
            viewModelScope.launch {
                realtimeManager.newNotifications.collect { notification ->
                    Log.d(TAG, "Received new notification via real-time: ${notification.title}")
                    handleNewNotification(notification)
                }
            }
            
            // Listen for notification updates
            viewModelScope.launch {
                realtimeManager.notificationUpdates.collect { notification ->
                    Log.d(TAG, "Received notification update via real-time: ${notification.id}")
                    handleNotificationUpdate(notification)
                }
            }
            
            // Listen for unread count updates
            viewModelScope.launch {
                realtimeManager.unreadCountUpdates.collect { count ->
                    Log.d(TAG, "Received unread count update via real-time: $count")
                    _unreadCount.value = count
                }
            }
        } else {
            Log.w(TAG, "No user ID found, cannot setup real-time subscription")
        }
    }

    /**
     * Handle new notification received via real-time
     */
    private fun handleNewNotification(notification: Notification) {
        // Add to the beginning of the list (most recent first)
        allNotifications.add(0, notification)
        _notifications.value = Resource.Success(allNotifications.toList())
        
        Log.d(TAG, "Added new notification to list: ${notification.title}")
    }

    /**
     * Handle notification update received via real-time
     */
    private fun handleNotificationUpdate(updatedNotification: Notification) {
        // Find and update the notification in the list
        val index = allNotifications.indexOfFirst { it.id == updatedNotification.id }
        if (index != -1) {
            allNotifications[index] = updatedNotification
            _notifications.value = Resource.Success(allNotifications.toList())
            
            Log.d(TAG, "Updated notification in list: ${updatedNotification.id}")
        } else {
            Log.w(TAG, "Notification not found in list for update: ${updatedNotification.id}")
        }
    }

    /**
     * Start real-time subscription (can be called from Activity)
     */
    fun startRealtimeUpdates() {
        val userId = tokenManager.getUserId()
        if (userId != -1) {
            realtimeManager.startSubscription(userId)
        }
    }

    /**
     * Stop real-time subscription (can be called from Activity)
     */
    fun stopRealtimeUpdates() {
        realtimeManager.stopSubscription()
    }

    /**
     * Check if real-time updates are connected
     */
    fun isRealtimeConnected(): Boolean {
        return realtimeManager.isConnected()
    }

    /**
     * Handle connection errors and attempt reconnection
     */
    fun handleRealtimeConnectionError() {
        realtimeManager.handleConnectionError()
    }
}
