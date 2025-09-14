package com.example.templet1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class file : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FileAdapter
    private val files = mutableListOf<FileModel>()

    private lateinit var etFileName: EditText
    private lateinit var etSearch: EditText
    private lateinit var etFullName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnUploadFile: Button
    private lateinit var btnAddUser: Button

    private var selectedUri: Uri? = null
    private var selectedFileType: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    // Pickers
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedUri = it
                selectedFileType = "image"
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let {
                    selectedUri = it
                    selectedFileType = "pdf"
                    Toast.makeText(requireContext(), "PDF selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedUri = it
                selectedFileType = "video"
                Toast.makeText(requireContext(), "Video selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val view = inflater.inflate(R.layout.fragment_file, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewFiles)
        etFileName = view.findViewById(R.id.etFileName)
        etSearch = view.findViewById(R.id.etSearch)
        etFullName = view.findViewById(R.id.etFullName)
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        btnUploadFile = view.findViewById(R.id.btnUploadFile)
        btnAddUser = view.findViewById(R.id.btnAddUser)

        val btnUploadImage: MaterialCardView = view.findViewById(R.id.card_upload_image)
        val btnUploadPdf: MaterialCardView = view.findViewById(R.id.card_upload_pdf)
        val btnUploadVideo: MaterialCardView = view.findViewById(R.id.card_upload_video)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileAdapter(
            files,
            onViewClick = { openFile(it) },
            onDownloadClick = { downloadFile(it) },
            onDeleteClick = { deleteFile(it) },
            onShareClick = { shareFile(it) }
        )
        recyclerView.adapter = adapter

        // Get current user ID safely
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Not logged in!", Toast.LENGTH_SHORT).show()
            return view
        }

        // Pickers
        btnUploadImage.setOnClickListener { pickImage.launch("image/*") }
        btnUploadPdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            pickPdf.launch(intent)
        }
        btnUploadVideo.setOnClickListener { pickVideo.launch("video/*") }

        // Upload selected file
        btnUploadFile.setOnClickListener {
            val name = etFileName.text.toString().trim()
            if (selectedUri == null || selectedFileType == null) {
                Toast.makeText(requireContext(), "Select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter file name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadFile(uid, selectedUri!!, selectedFileType!!, name)
        }

        // Add user (password manager)
        btnAddUser.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userData = hashMapOf(
                "fullName" to fullName,
                "username" to username,
                "password" to password,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("users").document(uid)
                .collection("userAccounts")
                .add(userData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "User saved", Toast.LENGTH_SHORT).show()
                    fetchFiles()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Search
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                filterFiles(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchFiles()
        return view
    }

    private fun uploadFile(uid: String, fileUri: Uri, fileType: String, fileName: String) {
        val fileRef = storage.child("users/$uid/uploads/$fileName")

        fileRef.putFile(fileUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                val fileData = hashMapOf(
                    "fileName" to fileName,
                    "fileUrl" to uri.toString(),
                    "fileType" to fileType,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("users").document(uid)
                    .collection("uploads")
                    .add(fileData)
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
        val uid = auth.currentUser?.uid ?: return

        files.clear()

        // Listen for uploads
        db.collection("users").document(uid).collection("uploads")
            .orderBy("timestamp")
            .addSnapshotListener { uploadSnapshot, error ->
                if (error != null) return@addSnapshotListener
                if (uploadSnapshot != null) {
                    val uploadFiles = uploadSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(FileModel::class.java)?.apply {
                            timestamp = doc.getLong("timestamp")
                        }
                    }
                    // Remove old uploads of type "upload" before adding new
                    files.removeAll { it.fileType != "password" }
                    files.addAll(uploadFiles)

                    // Listen for userAccounts (passwords)
                    db.collection("users").document(uid).collection("userAccounts")
                        .orderBy("timestamp")
                        .addSnapshotListener { accSnapshot, accError ->
                            if (accError != null) return@addSnapshotListener
                            if (accSnapshot != null) {
                                val accountFiles = accSnapshot.documents.map { doc ->
                                    FileModel(
                                        fileName = doc.getString("fullName") ?: "Account",
                                        fileUrl = "",
                                        fileType = "password",
                                        fullName = doc.getString("fullName"),
                                        username = doc.getString("username"),
                                        password = doc.getString("password"),
                                        timestamp = doc.getLong("timestamp")
                                    )
                                }
                                // Remove old password entries before adding
                                files.removeAll { it.fileType == "password" }
                                files.addAll(accountFiles)

                                adapter.updateList(files.sortedByDescending { it.timestamp })
                            }
                        }
                }
            }
    }

    private fun filterFiles(query: String) {
        val filtered = if (query.isEmpty()) files
        else files.filter { file ->
            val dateString = file.timestamp?.let {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(it))
            } ?: ""

            (file.fileName?.contains(query, ignoreCase = true) ?: false) ||
                    (file.username?.contains(query, ignoreCase = true) ?: false) ||
                    (file.fullName?.contains(query, ignoreCase = true) ?: false) ||
                    dateString.contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered)
    }

    private fun deleteFile(file: FileModel) {
        val uid = auth.currentUser?.uid ?: return

        if (file.fileType == "password") {
            db.collection("users").document(uid)
                .collection("userAccounts")
                .whereEqualTo("username", file.username)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        db.collection("users").document(uid)
                            .collection("userAccounts").document(doc.id).delete()
                    }
                    Toast.makeText(requireContext(), "Deleted account!", Toast.LENGTH_SHORT).show()
                    fetchFiles()
                }
        } else {
            val fileUrl = file.fileUrl
            if (fileUrl.isNullOrEmpty()) return

            val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
            fileRef.delete().addOnSuccessListener {
                db.collection("users").document(uid)
                    .collection("uploads")
                    .whereEqualTo("fileUrl", fileUrl)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot) {
                            db.collection("users").document(uid)
                                .collection("uploads").document(doc.id).delete()
                        }
                        Toast.makeText(requireContext(), "Deleted file!", Toast.LENGTH_SHORT).show()
                        fetchFiles()
                    }
            }
        }
    }

    private fun openFile(file: FileModel) {
        if (file.fileType == "password") {
            Toast.makeText(
                requireContext(),
                "Username: ${file.username}\nPassword: ${file.password}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            val type = when (file.fileType) {
                "pdf" -> "application/pdf"
                "video" -> "video/*"
                else -> "image/*"
            }
            intent.setDataAndType(Uri.parse(file.fileUrl), type)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }
    }

    private fun downloadFile(file: FileModel) {
        if (file.fileType == "password") {
            Toast.makeText(requireContext(), "Passwords can't be downloaded", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.fileUrl))
            startActivity(intent)
        }
    }

    private fun shareFile(file: FileModel) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        if (file.fileType == "password") {
            shareIntent.type = "text/plain"
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Account: ${file.username}\nPassword: ${file.password}"
            )
        } else {
            shareIntent.type = when (file.fileType) {
                "pdf" -> "application/pdf"
                "video" -> "video/*"
                else -> "image/*"
            }
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing File")
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check this file: ${file.fileUrl}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}
