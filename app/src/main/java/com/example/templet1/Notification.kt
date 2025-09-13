package com.example.templet1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Notification : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find RecyclerView from fragment_notification.xml
        val recyclerView = view.findViewById<RecyclerView>(R.id.notifications_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Dummy data (3 welcome notifications)
        val dummyNotifications = listOf(
            NotificationItem(
                id=1,
                iconRes = R.drawable.logo,
                title = "Welcome to MyApp!",
                body = "We're so excited to have you on board. Let's get started on your journey.",
                timestamp = "Just now",
                unread = true
            ),
            NotificationItem(
                id = 2,
                iconRes = R.drawable.ic_account_circle, // Material icon
                title = "Complete Your Profile",
                body = "Make your profile shine! Add a photo and a short bio to connect with others.",
                timestamp = "2m ago",
                unread = true
            ),
            NotificationItem(
                id = 3,
                iconRes = R.drawable.ic_lightbulb, // Material icon
                title = "Did You Know?",
                body = "You can customize your experience in the settings menu. Tap here to explore!",
                timestamp = "10m ago",
                unread = false
            )
        )

        // Attach adapter
        recyclerView.adapter = NotificationsAdapter(dummyNotifications) { item ->
            // Simple click action
            Toast.makeText(requireContext(), "Clicked: ${item.title}", Toast.LENGTH_SHORT).show()
        }

        // Optional: add divider lines between items
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(divider)
    }
}
