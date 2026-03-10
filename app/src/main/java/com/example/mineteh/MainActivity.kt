package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
