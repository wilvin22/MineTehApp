package com.example.mineteh.utils

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "mineteh_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    fun saveUser(context: Context, userId: Int, username: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getUserId(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_USER_ID, -1)
    }

    fun getUsername(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, null)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }
}
