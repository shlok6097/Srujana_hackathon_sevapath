package com.example.templet1

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlphaGeeksChatBot {


    private val apiKey = "AIzaSyAx1oMN_MIrB91rXE07sMM4MD6IVDBN0tY"


    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

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
            else -> {
                // Default AI-generated reply
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
