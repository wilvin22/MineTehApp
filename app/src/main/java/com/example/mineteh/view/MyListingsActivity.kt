package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// Kept for backward compatibility — delegates to ManageListingsActivity
class MyListingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, ManageListingsActivity::class.java))
        finish()
    }
}
