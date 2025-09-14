package com.example.formassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.templet1.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class chatbot : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var userMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var formWebView: WebView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private val db = Firebase.firestore
    private val studentId = "student123"
    private val bot = AlphaGeeksChatBot()

    private var pendingFormData: MutableMap<String, String> = mutableMapOf()
    private var waitingForField: String? = null

    // Google CSE
    private val googleApiKey = "AIzaSyBaNvEX1VWri7w-DBqEAFb3vnhmSRpyh5k"
    private val googleCseId = "c27103c5d2cfb4c6e"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        userMessage = view.findViewById(R.id.userMessage)
        sendButton = view.findViewById(R.id.sendButton)
        formWebView = view.findViewById(R.id.formWebView)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter

        formWebView.settings.javaScriptEnabled = true
        formWebView.settings.domStorageEnabled = true
        formWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                extractFormFields(formWebView) { fields ->
                    lifecycleScope.launch {
                        val profile = getStudentProfileFromFirestore()
                        val formData = bot.generateFormMapping(fields, profile)
                        handleFormData(formData)
                    }
                }
            }
        }

        sendButton.setOnClickListener {
            val text = userMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, isUser = true)
                handleUserMessage(text)
                userMessage.text.clear()
            }
        }

        return view
    }

    private fun addMessage(text: String, isUser: Boolean, type: MessageType = MessageType.TEXT) {
        val msg = ChatMessage(text, isUser, type = type)
        messages.add(msg)
        chatAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun handleUserMessage(userText: String) {
        // Handle missing fields
        waitingForField?.let { field ->
            pendingFormData[field] = userText
            saveToFirestore(field, userText)
            waitingForField = null
            checkIfFormComplete()
            return
        }

        lifecycleScope.launch {
            when {
                userText.contains("fill", true) && userText.contains("form", true) -> {
                    // 🔹 Dynamic form URL search via Google CSE
                    val formUrl = searchFormUrlGoogle(userText)
                    if (!formUrl.isNullOrBlank()) {
                        startFormAssistant(formUrl)
                    } else {
                        addMessage(
                            "I couldn't find a form for \"$userText\". Can you specify the form name?",
                            isUser = false
                        )
                    }
                }
                userText.contains("submit", true) -> submitForm()
                else -> {
                    val response = bot.sendMessage(userText)
                    addMessage(response.text ?: "No response", isUser = false, type = response.type)
                }
            }
        }
    }

    private fun startFormAssistant(url: String) {
        // Make WebView full screen
        formWebView.visibility = View.VISIBLE
        chatRecyclerView.visibility = View.GONE
        formWebView.loadUrl(url)
        addMessage("Opening the form and checking required fields...", isUser = false)
    }

    private fun extractFormFields(webView: WebView, onFieldsExtracted: (List<String>) -> Unit) {
        webView.evaluateJavascript(
            """
            (function() {
                var fields = [];
                document.querySelectorAll("input, select, textarea").forEach(e => {
                    if(e.name) fields.push(e.name);
                    else if(e.id) fields.push(e.id);
                });
                return JSON.stringify(fields);
            })();
            """
        ) { result ->
            if (!result.isNullOrBlank() && result != "null") {
                try {
                    val clean = result.trim().removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"")
                    val arr = JSONArray(clean)
                    val fieldList = List(arr.length()) { arr.getString(it) }
                    onFieldsExtracted(fieldList)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun handleFormData(formData: JSONObject) {
        pendingFormData.clear()
        formData.keys().forEach { key -> pendingFormData[key] = formData.getString(key) }
        checkIfFormComplete()
    }

    private fun checkIfFormComplete() {
        val missing = pendingFormData.filter { it.value == "MISSING" }
        if (missing.isNotEmpty()) {
            val field = missing.keys.first()
            waitingForField = field
            addMessage("I need your $field. Please provide it.", isUser = false)
        } else {
            addMessage("All fields are ready. Filling the form now...", isUser = false)
            autoFillForm(pendingFormData)
        }
    }

    private fun autoFillForm(formData: Map<String, String>) {
        formData.forEach { (field, value) ->
            formWebView.evaluateJavascript(
                "var el=document.getElementsByName('$field')[0];if(el)el.value='$value';",
                null
            )
        }
        addMessage("Form filled. Say 'submit' if you want me to submit it.", isUser = false)
    }

    private fun submitForm() {
        formWebView.evaluateJavascript(
            "var btn=document.querySelector('input[type=submit], button[type=submit]');if(btn)btn.click();",
            null
        )
        addMessage("I’ve submitted the form ✅", isUser = false)
    }

    private suspend fun getStudentProfileFromFirestore(): Map<String, String> {
        val snapshot = db.collection("students").document(studentId).get().await()
        return snapshot.data as? Map<String, String> ?: emptyMap()
    }

    private fun saveToFirestore(field: String, value: String) {
        db.collection("students").document(studentId).update(mapOf(field to value))
    }

    /** 🔹 Google CSE search */
    private suspend fun searchFormUrlGoogle(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/customsearch/v1?q=$encodedQuery&key=$googleApiKey&cx=$googleCseId"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext null
            if (items.length() > 0) return@withContext items.getJSONObject(0).optString("link")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
