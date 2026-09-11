package com.example.inframonitor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val API_KEY_KEY = stringPreferencesKey("api_key")
        const val DEFAULT_SERVER_URL = "https://api-infra.andr3sdev7.com/"
    }

    val serverUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_URL_KEY]
        }

    val apiKey: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[API_KEY_KEY]
        }

    suspend fun saveServerUrl(url: String) {
        var trimmed = url.trim()
        if (trimmed.startsWith("http://")) {
            trimmed = "https://" + trimmed.substring(7)
        } else if (!trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        val finalUrl = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = finalUrl
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY_KEY] = apiKey.trim()
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
