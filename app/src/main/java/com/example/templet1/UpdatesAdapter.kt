package com.example.templet1

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UpdatesAdapter(private val updates: List<GovernmentUpdate>) :
    RecyclerView.Adapter<UpdatesAdapter.UpdateViewHolder>() {

    inner class UpdateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val desc: TextView = itemView.findViewById(R.id.tvDescription)
        val date: TextView = itemView.findViewById(R.id.tvDate)
        val image: ImageView = itemView.findViewById(R.id.ivPhoto)
        val type: TextView = itemView.findViewById(R.id.tvType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_government_update, parent, false)
        return UpdateViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        val update = updates[position]
        holder.title.text = update.title
        holder.desc.text = update.description
        holder.date.text = update.date
        holder.type.text = update.type

        if (!update.imageUrl.isNullOrEmpty()) {
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(update.imageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.error)
                .into(holder.image)
        } else {
            holder.image.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.link))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = updates.size
}
