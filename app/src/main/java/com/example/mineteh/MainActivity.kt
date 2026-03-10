package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mineteh.utils.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        tokenManager = TokenManager(this)
        
        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            // User is not logged in, redirect to login
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Close MainActivity so user can't go back
            return
        }
        
        // User is logged in, show main interface
        setContentView(R.layout.activity_main)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setupWithNavController(navController)
        
        // Set up badge indicators
        setupNavigationBadges()
        
        // Handle navigation item reselection (scroll to top, refresh, etc.)
        bottomNavigation.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Scroll to top or refresh home content
                }
                R.id.nav_search -> {
                    // Clear search or focus search input
                }
                R.id.nav_auctions -> {
                    // Refresh auctions list
                }
                R.id.nav_cart -> {
                    // Scroll to top of cart
                }
                R.id.nav_profile -> {
                    // Scroll to top of profile
                }
            }
        }
    }
    
    private fun setupNavigationBadges() {
        // Example: Add badge to cart item (you can update this with real data)
        // val cartBadge = bottomNavigation.getOrCreateBadge(R.id.nav_cart)
        // cartBadge.number = getCartItemCount() // This would come from your cart data
        // cartBadge.isVisible = cartBadge.number > 0
        
        // Example: Add badge to profile for notifications
        // val profileBadge = bottomNavigation.getOrCreateBadge(R.id.nav_profile)
        // profileBadge.isVisible = hasUnreadNotifications()
    }
    
    // Method to update cart badge (call this when cart changes)
    fun updateCartBadge(count: Int) {
        val cartBadge = bottomNavigation.getOrCreateBadge(R.id.nav_cart)
        cartBadge.number = count
        cartBadge.isVisible = count > 0
    }
    
    // Method to update profile badge (call this when notifications change)
    fun updateProfileBadge(hasNotifications: Boolean) {
        val profileBadge = bottomNavigation.getOrCreateBadge(R.id.nav_profile)
        profileBadge.isVisible = hasNotifications
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh badges when returning to the app
        setupNavigationBadges()
    }
}
