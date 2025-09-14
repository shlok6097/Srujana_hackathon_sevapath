package com.example.templet1

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class AlphaGeeksChatBot {

    private val apiKey = "AIzaSyAx1oMN_MIrB91rXE07sMM4MD6IVDBN0tY"
    private val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)

    suspend fun sendMessage(userMessage: String): ChatMessage = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are Alpha Geeks ChatBot. 
                Always respond professionally and provide links/images when relevant.
            """.trimIndent()

            val lower = userMessage.lowercase()

            return@withContext when {
                lower.contains("form") -> {
                    val formUrl = searchFormUrlGoogle(userMessage) ?: "FORM_NOT_FOUND"
                    ChatMessage(
                        text = if (formUrl != "FORM_NOT_FOUND") "I found a form for you: $formUrl"
                        else "I could not find the form. Can you provide more details?",
                        isUser = false,
                        type = if (formUrl != "FORM_NOT_FOUND") MessageType.LINK else MessageType.TEXT
                    )
                }
                else -> {
                    val fullMessage = "$systemPrompt\nUser: $userMessage"
                    val response = model.generateContent(fullMessage)
                    ChatMessage(
                        text = response.text ?: "I couldn't understand that.",
                        isUser = false,
                        type = MessageType.TEXT
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext ChatMessage(
                text = "AlphaGeeks is currently unavailable.",
                isUser = false,
                type = MessageType.TEXT
            )
        }
    }

    private suspend fun searchFormUrlGoogle(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = "AIzaSyBaNvEX1VWri7w-DBqEAFb3vnhmSRpyh5k"
            val cseId = "c27103c5d2cfb4c6e"
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
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
