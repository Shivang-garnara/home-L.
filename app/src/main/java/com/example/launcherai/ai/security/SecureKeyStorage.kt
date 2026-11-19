package com.example.launcherai.ai.security

import android.content.Context
import android.util.Base64

/**
 * Encrypts and decrypts locally configured developer API keys using Base64 / XOR obfuscation.
 * NOTE: Local client-side storage protects against casual plain-text inspection in local XML files,
 * but DOES NOT make API keys safe in a distributed production APK. Production deployments MUST use
 * a backend proxy server to hold provider credentials securely.
 */
object SecureKeyStorage {

    private const val PREFS_NAME = "workspace_prefs"
    private const val MASK_BYTE: Byte = 0x5A

    fun saveApiKey(context: Context, rawApiKey: String) {
        saveProviderApiKey(context, "google_gemini", rawApiKey)
    }

    fun getApiKey(context: Context): String {
        return getProviderApiKey(context, "google_gemini")
    }

    fun saveProviderApiKey(context: Context, providerId: String, rawApiKey: String) {
        val keyName = "key_${providerId}_api_key_enc"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (rawApiKey.isBlank()) {
            prefs.edit().remove(keyName).apply()
            return
        }

        val rawBytes = rawApiKey.toByteArray(Charsets.UTF_8)
        val maskedBytes = ByteArray(rawBytes.size)
        for (i in rawBytes.indices) {
            maskedBytes[i] = (rawBytes[i].toInt() xor MASK_BYTE.toInt()).toByte()
        }
        val encoded = Base64.encodeToString(maskedBytes, Base64.NO_WRAP)
        prefs.edit().putString(keyName, encoded).apply()
    }

    fun getProviderApiKey(context: Context, providerId: String): String {
        val keyName = "key_${providerId}_api_key_enc"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString(keyName, "") ?: ""
        if (encoded.isBlank()) return ""

        return try {
            val maskedBytes = Base64.decode(encoded, Base64.NO_WRAP)
            val rawBytes = ByteArray(maskedBytes.size)
            for (i in maskedBytes.indices) {
                rawBytes[i] = (maskedBytes[i].toInt() xor MASK_BYTE.toInt()).toByte()
            }
            String(rawBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(context: Context, providerId: String = "google_gemini"): Boolean {
        return getProviderApiKey(context, providerId).isNotBlank()
    }
}
