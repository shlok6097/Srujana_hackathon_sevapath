package com.example.templet1

import java.lang.System.currentTimeMillis

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
   // val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT
)


