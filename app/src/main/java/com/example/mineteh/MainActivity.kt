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
        
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setupWithNavController(navController)
        
        // Set up badge indicators (can be updated later with actual counts)
        setupNavigationBadges(bottomNavigation)
    }
    
    private fun setupNavigationBadges(bottomNavigation: BottomNavigationView) {
        // Example: Add badge to cart item
        // val badge = bottomNavigation.getOrCreateBadge(R.id.nav_cart)
        // badge.number = 3 // This would come from your cart data
        // badge.isVisible = true
    }
}
