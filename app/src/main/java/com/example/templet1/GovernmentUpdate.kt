package com.example.templet1

data class GovernmentUpdate(
    val title: String,
    val description: String,
    val date: String,
    val link: String,
    val imageUrl: String? = null, // optional photo
    val type: String // e.g., "Cybercrime", "Flood", "Health", etc.
)
