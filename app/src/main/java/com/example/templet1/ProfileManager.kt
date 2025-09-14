package com.example.templet1




import android.content.Context
import androidx.core.content.edit



object ProfileManager {
    private const val PREFS_NAME = "UserProfilePrefs"
    // UPDATED: Add new keys for the complex form
    private val ALL_KEYS = listOf("name", "firstName", "lastName", "email", "phone", "address", "gender", "country")

    fun saveProfile(context: Context, profile: Map<String, String>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit {
            ALL_KEYS.forEach { key ->
                putString(key, profile[key])
            }
        }
    }

    fun loadProfile(context: Context): MutableMap<String, String> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profile = mutableMapOf<String, String>()
        ALL_KEYS.forEach { key ->
            profile[key] = sharedPrefs.getString(key, "") ?: ""
        }
        return profile
    }
}