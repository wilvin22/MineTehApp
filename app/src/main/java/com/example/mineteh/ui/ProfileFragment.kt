package com.example.mineteh.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mineteh.Login
import com.example.mineteh.R
import com.example.mineteh.utils.TokenManager
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = TokenManager(requireContext())
        
        // Set up user info and logout functionality
        setupUserInfo(view)
        setupLogoutButton(view)
    }
    
    private fun setupUserInfo(view: View) {
        val userNameTextView = view.findViewById<TextView>(R.id.user_name)
        val userEmailTextView = view.findViewById<TextView>(R.id.user_email)
        
        // Display user information if available
        val userName = tokenManager.getUserName()
        val userEmail = tokenManager.getUserEmail()
        
        userNameTextView.text = userName ?: "User Name"
        userEmailTextView.text = userEmail ?: "user@example.com"
    }
    
    private fun setupLogoutButton(view: View) {
        val logoutButton = view.findViewById<MaterialButton>(R.id.logout_button)
        logoutButton.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        // Clear all user data
        tokenManager.clearAll()
        
        // Navigate back to login
        val intent = Intent(requireContext(), Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        // Finish the current activity
        requireActivity().finish()
    }
}