package com.example.templet1

import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

suspend fun searchFormUrlGoogle(query: String): String? {
    val apiKey = "AIzaSyBaNvEX1VWri7w-DBqEAFb3vnhmSRpyh5k"
    val cseId = "c27103c5d2cfb4c6e"

    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url =
        "https://www.googleapis.com/customsearch/v1?q=$encodedQuery&key=$apiKey&cx=$cseId"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    val body = response.body?.string() ?: return null

    val json = JSONObject(body)
    val items = json.optJSONArray("items") ?: return null
    if (items.length() > 0) {
        return items.getJSONObject(0).optString("link") // first result link
    }
    return null
}
