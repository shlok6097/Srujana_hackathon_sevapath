package com.example.templet1

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class AlphaGeeksChatBot {

    private val apiKey = "AIzaSyAx1oMN_MIrB91rXE07sMM4MD6IVDBN0tY"
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    // 🔹 Send user message to bot
    suspend fun sendMessage(userMessage: String): ChatMessage = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are Alpha Geeks ChatBot. 
            Never mention Google or Gemini. 
            Always respond as Alpha Geeks with a professional, citizen-friendly style.
            Answer questions about fund spending, grievances, Trust Scores, and provide links/images when appropriate.
        """.trimIndent()

        val lower = userMessage.lowercase()

        return@withContext when {
            lower.contains("report") || lower.contains("complaint") -> generateTicket(userMessage)
            lower.contains("fund") || lower.contains("spend") -> fetchFundInfo(userMessage)
            lower.contains("trust score") -> fetchTrustScore(userMessage)
            lower.contains("infographic") -> fetchInfographic(userMessage)
            lower.contains("report link") -> fetchReportLink(userMessage)
            lower.contains("form") || lower.contains("scholarship") -> {
                // 🔹 Dynamically find form URL
                val formUrl = searchFormUrlGoogle(userMessage) ?: "FORM_NOT_FOUND"
                ChatMessage(
                    text = if (formUrl != "FORM_NOT_FOUND") {
                        "I found a form for you: $formUrl. Opening assistant..."
                    } else {
                        "I couldn't find the form. Can you provide more details?"
                    },
                    isUser = false,
                    type = MessageType.LINK
                )
            }
            else -> {
                val fullMessage = "$systemPrompt\nUser: $userMessage"
                val response = model.generateContent(fullMessage)
                ChatMessage(
                    text = response.text ?: "Alpha Geeks could not understand that.",
                    isUser = false,
                    type = MessageType.TEXT
                )
            }
        }
    }

    
    suspend fun generateFormMapping(fields: List<String>, profile: Map<String, String>): JSONObject {
        val prompt = """
        You are filling an online form. 
        Fields: ${fields.joinToString(", ")}.
        Profile: $profile.
        Output valid JSON mapping of fieldName → value.
        If value is missing in profile, use "MISSING".
    """.trimIndent()

        val response = model.generateContent(prompt)
        var text = response.text ?: "{}"

        // Remove any triple backticks or code blocks
        text = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            JSONObject(text)
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject() // fallback empty JSON to prevent crash
        }
    }


    // 🔹 Google Custom Search API to find dynamic form URLs
    private suspend fun searchFormUrlGoogle(query: String): String? = withContext(Dispatchers.IO) {
        val apiKey = "YOUR_GOOGLE_API_KEY" // replace
        val cseId = "YOUR_CSE_ID"          // replace

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/customsearch/v1?q=$encodedQuery&key=$apiKey&cx=$cseId"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null

        val json = JSONObject(body)
        val items = json.optJSONArray("items") ?: return@withContext null
        if (items.length() > 0) {
            return@withContext items.getJSONObject(0).optString("link")
        }
        null
    }

    // ----- Existing helper methods -----
    private fun generateTicket(description: String): ChatMessage {
        val ticketId = "AG-${System.currentTimeMillis()}"
        return ChatMessage(
            text = "Your complaint has been registered with ticket ID: $ticketId. Status: Pending.",
            isUser = false,
            type = MessageType.TEXT
        )
    }

    private fun fetchFundInfo(query: String): ChatMessage {
        val reportUrl = "https://alpha-geeks.org/reports/fund-123"
        return ChatMessage(
            text = "According to Alpha Geeks records, 15 lakh INR was spent on school repairs in your district. " +
                    "View full report: $reportUrl",
            isUser = false,
            type = MessageType.LINK
        )
    }

    private fun fetchTrustScore(query: String): ChatMessage {
        val infographicUrl = "https://alpha-geeks.org/images/trustscore.png"
        return ChatMessage(
            text = "XYZ Institute has the highest Trust Score: ★★★★★ (98/100). Tap to view infographic: $infographicUrl",
            isUser = false,
            type = MessageType.INFOGRAPHIC
        )
    }

    private fun fetchInfographic(query: String): ChatMessage {
        val imageUrl = "https://alpha-geeks.org/images/infographic123.png"
        return ChatMessage(
            text = imageUrl,
            isUser = false,
            type = MessageType.IMAGE
        )
    }

    private fun fetchReportLink(query: String): ChatMessage {
        val reportUrl = "https://alpha-geeks.org/reports/district-456"
        return ChatMessage(
            text = reportUrl,
            isUser = false,
            type = MessageType.LINK
        )
    }

    fun translateToLanguage(message: String, languageCode: String): String {
        return "[$languageCode] $message"
    }
}
