package com.example.mineteh.utils

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.gotrue.user.UserSession

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "mineteh_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_TOKEN_SAVED_AT = "token_saved_at"

        // New keys for Supabase session management
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_SESSION = "supabase_session"
        private const val KEY_FCM_TOKEN = "fcm_token"

        // Tokens expire after 30 days
        private const val TOKEN_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000
    }

    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: Int) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun setRememberMe(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun saveSavedEmail(email: String) {
        prefs.edit().putString(KEY_SAVED_EMAIL, email).apply()
    }

    fun getSavedEmail(): String? {
        return prefs.getString(KEY_SAVED_EMAIL, null)
    }

    /**
     * Saves a Supabase session to SharedPreferences.
     * Stores both access token and refresh token for session management.
     * 
     * @param session The Supabase UserSession to save
     */
    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            // Also save to legacy KEY_TOKEN for backward compatibility
            putString(KEY_TOKEN, session.accessToken)
            apply()
        }
    }

    /**
     * Retrieves the access token from the stored session.
     * 
     * @return Access token string if available, null otherwise
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Retrieves the refresh token from the stored session.
     * 
     * @return Refresh token string if available, null otherwise
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun clearAll() {
        // If remember me is enabled, we might want to keep the email but clear the token
        val rememberMe = isRememberMeEnabled()
        val savedEmail = getSavedEmail()
        
        prefs.edit().clear().apply()
        
        if (rememberMe) {
            setRememberMe(true)
            savedEmail?.let { saveSavedEmail(it) }
        }
    }

    fun isLoggedIn(): Boolean {
        // Check both legacy token and new access token for backward compatibility
        return getToken() != null || getAccessToken() != null
    }

    fun isTokenExpired(): Boolean {
        val savedAt = prefs.getLong(KEY_TOKEN_SAVED_AT, 0L)
        if (savedAt == 0L) return false // no timestamp means old session, treat as valid
        return System.currentTimeMillis() - savedAt > TOKEN_EXPIRY_MS
    }

    /**
     * Saves the FCM token for push notifications.
     * 
     * @param token The FCM token to save
     */
    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    /**
     * Retrieves the stored FCM token.
     * 
     * @return FCM token string if available, null otherwise
     */
    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Clears the stored FCM token.
     */
    fun clearFcmToken() {
        prefs.edit().remove(KEY_FCM_TOKEN).apply()
    }
}
