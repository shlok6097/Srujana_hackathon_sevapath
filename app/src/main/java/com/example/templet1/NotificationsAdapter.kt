package com.example.templet1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class NotificationsAdapter(
    private val items: List<NotificationItem>,
    private val onClick: ((NotificationItem) -> Unit)? = null
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ShapeableImageView = view.findViewById(R.id.notification_icon)
        val title: TextView = view.findViewById(R.id.notification_title)
        val body: TextView = view.findViewById(R.id.notification_body)
        val timestamp: TextView = view.findViewById(R.id.notification_timestamp)
        val unreadDot: View = view.findViewById(R.id.unread_dot)
        val thumbnail: ShapeableImageView = view.findViewById(R.id.notification_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.title.text = item.title
        holder.body.text = item.body
        holder.timestamp.text = item.timestamp
        holder.unreadDot.visibility = if (item.unread) View.VISIBLE else View.GONE

        if (item.hasThumbnail && item.thumbnailRes != null) {
            holder.thumbnail.visibility = View.VISIBLE
            holder.thumbnail.setImageResource(item.thumbnailRes)
        } else {
            holder.thumbnail.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size
}
