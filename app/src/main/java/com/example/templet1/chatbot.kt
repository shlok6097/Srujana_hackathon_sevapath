// In file: chatbot.kt
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.templet1.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class chatbot : Fragment() {

    private val DEBUG_TAG = "FormAssistantDebug"

    // --- View, Adapter, and Firebase Properties ---
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var userMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var formWebView: WebView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val bot = AlphaGeeksChatBot()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var userProfile: MutableMap<String, String>
    private var currentRequiredFields: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        userMessage = view.findViewById(R.id.userMessage)
        sendButton = view.findViewById(R.id.sendButton)
        formWebView = view.findViewById(R.id.formWebView)
        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter
        WebView.setWebContentsDebuggingEnabled(true)
        formWebView.settings.javaScriptEnabled = true
        formWebView.webViewClient = createWebViewClient()


        lifecycleScope.launch {
            Log.d(DEBUG_TAG, "Starting to load profile...")
            userProfile = ProfileManager.loadProfile(requireContext())
            Log.d(DEBUG_TAG, "--> Step 1: Profile loaded from local cache: $userProfile")
            if (userProfile["gender"].isNullOrBlank()) userProfile["gender"] = "Male"

            val firestoreProfile = getCurrentUserProfile()
            if (firestoreProfile.isNotEmpty()) {
                Log.d(DEBUG_TAG, "--> Step 2: Profile fetched from Firestore: $firestoreProfile")
                firestoreProfile.forEach { (key, value) -> userProfile[key] = value.toString() }
                ProfileManager.saveProfile(requireContext(), userProfile)
                Log.d(DEBUG_TAG, "--> Step 3: Merged and saved profile: $userProfile")
                addMessageToChat("Synced your profile from the cloud!", isUser = false)
            } else {
                Log.d(DEBUG_TAG, "--> Step 2: No profile found in Firestore. Using local cache.")
            }
        }

        sendButton.setOnClickListener {
            val text = userMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessageToChat(text, isUser = true)
                handleUserMessage(text)
                userMessage.text.clear()
            }

        }
        val micButton: ImageButton = view.findViewById(R.id.micButton)
        micButton.setOnClickListener {
            // Navigate using the action
            findNavController().navigate(R.id.action_chatbot_to_voiceFragment)
        }



        addMessageToChat(
            "Hello! Currently, our forms are under development as our dedicated team is working hard to make them fully functional. " +
                    "The following forms will soon be available:\n\n" +
                    "1. Login\n" +
                    "2. Registration Form 2\n" +
                    "3. Registration Form 3\n" +
                    "4. Registration Form 4\n" +
                    "5. Centralized Public Grievance Redress and Monitoring System (CPGRAMS)\n\n" +
                    "We appreciate your patience and understanding!",
            isUser = false
        )

        return view
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(DEBUG_TAG, "--> Step 5: WebView finished loading URL: $url")

                val scriptToRun: String?
                val delay: Long

                when {
                    url?.contains("herokuapp.com") == true -> {
                        scriptToRun = getHerokuLoginFillScript()
                        delay = 1500L
                    }
                    url?.contains("selenium.dev") == true -> {
                        scriptToRun = getSeleniumFormFillScript()
                        delay = 1500L
                    }
                    url?.contains("way2automation.com") == true -> {
                        scriptToRun = getWay2AutomationFillScript()
                        delay = 1500L
                    }
                    url?.contains("demoqa.com") == true -> {
                        scriptToRun = getComplexFormFillScript()
                        delay = 2000L
                    }
                    url?.contains("pgportal.gov.in/Registration") == true -> {
                        scriptToRun = getCPGRAMSFillScript()
                        delay = 2000L  // still keep a small delay before injection, the JS waitFor will handle the rest
                    }

                    url?.contains("india.gov.in") == true -> {
                        scriptToRun = getGovIndiaFillScript()
                        delay = 3000L // Longer delay for the slower government site
                    }
                    else -> {
                        scriptToRun = null
                        delay = 0L
                    }
                }

                if (scriptToRun != null) {
                    Log.d(DEBUG_TAG, "--> Step 6: A matching form was found. Attempting to fill...")
                    injectJavaScript(scriptToRun, delay)
                } else {
                    Log.w(DEBUG_TAG, "--> Step 6: No matching form script found for this URL.")
                }

                val missingKeys = currentRequiredFields.filter { userProfile[it].isNullOrBlank() }
                Log.d(DEBUG_TAG, "--> Step 7: Checking for missing required data. Missing keys: $missingKeys")

                if (missingKeys.isNotEmpty()) {
                    Log.d(DEBUG_TAG, "--> Step 8: Data is missing. Showing pop-up dialog...")
                    showMissingDataDialog(missingKeys) { newData ->
                        Log.d(DEBUG_TAG, "--> Step 9: Pop-up submitted with data: $newData")
                        newData.forEach { (key, value) -> userProfile[key] = value }
                        ProfileManager.saveProfile(requireContext(), userProfile)

                        scriptToRun?.let {
                            Log.d(DEBUG_TAG, "--> Step 10: Re-injecting script with complete data.")
                            injectJavaScript(it, delay)
                        }
                        addMessageToChat("Information updated. Re-filling form...", isUser = false)
                    }
                } else {
                    Log.d(DEBUG_TAG, "--> Step 8: No data missing. Form-fill process complete.")
                }
            }
        }
    }

    private fun handleUserMessage(userText: String) {
        Log.d(DEBUG_TAG, "--> Step 4: Handling user message: '$userText'")
        val lowerCaseText = userText.lowercase()
        when {
            lowerCaseText.contains("form 1") -> startForm1_LoginDemo()
            lowerCaseText.contains("form 2") -> startForm2_ContactDemo()
            lowerCaseText.contains("form 3") -> startForm3_RegistrationDemo()
            lowerCaseText.contains("form 4") -> startForm4_ComplexDemo()
            lowerCaseText.contains("gov form") -> startForm5_GovDemo()
            lowerCaseText.contains("cpgrams") || lowerCaseText.contains("grievance") || lowerCaseText.contains("pgportal") ->
                startFormGov_CPGRAMS()
            else -> {
                lifecycleScope.launch {
                    val response = bot.sendMessage(userText)
                    addMessageToChat(response.text, isUser = false, type = response.type)
                }
            }
        }
    }

    private fun showMissingDataDialog(missingKeys: List<String>, onComplete: (Map<String, String>) -> Unit) {
        if (!isAdded) return
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Additional Information Required")
        builder.setCancelable(false)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val editTextMap = mutableMapOf<String, EditText>()
        missingKeys.forEach { key ->
            val label = TextView(context).apply { text = key.replaceFirstChar { it.uppercase() } }
            val editText = EditText(context).apply { hint = "Enter your ${key.lowercase()}" }
            layout.addView(label)
            layout.addView(editText)
            editTextMap[key] = editText
        }

        builder.setView(layout)
        builder.setPositiveButton("Submit") { _, _ ->
            val enteredData = editTextMap.mapValues { it.value.text.toString().trim() }
            onComplete(enteredData)
        }
        builder.show()
    }
    private fun startFormGov_CPGRAMS() {
        addMessageToChat("Opening CPGRAMS grievance/registration form...", isUser = false)

        // Add all the required fields here
        currentRequiredFields = listOf(
            "name",
            "address1",
            "address2",
            "address3",
            "pincode",
            "mobileNo",
            "emailAddress",
            "comments"
        )

        showWebView()
        formWebView.loadUrl("https://pgportal.gov.in/Registration")
    }




    // --- Specific JavaScript generating functions ---

    private fun getHerokuLoginFillScript(): String {
        return """
        (function() {
            document.querySelector('#username').value = 'tomsmith';
            document.querySelector('#password').value = 'SuperSecretPassword!';
        })();
        """
    }

    private fun getSeleniumFormFillScript(): String {
        return """
        (function() {
            document.querySelector('#my-text-id').value = '${userProfile["name"]}';
            document.querySelector('#my-password').value = 'password123';
            document.querySelector('#my-textarea').value = 'This is an automated message.';
        })();
        """
    }

    private fun getWay2AutomationFillScript(): String {
        return """
        (function() {
            document.querySelector('input[name="name"]').value = '${userProfile["name"]}';
            document.querySelector('input[name="phone"]').value = '${userProfile["phone"]}';
            document.querySelector('input[name="email"]').value = '${userProfile["email"]}';
        })();
        """
    }

    private fun getComplexFormFillScript(): String {
        return """
    (function() {
        var firstName = document.querySelector('#firstName');
        if (firstName) firstName.value = '${userProfile["firstName"] ?: ""}';

        var lastName = document.querySelector('#lastName');
        if (lastName) lastName.value = '${userProfile["lastName"] ?: ""}';

        var email = document.querySelector('#userEmail');
        if (email) email.value = '${userProfile["email"] ?: ""}';

        var phone = document.querySelector('#userNumber');
        if (phone) phone.value = '${userProfile["phone"] ?: ""}';

        var gender = ('${userProfile["gender"] ?: ""}').toLowerCase();
        if (gender === 'male') {
            var male = document.querySelector('label[for="gender-radio-1"]');
            if (male) male.click();
        } else if (gender === 'female') {
            var female = document.querySelector('label[for="gender-radio-2"]');
            if (female) female.click();
        } else if (gender) {
            var other = document.querySelector('label[for="gender-radio-3"]');
            if (other) other.click();
        }

        var hobby = document.querySelector('label[for="hobbies-checkbox-1"] input');
        if (hobby && !hobby.checked) {
            document.querySelector('label[for="hobbies-checkbox-1"]').click();
        }
    })();
    """
    }


    private fun getGovIndiaFillScript(): String {
        val profileJson = JSONObject(userProfile as Map<*, *>).toString() // convert to JSON string

        return """
    (function waitForIframe(tries) {
        tries = tries || 0;
        var iframe = document.querySelector('iframe');
        if (!iframe) {
            if (tries < 20) setTimeout(function(){ waitForIframe(tries+1); }, 500);
            return;
        }
        var innerDoc = iframe.contentDocument || iframe.contentWindow.document;
        if (!innerDoc) {
            if (tries < 20) setTimeout(function(){ waitForIframe(tries+1); }, 500);
            return;
        }

        // Load full profile as JSON
        var profile = $profileJson;

        function ensureValue(key, label) {
            var val = profile[key];
            if (!val || val.trim() === "") {
                val = prompt("Please enter your " + label + ":");
                profile[key] = val; // store back into object for reuse
            }
            return val || "";
        }

        var nameField     = innerDoc.getElementById('edit-name');
        var mailField     = innerDoc.getElementById('edit-mail');
        var commentsField = innerDoc.getElementById('edit-comments');

        if (nameField) nameField.value = ensureValue("name", "Full Name");
        if (mailField) mailField.value = ensureValue("email", "Email Address");
        if (commentsField) commentsField.value = ensureValue("comments", "Comments");

        // Extended fields
        var address1Field = innerDoc.getElementById('edit-address1');
        var address2Field = innerDoc.getElementById('edit-address2');
        var address3Field = innerDoc.getElementById('edit-address3');
        var pincodeField  = innerDoc.getElementById('edit-pincode');
        var mobileField   = innerDoc.getElementById('edit-mobile');
        var emailField    = innerDoc.getElementById('edit-email');

        if (address1Field) address1Field.value = ensureValue("address1", "Address Line 1");
        if (address2Field) address2Field.value = ensureValue("address2", "Address Line 2");
        if (address3Field) address3Field.value = ensureValue("address3", "Address Line 3");
        if (pincodeField)  pincodeField.value  = ensureValue("pincode", "Pincode");
        if (mobileField)   mobileField.value   = ensureValue("mobileNo", "Mobile Number");
        if (emailField)    emailField.value    = ensureValue("emailAddress", "Email Address");
    })();
    """
    }




    private fun injectJavaScript(script: String, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            formWebView.evaluateJavascript(script, null)
        }, delay)
    }

    // --- Form Trigger Functions ---

    private fun startForm1_LoginDemo() {
        addMessageToChat("Opening a simple login form...", isUser = false)
        currentRequiredFields = listOf()
        showWebView()
        formWebView.loadUrl("https://the-internet.herokuapp.com/login")
    }

    private fun startForm2_ContactDemo() {
        addMessageToChat("Opening a standard contact form...", isUser = false)
        currentRequiredFields = listOf("name")
        showWebView()
        formWebView.loadUrl("https://www.selenium.dev/selenium/web/web-form.html")
    }

    private fun startForm3_RegistrationDemo() {
        addMessageToChat("Opening a simple registration form...", isUser = false)
        currentRequiredFields = listOf("name", "phone", "email")
        showWebView()
        formWebView.loadUrl("https://www.way2automation.com/way2auto_jquery/registration.php")
    }

    private fun startForm4_ComplexDemo() {
        addMessageToChat("Opening a complex practice form...", isUser = false)
        currentRequiredFields = listOf("firstName", "lastName", "email", "gender", "phone")
        showWebView()
        formWebView.loadUrl("https://demoqa.com/automation-practice-form")
    }

    private fun startForm5_GovDemo() {
        addMessageToChat("Attempting to fill the National Portal of India contact form...", isUser = false)
        currentRequiredFields = listOf("name", "email")
        showWebView()
        formWebView.loadUrl("https://www.india.gov.in/contact-us")
    }



    private fun getCPGRAMSFillScript(): String {
        val profileJson = JSONObject(userProfile as Map<*, *>).toString()
        return """
    (function waitFor(tries) {
        tries = tries || 0;
        var profile = $profileJson;

        function ensureValue(key, label) {
            var val = profile[key];
            if (!val || val.trim() === "") {
                val = prompt("Please enter your " + label + ":");
                profile[key] = val;
            }
            return val || "";
        }

        function setValue(selector, key, label) {
            var el = document.querySelector(selector);
            if (el) el.value = ensureValue(key, label);
        }

        setValue('input[name="Name"], input[id*=name]', "name", "Full Name");
        setValue('input[type="email"], input[id*=email]', "emailAddress", "Email Address");
        setValue('input[type="tel"], input[id*=phone]', "mobileNo", "Mobile Number");
        setValue('textarea[id*=address1]', "address1", "Address Line 1");
        setValue('textarea[id*=address2]', "address2", "Address Line 2");
        setValue('textarea[id*=address3]', "address3", "Address Line 3");
        setValue('input[id*=pincode]', "pincode", "Pincode");
        setValue('textarea[id*=comments]', "comments", "Grievance Details");

        var captcha = document.querySelector('input[id*="captcha"], input[name*="captcha"]');
        if (captcha) captcha.focus();
    })();
    """
    }



    // --- Utility Functions ---
    private suspend fun getCurrentUserProfile(): Map<String, Any> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyMap()
        try {
            db.collection("users").document(userId).get().await().data ?: emptyMap()
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching user profile", e); emptyMap()
        }
    }

    private fun addMessageToChat(text: String, isUser: Boolean, type: MessageType = MessageType.TEXT) {
        val message = ChatMessage(text, isUser, type = type)
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