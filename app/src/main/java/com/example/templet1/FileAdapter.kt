package com.example.templet1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class FileAdapter(
    private var files: MutableList<FileModel>,   // ✅ Mutable so we can update
    private val onViewClick: (FileModel) -> Unit,
    private val onDownloadClick: (FileModel) -> Unit,
    private val onDeleteClick: (FileModel) -> Unit,
    private val onShareClick: (FileModel) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPreview: ImageView = itemView.findViewById(R.id.imgPreview)
        val txtFileName: TextView = itemView.findViewById(R.id.txtFileName)
        val txtFileType: TextView = itemView.findViewById(R.id.txtFileType)
        val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownload)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnShare: ImageButton = itemView.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        when (file.fileType) {
            "image" -> {
                holder.txtFileName.text = file.fileName
                holder.txtFileType.text = "Image"
                Glide.with(holder.itemView.context)
                    .load(file.fileUrl)
                    .apply(RequestOptions().centerCrop())
                    .into(holder.imgPreview)
            }
            "pdf" -> {
                holder.txtFileName.text = file.fileName
                holder.txtFileType.text = "PDF"
                holder.imgPreview.setImageResource(R.drawable.ic_pdf)
            }
            "video" -> {
                holder.txtFileName.text = file.fileName
                holder.txtFileType.text = "Video"
                Glide.with(holder.itemView.context)
                    .load(file.fileUrl) // may need Uri.parse(file.fileUrl)
                    .apply(RequestOptions().frame(1000000)) // first second
                    .into(holder.imgPreview)
            }
            "password" -> {
                holder.txtFileName.text = "Account: ${file.username ?: "Unknown"}"
                holder.txtFileType.text = "Password: ${maskPassword(file.password)}"
                holder.imgPreview.setImageResource(R.drawable.ic_lock) // custom lock icon
            }
            else -> {
                holder.txtFileName.text = file.fileName
                holder.txtFileType.text = "Unknown"
                holder.imgPreview.setImageResource(R.drawable.ic_file)
            }
        }

        holder.btnView.setOnClickListener { onViewClick(file) }
        holder.btnDownload.setOnClickListener { onDownloadClick(file) }
        holder.btnDelete.setOnClickListener { onDeleteClick(file) }
        holder.btnShare.setOnClickListener { onShareClick(file) }
    }

    override fun getItemCount(): Int = files.size

    private fun maskPassword(password: String?): String {
        return if (password.isNullOrEmpty()) "N/A" else "•".repeat(password.length)
    }

    // ✅ This lets you refresh the list
    fun updateList(newFiles: List<FileModel>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }
}
