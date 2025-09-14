package com.example.templet1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class help : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private lateinit var etSearch: EditText
    private lateinit var etWebsiteName: EditText
    private lateinit var etWebsiteUrl: EditText
    private lateinit var btnAddLink: Button
    private lateinit var btnSelectPhoto: ImageButton
    private lateinit var rvLinks: RecyclerView
    private lateinit var webView: WebView
    private lateinit var progressBarWeb: ProgressBar

    private var selectedPhotoUri: Uri? = null
    private lateinit var adapter: LinksAdapter
    private var allLinks = mutableListOf<LinkModel>()

    // Useful numbers arrays
    private lateinit var btnCalls: List<ImageButton>
    private lateinit var tvNumbers: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_help, container, false)

        // Initialize views
        etSearch = view.findViewById(R.id.etSearch)
        etWebsiteName = view.findViewById(R.id.etWebsiteName)
        etWebsiteUrl = view.findViewById(R.id.etWebsiteUrl)
        btnAddLink = view.findViewById(R.id.btnAddLink)
        btnSelectPhoto = view.findViewById(R.id.btnSelectPhoto)
        rvLinks = view.findViewById(R.id.rvLinks)
        webView = view.findViewById(R.id.webView)
        progressBarWeb = view.findViewById(R.id.progressBarWeb)

        // Initialize useful number buttons and textviews
        btnCalls = listOf(
            view.findViewById(R.id.btnCall1),
            view.findViewById(R.id.btnCall2),
            view.findViewById(R.id.btnCall3),
            view.findViewById(R.id.btnCall4),
            view.findViewById(R.id.btnCall5),
            view.findViewById(R.id.btnCall6),
            view.findViewById(R.id.btnCall7),
            view.findViewById(R.id.btnCall8),
            view.findViewById(R.id.btnCall9),
            view.findViewById(R.id.btnCall10),
            view.findViewById(R.id.btnCall11)
        )

        tvNumbers = listOf(
            view.findViewById(R.id.tvNumber1),
            view.findViewById(R.id.tvNumber2),
            view.findViewById(R.id.tvNumber3),
            view.findViewById(R.id.tvNumber4),
            view.findViewById(R.id.tvNumber5),
            view.findViewById(R.id.tvNumber6),
            view.findViewById(R.id.tvNumber7),
            view.findViewById(R.id.tvNumber8),
            view.findViewById(R.id.tvNumber9),
            view.findViewById(R.id.tvNumber10),
            view.findViewById(R.id.tvNumber11)
        )

        // Set click listeners for all buttons
        for (i in btnCalls.indices) {
            btnCalls[i].setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${tvNumbers[i].text}")
                startActivity(intent)
            }
        }

        // WebView setup
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBarWeb.visibility = View.GONE
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBarWeb.visibility = View.VISIBLE
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.visibility = View.GONE
        progressBarWeb.visibility = View.GONE

        // Adapter setup
        adapter = LinksAdapter(emptyList()) { url ->
            etSearch.visibility = View.GONE
            etWebsiteName.visibility = View.GONE
            etWebsiteUrl.visibility = View.GONE
            btnAddLink.visibility = View.GONE
            btnSelectPhoto.visibility = View.GONE
            rvLinks.visibility = View.GONE
            progressBarWeb.visibility = View.VISIBLE
            webView.visibility = View.VISIBLE
            webView.loadUrl(url)
        }

        rvLinks.layoutManager = LinearLayoutManager(requireContext())
        rvLinks.adapter = adapter

        // Select photo
        btnSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        // Add link
        btnAddLink.setOnClickListener { addLink() }

        // Search filter
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val filtered = allLinks.filter {
                    it.websiteName.contains(s.toString(), ignoreCase = true)
                }
                adapter.updateData(filtered)
            }
        })

        // Load links from Firestore
        loadLinks()

        // Handle back press for WebView
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
                webView.goBack()
            } else if (webView.visibility == View.VISIBLE) {
                webView.visibility = View.GONE
                progressBarWeb.visibility = View.GONE
                etSearch.visibility = View.VISIBLE
                etWebsiteName.visibility = View.VISIBLE
                etWebsiteUrl.visibility = View.VISIBLE
                btnAddLink.visibility = View.VISIBLE
                btnSelectPhoto.visibility = View.VISIBLE
                rvLinks.visibility = View.VISIBLE
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedPhotoUri = data?.data
        }
    }

    private fun addLink() {
        val websiteName = etWebsiteName.text.toString().trim()
        val url = etWebsiteUrl.text.toString().trim()
        if (websiteName.isEmpty() || url.isEmpty()) return

        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val firestore = FirebaseFirestore.getInstance()
        val storage = Firebase.storage.reference

        if (selectedPhotoUri != null) {
            val photoRef = storage.child("users/$userId/${System.currentTimeMillis()}.jpg")
            photoRef.putFile(selectedPhotoUri!!)
                .continueWithTask { photoRef.downloadUrl }
                .addOnSuccessListener { uri ->
                    saveLinkToFirestore(firestore, userId, websiteName, url, uri.toString())
                }
        } else {
            saveLinkToFirestore(firestore, userId, websiteName, url, null)
        }

        etWebsiteName.text.clear()
        etWebsiteUrl.text.clear()
        selectedPhotoUri = null
    }

    private fun saveLinkToFirestore(
        firestore: FirebaseFirestore,
        userId: String,
        websiteName: String,
        url: String,
        photoUrl: String?
    ) {
        val linkData = hashMapOf(
            "websiteName" to websiteName,
            "url" to url,
            "photoUrl" to photoUrl,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(userId)
            .collection("links")
            .add(linkData)
            .addOnSuccessListener { docRef ->
                Toast.makeText(requireContext(), "Link added", Toast.LENGTH_SHORT).show()
                allLinks.add(0, LinkModel(docRef.id, websiteName, url, photoUrl))
                adapter.updateData(allLinks)
            }
    }

    private fun loadLinks() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("links")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allLinks = snapshot.documents.map { doc ->
                        LinkModel(
                            id = doc.id,
                            websiteName = doc.getString("websiteName") ?: "",
                            url = doc.getString("url") ?: "",
                            photoUrl = doc.getString("photoUrl")
                        )
                    }.toMutableList()
                    adapter.updateData(allLinks)
                }
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            help().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
