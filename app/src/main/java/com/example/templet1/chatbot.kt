package com.example.formassistant

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import kotlinx.coroutines.launch

class chatbot : Fragment() {

    // --- View and Adapter Properties ---
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var userMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var formWebView: WebView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val bot = AlphaGeeksChatBot()

    // --- User Data and State Management ---
    private val userProfile = mutableMapOf(
        "name" to "Alex Doe",
        "email" to "alex.doe@example.com",
        // Phone is intentionally left blank to trigger the bot's question
        "phone" to "",
        "address" to "123 Tech Park, Bangalore"
    )

    private var awaitingAnswerFor: String? = null
    private var onDataCollected: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        // --- Standard View Initialization ---
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        userMessage = view.findViewById(R.id.userMessage)
        sendButton = view.findViewById(R.id.sendButton)
        formWebView = view.findViewById(R.id.formWebView)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter

        // IMPORTANT: Enable WebView debugging for easy troubleshooting
        WebView.setWebContentsDebuggingEnabled(true)

        formWebView.settings.javaScriptEnabled = true
        formWebView.settings.domStorageEnabled = true
        formWebView.webViewClient = WebViewClient()

        sendButton.setOnClickListener {
            val text = userMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessageToChat(text, isUser = true)
                handleUserMessage(text)
                userMessage.text.clear()
            }
        }

        addMessageToChat("Hi! I am the Alpha Geeks assistant. You can ask me to fill a form. Try 'fill form 1'.", isUser = false)
        return view
    }

    private fun handleUserMessage(userText: String) {
        // --- State 1: Awaiting an answer from the user ---
        if (awaitingAnswerFor != null) {
            val key = awaitingAnswerFor!!
            addMessageToChat("Thanks! I've updated your $key.", isUser = false)

            userProfile[key] = userText // Save the answer
            awaitingAnswerFor = null // Reset state

            // Resume the paused action (the form filling)
            onDataCollected?.invoke()
            return
        }

        // --- State 2: Processing a new command ---
        val lowerCaseText = userText.lowercase()
        when {
            lowerCaseText.contains("form 1") || lowerCaseText.contains("chanakya") -> {
                startForm1_Chanakya()
            }
            lowerCaseText.contains("form 2") || lowerCaseText.contains("google") -> {
                startForm2_GoogleDemo()
            }
            lowerCaseText.contains("form 3") || lowerCaseText.contains("contact") -> {
                startForm3_Example()
            }
            else -> {
                // Default to Gemini for general chat
                lifecycleScope.launch {
                    val response = bot.sendMessage(userText)
                    addMessageToChat(response.text, isUser = false, type = response.type)
                }
            }
        }
    }

    // --- Core Logic for Data Collection ---
    private fun collectMissingData(requiredKeys: List<String>, formName: String, action: () -> Unit) {
        val missingKey = requiredKeys.firstOrNull { userProfile[it].isNullOrBlank() }

        if (missingKey != null) {
            // Data is missing, so we ask the user for it
            awaitingAnswerFor = missingKey
            onDataCollected = { collectMissingData(requiredKeys, formName, action) }
            addMessageToChat("To fill the $formName, I need your $missingKey. What is it?", isUser = false)
        } else {
            // All required data is present, so we can run the action
            action()
        }
    }

    // --- Demo Form 1: Chanakya University ---
    private fun startForm1_Chanakya() {
        val requiredFields = listOf("name", "email", "phone")
        collectMissingData(requiredFields, formName = "Chanakya form") {
            // This block runs only when all data is available
            addMessageToChat("Great, I have all the info. Opening and filling the Chanakya form...", isUser = false)
            showWebView()
            formWebView.loadUrl("https://admissions.chanakyauniversity.in/ug-application")

            val jsToInject = """
            (function() {
                document.querySelector('input[name="student_name"]').value = '${userProfile["name"]}';
                document.querySelector('input[name="student_email"]').value = '${userProfile["email"]}';
                document.querySelector('input[name="student_mobile"]').value = '${userProfile["phone"]}';
                console.log('Chanakya form filled!');
            })();
            """
            // Delay injection to allow the page to load
            Handler(Looper.getMainLooper()).postDelayed({
                formWebView.evaluateJavascript(jsToInject) { result -> Log.d("WebView", "JS Result: $result") }
            }, 2500)
        }
    }

    // --- Demo Form 2: Google Form ---
    private fun startForm2_GoogleDemo() {
        val requiredFields = listOf("name", "email", "address")
        collectMissingData(requiredFields, formName = "Google Form") {
            addMessageToChat("Got it. Opening and filling the Google Form demo...", isUser = false)
            showWebView()
            formWebView.loadUrl("https://forms.gle/YaX1LpH7EudR3hA78")

            val jsToInject = """
            (function() {
                document.querySelector('input[aria-label="Your Name"]').value = '${userProfile["name"]}';
                document.querySelector('input[aria-label="Email Address"]').value = '${userProfile["email"]}';
                document.querySelector('textarea[aria-label="Your Address"]').value = '${userProfile["address"]}';
                console.log('Google Form filled!');
            })();
            """
            Handler(Looper.getMainLooper()).postDelayed({
                formWebView.evaluateJavascript(jsToInject) { result -> Log.d("WebView", "JS Result: $result") }
            }, 2500)
        }
    }

    // --- Demo Form 3: Generic Contact Form ---
    private fun startForm3_Example() {
        val requiredFields = listOf("name", "email")
        collectMissingData(requiredFields, formName = "Contact Form") {
            addMessageToChat("Alright, opening and filling the sample contact form...", isUser = false)
            showWebView()
            formWebView.loadUrl("https://www.jotform.com/form-templates/classic-contact-form")

            val jsToInject = """
            (function() {
                document.querySelector('#first_3').value = '${userProfile["name"]?.split(" ")?.firstOrNull() ?: ""}';
                document.querySelector('#last_3').value = '${userProfile["name"]?.split(" ")?.getOrNull(1) ?: ""}';
                document.querySelector('#input_4').value = '${userProfile["email"]}';
                console.log('Example form filled!');
            })();
            """
            Handler(Looper.getMainLooper()).postDelayed({
                formWebView.evaluateJavascript(jsToInject) { result -> Log.d("WebView", "JS Result: $result") }
            }, 2500)
        }
    }

    // --- Utility Functions ---
    private fun addMessageToChat(text: String, isUser: Boolean, type: MessageType = MessageType.TEXT) {
        val message = ChatMessage(text, isUser, type)
        activity?.runOnUiThread {
            messages.add(message)
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.scrollToPosition(messages.size - 1)
        }
    }


    private fun showWebView() {
        formWebView.visibility = View.VISIBLE
        chatRecyclerView.visibility = View.GONE
    }
}