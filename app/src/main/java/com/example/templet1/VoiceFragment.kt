package com.example.templet1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.*
import java.util.Locale

class VoiceFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var transcriptionText: TextView
    private lateinit var voiceSpinner: Spinner

    private var textToSpeech: TextToSpeech? = null
    private val REQUEST_CODE_SPEECH_INPUT = 100

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyCum_dlz2p2TeNSwcutU00mn85p77LzWSA"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_voice, container, false)

        micButton = view.findViewById(R.id.mic_button)
        progressBar = view.findViewById(R.id.thinking_progress_bar)
        transcriptionText = view.findViewById(R.id.partial_transcription_text)
        voiceSpinner = view.findViewById(R.id.voiceSpinner)

        setupTextToSpeech()
        setupVoiceSelector()

        micButton.setOnClickListener { startSpeechInput() }

        return view
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()

                // Populate voice spinner
                val voiceList = textToSpeech?.voices?.map { it.name }?.sorted() ?: listOf()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, voiceList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                voiceSpinner.adapter = adapter

                // Select default voice if available
                val defaultIndex = voiceList.indexOfFirst { it.contains("x-tpd-network") }
                if (defaultIndex >= 0) voiceSpinner.setSelection(defaultIndex)
            } else {
                Log.e("TTS", "TextToSpeech initialization failed")
            }
        }
    }

    private fun setupVoiceSelector() {
        voiceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedVoiceName = parent.getItemAtPosition(position) as String
                val selectedVoice = textToSpeech?.voices?.firstOrNull { it.name == selectedVoiceName }
                selectedVoice?.let {
                    textToSpeech?.voice = it
                    // Demonstrate selected voice
                    textToSpeech?.speak("This is my voice!", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun startSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        try { startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT) }
        catch (e: Exception) { Toast.makeText(requireContext(), "Speech input not supported", Toast.LENGTH_SHORT).show() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            transcriptionText.text = spokenText
            callGemini(spokenText)
        }
    }

    private fun callGemini(userPrompt: String) {
        progressBar.visibility = View.VISIBLE
        micButton.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val prompt = """
                You are Jarvis, a witty, funny, and charming assistant created by Shlok.
                Always respond in a humorous or playful tone.
                Never be boring, never mention Gemini or AI.
                
                User says: $userPrompt
            """.trimIndent()

                val response = generativeModel.generateContent(content { text(prompt) })
                var reply = response.text ?: "Oops! I forgot my jokes!"

                // Replace any remaining "Gemini" with "Jarvis"
                reply = reply.replace("Gemini", "Jarvis", ignoreCase = true)

                withContext(Dispatchers.Main) {
                    transcriptionText.text = reply
                    textToSpeech?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                    progressBar.visibility = View.GONE
                    micButton.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    micButton.isEnabled = true
                    transcriptionText.text = "Error: Try again"
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
