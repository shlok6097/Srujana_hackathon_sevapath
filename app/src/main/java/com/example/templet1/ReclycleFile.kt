package com.example.templet1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReclycleFile(
    private var fileList: List<FileModel>,
    private val onViewClick: (FileModel) -> Unit,
    private val onDeleteClick: (FileModel) -> Unit,
    private val onDownloadClick: (FileModel) -> Unit,
    private val onShareClick: (FileModel) -> Unit
) : RecyclerView.Adapter<ReclycleFile.FileViewHolder>() {

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvFileName)
        val btnView: ImageView = itemView.findViewById(R.id.btnView)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        val btnDownload: ImageView = itemView.findViewById(R.id.btnDownload)
        val btnShare: ImageView = itemView.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fil, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.tvName.text = file.fileName

        holder.btnView.setOnClickListener { onViewClick(file) }
        holder.btnDelete.setOnClickListener { onDeleteClick(file) }
        holder.btnDownload.setOnClickListener { onDownloadClick(file) }
        holder.btnShare.setOnClickListener { onShareClick(file) }
    }

    override fun getItemCount() = fileList.size

    fun updateList(newList: List<FileModel>) {
        fileList = newList
        notifyDataSetChanged()
    }
}
