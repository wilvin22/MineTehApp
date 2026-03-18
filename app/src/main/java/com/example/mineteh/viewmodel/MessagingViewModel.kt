package com.example.mineteh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mineteh.models.Conversation
import com.example.mineteh.models.Message
import com.example.mineteh.model.repository.MessagingRepository
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.launch

class MessagingViewModel(application: Application) : AndroidViewModel(application) {
    private val messagingRepository = MessagingRepository(application.applicationContext)

    private val _conversations = MutableLiveData<Resource<List<Conversation>>>()
    val conversations: LiveData<Resource<List<Conversation>>> = _conversations

    private val _messages = MutableLiveData<Resource<List<Message>>>()
    val messages: LiveData<Resource<List<Message>>> = _messages

    private val _sendMessageResult = MutableLiveData<Resource<Message>?>()
    val sendMessageResult: LiveData<Resource<Message>?> = _sendMessageResult

    private val _conversation = MutableLiveData<Resource<Conversation>?>()
    val conversation: LiveData<Resource<Conversation>?> = _conversation

    fun loadConversations() {
        _conversations.value = Resource.Loading()

        viewModelScope.launch {
            val result = messagingRepository.getUserConversations()
            _conversations.value = result
        }
    }

    fun loadMessages(conversationId: Int) {
        _messages.value = Resource.Loading()

        viewModelScope.launch {
            val result = messagingRepository.getMessages(conversationId)
            _messages.value = result
            
            // Mark messages as read
            if (result is Resource.Success) {
                messagingRepository.markMessagesAsRead(conversationId)
            }
        }
    }

    fun sendMessage(conversationId: Int, messageText: String) {
        _sendMessageResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = messagingRepository.sendMessage(conversationId, messageText)
            _sendMessageResult.value = result
            
            // Reload messages after sending
            if (result is Resource.Success) {
                loadMessages(conversationId)
            }
        }
    }

    fun getOrCreateConversation(otherUserId: Int, listingId: Int?) {
        _conversation.value = Resource.Loading()

        viewModelScope.launch {
            val result = messagingRepository.getOrCreateConversation(otherUserId, listingId)
            _conversation.value = result
        }
    }

    fun resetSendMessageResult() {
        _sendMessageResult.value = null
    }

    fun resetConversationResult() {
        _conversation.value = null
    }
}
