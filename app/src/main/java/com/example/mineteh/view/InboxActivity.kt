package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.view.MessageAdapter
import com.example.mineteh.view.ProfileActivity
import com.example.mineteh.R
import com.example.mineteh.view.SellActivity
import com.example.mineteh.model.MessageModel
import com.example.mineteh.utils.NotificationBadgeManager
import com.example.mineteh.viewmodel.NotificationsViewModel
import androidx.activity.viewModels
import android.widget.TextView

class InboxActivity : AppCompatActivity() {

    private val notificationsViewModel: NotificationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inbox)

        val recyclerView = findViewById<RecyclerView>(R.id.messageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val messageList = listOf(
            MessageModel(
                "Kalamofree",
                "You've been outbid!",
                "1m",
                R.drawable.ic_launcher_background
            ),
            MessageModel(
                "PogiDiscounts",
                "Congrats! You've won this bid for 800 pesos!",
                "4m",
                R.drawable.ic_launcher_background
            ),
            MessageModel(
                "BuyNaBesh",
                "You: Yes, besh. Available pa kaya bili ka na! 😉",
                "11m",
                R.drawable.ic_launcher_background
            ),
            MessageModel(
                "GastosPaMORE",
                "Bili na bago maubos!",
                "2d",
                R.drawable.ic_launcher_background
            )
        )

        recyclerView.adapter = MessageAdapter(messageList)

        // Setup notification badge
        val notificationBadge = findViewById<TextView>(R.id.notificationBadge)
        NotificationBadgeManager.setupBadge(this, this, notificationsViewModel, notificationBadge)

        // Navigation setup
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_notifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_sell).setOnClickListener {
            startActivity(Intent(this, SellActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }
}