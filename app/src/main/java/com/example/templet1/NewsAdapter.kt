package com.example.templet1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NewsAdapter(private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.newsImage)
        val heading: TextView = view.findViewById(R.id.newsHeading)
        val description: TextView = view.findViewById(R.id.newsDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = newsList[position]
        holder.heading.text = item.heading
        holder.description.text = item.description
        Glide.with(holder.itemView.context)
            .load(item.image)
            .placeholder(R.drawable.placeholder) // optional
            .error(R.drawable.error)            // optional
            .into(holder.imageView)
    }

    override fun getItemCount() = newsList.size

}
