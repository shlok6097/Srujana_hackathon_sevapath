package com.example.templet1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FileAdapter(
    private val files: List<FileModel>,
    private val onViewClick: (FileModel) -> Unit,
    private val onDownloadClick: (FileModel) -> Unit,
    private val onDeleteClick: (FileModel) -> Unit,
    private val onShareClick: (FileModel) -> Unit   // NEW
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPreview: ImageView = itemView.findViewById(R.id.imgPreview)
        val txtFileName: TextView = itemView.findViewById(R.id.txtFileName)
        val txtFileType: TextView = itemView.findViewById(R.id.txtFileType)
        val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownload)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnShare: ImageButton = itemView.findViewById(R.id.btnShare) // NEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        holder.txtFileName.text = file.fileName
        holder.txtFileType.text = file.fileType

        if (file.fileType == "image") {
            Glide.with(holder.itemView.context).load(file.fileUrl).into(holder.imgPreview)
        } else {
            holder.imgPreview.setImageResource(R.drawable.ic_pdf)
        }

        holder.btnView.setOnClickListener { onViewClick(file) }
        holder.btnDownload.setOnClickListener { onDownloadClick(file) }
        holder.btnDelete.setOnClickListener { onDeleteClick(file) }
        holder.btnShare.setOnClickListener { onShareClick(file) } // NEW
    }

    override fun getItemCount(): Int = files.size
}
