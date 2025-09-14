package com.example.templet1

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class LinksAdapter(
    private var links: List<LinkModel>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<LinksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.link_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val link = links[position]
        holder.tvName.text = link.websiteName
        holder.progressBar.visibility = View.VISIBLE

        val photoUrl = link.photoUrl
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_upload_image)
                .error(R.drawable.ic_upload_image)
                .circleCrop()
                .listener(object : RequestListener<android.graphics.drawable.Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(holder.ivPhoto)
        } else {
            holder.progressBar.visibility = View.GONE
            holder.ivPhoto.setImageResource(R.drawable.ic_upload_image)
        }

        holder.itemView.setOnClickListener { onClick(link.url) }
    }

    override fun getItemCount() = links.size

    fun updateData(newLinks: List<LinkModel>) {
        links = newLinks
        notifyDataSetChanged()
    }
}
