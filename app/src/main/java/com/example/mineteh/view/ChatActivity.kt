package com.example.mineteh.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.view.ChatAdapter
import com.example.mineteh.R
import com.example.mineteh.databinding.ChatBinding
import com.example.mineteh.model.ChatMessageModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ChatBinding
    private val chatMessages = mutableListOf<ChatMessageModel>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get intent data
        val senderName = intent.getStringExtra("senderName") ?: "User"
        val profileImage = intent.getIntExtra("profileImage", R.drawable.ic_launcher_background)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.chatUserName.text = senderName
        binding.chatProfileImage.setImageResource(profileImage)

        setupRecyclerView()
        loadDummyMessages()

        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Start from bottom
        }
        binding.chatRecyclerView.adapter = adapter
    }

    private fun loadDummyMessages() {
        chatMessages.add(ChatMessageModel("Hello! Is this still available?", false, "10:00 AM"))
        chatMessages.add(ChatMessageModel("Yes, it is! Are you interested?", true, "10:05 AM"))
        chatMessages.add(ChatMessageModel("Yes, I'd like to place a bid.", false, "10:10 AM"))
        adapter.notifyDataSetChanged()
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun sendMessage(content: String) {
        val newMessage = ChatMessageModel(content, true, "Now")
        chatMessages.add(newMessage)
        adapter.notifyDataSetChanged()
        binding.etMessage.text.clear()
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }
}