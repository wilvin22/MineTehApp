package com.example.mineteh.model

data class ChatMessageModel(
    val content: String,
    val isSentByMe: Boolean,
    val time: String
)