package com.example.templet1

data class NotificationItem(
    val id: Int,
    val iconRes: Int, // single field for the icon
    val title: String,
    val body: String,
    val timestamp: String,
    val unread: Boolean = true, // unread dot
    val hasThumbnail: Boolean = false,
    val thumbnailRes: Int? = null


)
