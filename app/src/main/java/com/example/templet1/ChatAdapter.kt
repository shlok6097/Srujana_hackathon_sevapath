package com.example.templet1

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val USER = 0
    private val BOT = 1

    override fun getItemViewType(position: Int) = if (messages[position].isUser) USER else BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == USER) {
            UserViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            BotViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]

        if (holder is UserViewHolder) {
            holder.message.text = msg.text
        }

        if (holder is BotViewHolder) {
            holder.message.visibility = View.VISIBLE
            holder.image.visibility = View.GONE

            when (msg.type) {
                MessageType.TEXT -> holder.message.text = msg.text
                MessageType.LINK -> {
                    holder.message.text = msg.text
                    holder.message.setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.text))
                            holder.itemView.context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(holder.itemView.context, "Invalid URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                MessageType.IMAGE -> {
                    holder.message.visibility = View.GONE
                    holder.image.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(msg.text)
                        .into(holder.image)
                }
                MessageType.VIDEO, MessageType.INFOGRAPHIC -> {
                    holder.message.text = "Tap to view"
                    holder.message.setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.text))
                            holder.itemView.context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(holder.itemView.context, "Invalid URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun addMessage(message: ChatMessage, recyclerView: RecyclerView) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.messageText)
        // Removed image to prevent crash
    }

    inner class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.messageText)
        val image: ImageView = view.findViewById(R.id.messageImage)
    }
}
