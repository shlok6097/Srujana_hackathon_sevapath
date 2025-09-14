package com.example.templet1

import retrofit2.http.GET

interface ApiService {
    @GET("updates") // 👈 replace with your real endpoint
    suspend fun getUpdates(): List<GovernmentUpdate>
}
