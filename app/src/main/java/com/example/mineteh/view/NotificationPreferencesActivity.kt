package com.example.mineteh.view

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.R
import com.example.mineteh.Resource
import com.example.mineteh.model.NotificationPreferences
import com.example.mineteh.viewmodel.NotificationsViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class NotificationPreferencesActivity : AppCompatActivity() {

    private val viewModel: NotificationsViewModel by viewModels()
    
    // UI Components
    private lateinit var backButton: ImageView
    private lateinit var progressBar: ProgressBar
    
    // Notification type switches
    private lateinit var bidPlacedSwitch: SwitchMaterial
    private lateinit var bidOutbidSwitch: SwitchMaterial
    private lateinit var auctionEndingSwitch: SwitchMaterial
    private lateinit var auctionWonSwitch: SwitchMaterial
    private lateinit var auctionLostSwitch: SwitchMaterial
    private lateinit var itemSoldSwitch: SwitchMaterial
    private lateinit var newMessageSwitch: SwitchMaterial
    private lateinit var listingApprovedSwitch: SwitchMaterial
    private lateinit var paymentReceivedSwitch: SwitchMaterial
    
    // Global settings
    private lateinit var pushNotificationsSwitch: SwitchMaterial
    
    // Quiet hours
    private lateinit var quietHoursSwitch: SwitchMaterial
    private lateinit var quietHoursStartText: TextView
    private lateinit var quietHoursEndText: TextView
    
    private var currentPreferences: NotificationPreferences? = null
    private var quietHoursStart: String? = null
    private var quietHoursEnd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_preferences)
        
        initializeViews()
        setupClickListeners()
        observeViewModel()
        
        // Load current preferences
        viewModel.loadPreferences()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        progressBar = findViewById(R.id.progressBar)
        
        // Notification type switches
        bidPlacedSwitch = findViewById(R.id.bidPlacedSwitch)
        bidOutbidSwitch = findViewById(R.id.bidOutbidSwitch)
        auctionEndingSwitch = findViewById(R.id.auctionEndingSwitch)
        auctionWonSwitch = findViewById(R.id.auctionWonSwitch)
        auctionLostSwitch = findViewById(R.id.auctionLostSwitch)
        itemSoldSwitch = findViewById(R.id.itemSoldSwitch)
        newMessageSwitch = findViewById(R.id.newMessageSwitch)
        listingApprovedSwitch = findViewById(R.id.listingApprovedSwitch)
        paymentReceivedSwitch = findViewById(R.id.paymentReceivedSwitch)
        
        // Global settings
        pushNotificationsSwitch = findViewById(R.id.pushNotificationsSwitch)
        
        // Quiet hours
        quietHoursSwitch = findViewById(R.id.quietHoursSwitch)
        quietHoursStartText = findViewById(R.id.quietHoursStartText)
        quietHoursEndText = findViewById(R.id.quietHoursEndText)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        // Save preferences when switches change
        val switches = listOf(
            bidPlacedSwitch, bidOutbidSwitch, auctionEndingSwitch,
            auctionWonSwitch, auctionLostSwitch, itemSoldSwitch,
            newMessageSwitch, listingApprovedSwitch, paymentReceivedSwitch,
            pushNotificationsSwitch
        )
        
        switches.forEach { switch ->
            switch.setOnCheckedChangeListener { _, _ ->
                savePreferences()
            }
        }
        
        // Quiet hours switch
        quietHoursSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateQuietHoursVisibility(isChecked)
            savePreferences()
        }
        
        // Quiet hours time pickers
        quietHoursStartText.setOnClickListener {
            showTimePicker(true)
        }
        
        quietHoursEndText.setOnClickListener {
            showTimePicker(false)
        }
    }

    private fun observeViewModel() {
        viewModel.preferences.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    resource.data?.let { preferences ->
                        currentPreferences = preferences
                        updateUI(preferences)
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }
    }

    private fun updateUI(preferences: NotificationPreferences) {
        // Update notification type switches
        bidPlacedSwitch.isChecked = preferences.bidPlacedEnabled
        bidOutbidSwitch.isChecked = preferences.bidOutbidEnabled
        auctionEndingSwitch.isChecked = preferences.auctionEndingEnabled
        auctionWonSwitch.isChecked = preferences.auctionWonEnabled
        auctionLostSwitch.isChecked = preferences.auctionLostEnabled
        itemSoldSwitch.isChecked = preferences.itemSoldEnabled
        newMessageSwitch.isChecked = preferences.newMessageEnabled
        listingApprovedSwitch.isChecked = preferences.listingApprovedEnabled
        paymentReceivedSwitch.isChecked = preferences.paymentReceivedEnabled
        
        // Update global settings
        pushNotificationsSwitch.isChecked = preferences.pushNotificationsEnabled
        
        // Update quiet hours
        val hasQuietHours = preferences.quietHoursStart != null && preferences.quietHoursEnd != null
        quietHoursSwitch.isChecked = hasQuietHours
        
        if (hasQuietHours) {
            quietHoursStart = preferences.quietHoursStart
            quietHoursEnd = preferences.quietHoursEnd
            updateQuietHoursDisplay()
        }
        
        updateQuietHoursVisibility(hasQuietHours)
    }

    private fun savePreferences() {
        currentPreferences?.let { current ->
            val updatedPreferences = current.copy(
                bidPlacedEnabled = bidPlacedSwitch.isChecked,
                bidOutbidEnabled = bidOutbidSwitch.isChecked,
                auctionEndingEnabled = auctionEndingSwitch.isChecked,
                auctionWonEnabled = auctionWonSwitch.isChecked,
                auctionLostEnabled = auctionLostSwitch.isChecked,
                itemSoldEnabled = itemSoldSwitch.isChecked,
                newMessageEnabled = newMessageSwitch.isChecked,
                listingApprovedEnabled = listingApprovedSwitch.isChecked,
                paymentReceivedEnabled = paymentReceivedSwitch.isChecked,
                pushNotificationsEnabled = pushNotificationsSwitch.isChecked,
                quietHoursStart = if (quietHoursSwitch.isChecked) quietHoursStart else null,
                quietHoursEnd = if (quietHoursSwitch.isChecked) quietHoursEnd else null
            )
            
            viewModel.updatePreferences(updatedPreferences)
        }
    }

    private fun updateQuietHoursVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        findViewById<View>(R.id.quietHoursTimeContainer).visibility = visibility
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentTime = if (isStartTime) quietHoursStart else quietHoursEnd
        
        // Parse current time if available
        if (currentTime != null) {
            try {
                val parts = currentTime.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
            } catch (e: Exception) {
                // Use current time if parsing fails
            }
        }
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val timeString = String.format("%02d:%02d", hourOfDay, minute)
                if (isStartTime) {
                    quietHoursStart = timeString
                } else {
                    quietHoursEnd = timeString
                }
                updateQuietHoursDisplay()
                savePreferences()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        )
        
        timePickerDialog.setTitle(if (isStartTime) "Select Start Time" else "Select End Time")
        timePickerDialog.show()
    }

    private fun updateQuietHoursDisplay() {
        quietHoursStartText.text = formatTime(quietHoursStart) ?: "Select start time"
        quietHoursEndText.text = formatTime(quietHoursEnd) ?: "Select end time"
    }

    private fun formatTime(timeString: String?): String? {
        if (timeString == null) return null
        
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = inputFormat.parse(timeString)
            time?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            timeString
        }
    }
}