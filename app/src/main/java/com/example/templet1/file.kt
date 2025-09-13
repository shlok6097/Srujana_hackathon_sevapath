package com.example.templet1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class file : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnUploadImage: Button
    private lateinit var btnUploadPdf: Button
    private lateinit var adapter: FileAdapter
    private val files = mutableListOf<FileModel>()

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    // Pick image
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadFile(it, "image") }
        }

    // Pick pdf
    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> uploadFile(uri, "pdf") }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_file, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewFiles)
        val btnUploadImage: MaterialCardView = view.findViewById(R.id.card_upload_image)
        val btnUploadPdf: MaterialCardView = view.findViewById(R.id.card_upload_pdf)



        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileAdapter(
            files,
            onViewClick = { openFile(it) },
            onDownloadClick = { downloadFile(it) },
            onDeleteClick = { deleteFile(it) },
            onShareClick = { shareFile(it) }
        )
        recyclerView.adapter = adapter

        btnUploadImage.setOnClickListener { pickImage.launch("image/*") }

        btnUploadPdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            pickPdf.launch(intent)
        }

        fetchFiles()

        return view
    }

    private fun uploadFile(fileUri: Uri, fileType: String) {
        val fileName = "${System.currentTimeMillis()}_${fileUri.lastPathSegment}"
        val fileRef = storage.child("uploads/$fileName")

        fileRef.putFile(fileUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                val fileData = hashMapOf(
                    "fileName" to fileName,
                    "fileUrl" to uri.toString(),
                    "fileType" to fileType,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("uploads").add(fileData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show()
                        fetchFiles()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFiles() {
        db.collection("uploads")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                files.clear()
                files.addAll(result.toObjects(FileModel::class.java))
                adapter.notifyDataSetChanged()
            }
    }

    private fun deleteFile(file: FileModel) {
        val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(file.fileUrl)
        fileRef.delete().addOnSuccessListener {
            db.collection("uploads").whereEqualTo("fileUrl", file.fileUrl).get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        db.collection("uploads").document(doc.id).delete()
                    }
                    Toast.makeText(requireContext(), "Deleted!", Toast.LENGTH_SHORT).show()
                    fetchFiles()
                }
        }
    }

    private fun openFile(file: FileModel) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(file.fileUrl),
            if (file.fileType == "pdf") "application/pdf" else "image/*"
        )
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
    }

    private fun downloadFile(file: FileModel) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.fileUrl))
        startActivity(intent)
    }

    private fun shareFile(file: FileModel) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = if (file.fileType == "pdf") "application/pdf" else "image/*"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing File")
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check this file: ${file.fileUrl}")
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}
