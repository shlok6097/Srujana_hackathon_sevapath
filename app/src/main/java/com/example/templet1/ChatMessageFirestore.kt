package com.example.templet1

// A simple class that is safe to save to Firestore
data class ChatMessageFirestore(
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = MessageType.TEXT.name // Store the enum's name
)