package com.example.mineteh.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.R
import com.example.mineteh.databinding.ChatBinding
import com.example.mineteh.model.ChatMessageModel
import com.example.mineteh.models.Message
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.MessagingViewModel
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ChatBinding
    private val chatMessages = mutableListOf<ChatMessageModel>()
    private lateinit var adapter: ChatAdapter
    private val viewModel: MessagingViewModel by viewModels()
    
    private var conversationId: Int = -1
    private var otherUserId: Int = -1
    private var listingId: Int? = null
    private var currentUserId: Int = -1
    
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (conversationId != -1) {
                viewModel.loadMessages(conversationId)
            }
            handler.postDelayed(this, 3000) // Refresh every 3 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get current user ID
        val tokenManager = com.example.mineteh.utils.TokenManager(this)
        currentUserId = tokenManager.getUserId()

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Get intent data
        conversationId = intent.getIntExtra("conversation_id", -1)
        otherUserId = intent.getIntExtra("other_user_id", -1)
        listingId = intent.getIntExtra("listing_id", -1).takeIf { it != -1 }
        val initialMessage = intent.getStringExtra("initial_message")
        
        val otherUserName = intent.getStringExtra("other_user_name") ?: "User"
        binding.chatUserName.text = otherUserName

        setupRecyclerView()
        setupObservers()
        
        // If we have conversation ID, load messages
        if (conversationId != -1) {
            viewModel.loadMessages(conversationId)
            startAutoRefresh()
        } else if (otherUserId != -1) {
            // Create or get conversation, then send initial message if provided
            viewModel.getOrCreateConversation(otherUserId, listingId, initialMessage)
        } else {
            Toast.makeText(this, "Invalid conversation data", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotEmpty() && conversationId != -1) {
                viewModel.sendMessage(conversationId, content)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.adapter = adapter
    }
    
    private fun setupObservers() {
        // Observe conversation creation
        viewModel.conversation.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { conv ->
                        conversationId = conv.conversationId
                        viewModel.loadMessages(conversationId)
                        startAutoRefresh()
                    }
                    viewModel.resetConversationResult()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to create conversation", Toast.LENGTH_SHORT).show()
                    viewModel.resetConversationResult()
                }
                else -> {}
            }
        }
        
        // Observe messages
        viewModel.messages.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    resource.data?.let { messages ->
                        displayMessages(messages)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Observe send message result
        viewModel.sendMessageResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSend.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnSend.isEnabled = true
                    viewModel.resetSendMessageResult()
                }
                is Resource.Error -> {
                    binding.btnSend.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Failed to send message", Toast.LENGTH_SHORT).show()
                    viewModel.resetSendMessageResult()
                }
                null -> {
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }
    
    private fun displayMessages(messages: List<Message>) {
        chatMessages.clear()
        messages.forEach { msg ->
            val isSent = msg.senderId == currentUserId
            val time = formatTime(msg.sentAt)
            chatMessages.add(ChatMessageModel(msg.messageText, isSent, time))
        }
        adapter.notifyDataSetChanged()
        if (chatMessages.isNotEmpty()) {
            binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
        }
    }
    
    private fun formatTime(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp)
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: "Now"
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error formatting time", e)
            "Now"
        }
    }
    
    private fun startAutoRefresh() {
        handler.postDelayed(refreshRunnable, 3000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun getMessageDataFromIntent(): Pair<Int?, Int?> {
        // First check for deep link URI
        intent.data?.let { uri ->
            if (uri.scheme == "mineteh" && uri.host == "chat") {
                val pathSegments = uri.pathSegments
                val messageId = if (pathSegments.isNotEmpty()) {
                    pathSegments[0].toIntOrNull()
                } else null
                
                val senderId = uri.getQueryParameter("sender_id")?.toIntOrNull()
                return Pair(messageId, senderId)
            }
        }
        
        // Fallback to regular intent extras
        val messageId = intent.getIntExtra("message_id", -1).takeIf { it != -1 }
        val senderId = intent.getIntExtra("sender_id", -1).takeIf { it != -1 }
        return Pair(messageId, senderId)
    }
}