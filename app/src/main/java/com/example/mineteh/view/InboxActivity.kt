package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.MessagingViewModel
import com.example.mineteh.viewmodel.NotificationsViewModel

class InboxActivity : AppCompatActivity() {

    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private val messagingViewModel: MessagingViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var emptyText: TextView
    private lateinit var loadingView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inbox)

        recyclerView = findViewById(R.id.messageRecyclerView)
        emptyView = findViewById(R.id.emptyStateContainer)
        emptyText = findViewById(R.id.emptyMessagesText)
        loadingView = findViewById(R.id.loadingProgress)
        
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupObservers()
        messagingViewModel.loadConversations()

        findViewById<android.widget.ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun setupObservers() {
        messagingViewModel.conversations.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    loadingView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.GONE
                }
                is Resource.Success -> {
                    loadingView.visibility = View.GONE
                    val conversations = resource.data ?: emptyList()
                    if (conversations.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                        recyclerView.adapter = ConversationAdapter(conversations) { conversation ->
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                putExtra("conversation_id", conversation.conversationId)
                                putExtra("other_user_id", conversation.otherUser?.accountId ?: -1)
                                putExtra("other_user_name", conversation.otherUser?.username ?: "User")
                            putExtra("other_user_first_name", conversation.otherUser?.firstName ?: "")
                            putExtra("other_user_last_name", conversation.otherUser?.lastName ?: "")
                                conversation.listingId?.let { putExtra("listing_id", it) }
                            }
                            startActivity(intent)
                        }
                    }
                }
                is Resource.Error -> {
                    loadingView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyText.text = resource.message ?: "Failed to load conversations"
                    Toast.makeText(this, resource.message ?: "Failed to load conversations", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh conversations when returning to inbox
        messagingViewModel.loadConversations()
    }
}