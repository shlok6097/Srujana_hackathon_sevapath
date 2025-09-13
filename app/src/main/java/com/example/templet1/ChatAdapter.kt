package com.example.templet1

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.templet1.R
import com.bumptech.glide.Glide

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val USER = 0
    private val BOT = 1

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) USER else BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (message.type) {
            MessageType.TEXT -> {
                if (holder is UserViewHolder) holder.message.text = message.text
                if (holder is BotViewHolder) holder.message.text = message.text
            }

            MessageType.LINK -> {
                if (holder is BotViewHolder) {
                    holder.message.text = message.text
                    holder.message.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.text))
                        holder.itemView.context.startActivity(intent)
                    }
                }
            }

            MessageType.IMAGE -> {
                if (holder is BotViewHolder) {
                    holder.message.visibility = View.GONE
                    holder.image.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(message.text) // URL of image
                        .into(holder.image)
                }
            }

            MessageType.VIDEO -> {
                if (holder is BotViewHolder) {
                    holder.message.text = "Video: Tap to play"
                    holder.message.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.text))
                        holder.itemView.context.startActivity(intent)
                    }
                }
            }

            MessageType.INFOGRAPHIC -> {
                if (holder is BotViewHolder) {
                    holder.message.text = "Infographic: Tap to view"
                    holder.message.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.text))
                        holder.itemView.context.startActivity(intent)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.messageText)
    }

    inner class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.messageText)
        val image: ImageView = view.findViewById(R.id.messageImage) // make sure your XML has ImageView with this id
    }
}
