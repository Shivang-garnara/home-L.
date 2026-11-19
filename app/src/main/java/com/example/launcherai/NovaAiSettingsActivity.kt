package com.example.launcherai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.ai.execution.NovaAiEngine
import com.example.launcherai.ai.provider.ClaudeAiProvider
import com.example.launcherai.ai.provider.ClaudeModels
import com.example.launcherai.ai.provider.GeminiAiProvider
import com.example.launcherai.ai.provider.GeminiModels
import com.example.launcherai.ai.provider.LocalAgentProvider
import com.example.launcherai.ai.provider.OnDeviceLlmProvider
import com.example.launcherai.ai.provider.OpenAiProvider
import com.example.launcherai.ai.provider.OpenAiModels
import com.example.launcherai.ai.security.SecureKeyStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NovaAiSettingsActivity : AppCompatActivity() {

    private var activeModelIndex = 0
    private val modelChoices = arrayOf(
        "Local On-Device Launcher Agent (Offline Rule Default)",
        "Google Gemini 2.5 Flash (Stable Production Default)",
        "Google Gemini 2.5 Pro (Advanced Reasoning Agent)",
        "Google Gemini 2.0 Flash (Stable Production)",
        "OpenAI GPT-4o (Multimodal Production Agent)",
        "OpenAI GPT-4o-mini (Fast Low-Cost Agent)",
        "Anthropic Claude 3.7 Sonnet (Latest Flagship Agent)",
        "Anthropic Claude 3.5 Haiku (Fast Low-Latency Agent)",
        "On-Device LiteRT-LM (Gemma-2B 4-bit Quantized)"
    )

    private val geminiProvider = GeminiAiProvider()
    private val openAiProvider = OpenAiProvider()
    private val claudeProvider = ClaudeAiProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_settings)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        updateStatusUI()
        setupListeners()
    }

    private fun updateStatusUI() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val savedModelIndex = prefs.getInt("key_active_model_index", 0)
        activeModelIndex = savedModelIndex

        val tvActiveModel = findViewById<TextView>(R.id.tvActiveModel)
        val tvApiKeyStatus = findViewById<TextView>(R.id.tvApiKeyStatus)

        tvActiveModel.text = "Active Engine: ${modelChoices[activeModelIndex]}"

        val (providerId, providerName, apiKey) = getActiveProviderDetails(activeModelIndex)

        if (activeModelIndex == 0) {
            tvApiKeyStatus.text = "Engine Status: Local Rule Agent Active (100% Offline)"
            tvApiKeyStatus.setTextColor(getColor(R.color.accent_cyan))
            NovaAiEngine.activeProvider = LocalAgentProvider()
        } else if (activeModelIndex == 8) {
            tvApiKeyStatus.text = "Engine Status: Prepared for LiteRT-LM (~1.5 GB Gemma Weights Required)"
            tvApiKeyStatus.setTextColor(getColor(R.color.status_blue))
            NovaAiEngine.activeProvider = OnDeviceLlmProvider()
        } else {
            if (apiKey.isNotBlank()) {
                tvApiKeyStatus.text = "$providerName Key: Configured (••••${apiKey.takeLast(4)})"
                tvApiKeyStatus.setTextColor(getColor(R.color.status_green))
            } else {
                tvApiKeyStatus.text = "$providerName Key: Not Configured (Falling Back to Local Agent)"
                tvApiKeyStatus.setTextColor(getColor(R.color.status_orange))
            }

            when (activeModelIndex) {
                1 -> {
                    geminiProvider.updateConfig(apiKey, GeminiModels.GEMINI_2_5_FLASH)
                    NovaAiEngine.activeProvider = geminiProvider
                }
                2 -> {
                    geminiProvider.updateConfig(apiKey, GeminiModels.GEMINI_2_5_PRO)
                    NovaAiEngine.activeProvider = geminiProvider
                }
                3 -> {
                    geminiProvider.updateConfig(apiKey, GeminiModels.GEMINI_2_0_FLASH)
                    NovaAiEngine.activeProvider = geminiProvider
                }
                4 -> {
                    openAiProvider.updateConfig(apiKey, OpenAiModels.GPT_4O)
                    NovaAiEngine.activeProvider = openAiProvider
                }
                5 -> {
                    openAiProvider.updateConfig(apiKey, OpenAiModels.GPT_4O_MINI)
                    NovaAiEngine.activeProvider = openAiProvider
                }
                6 -> {
                    claudeProvider.updateConfig(apiKey, ClaudeModels.CLAUDE_3_7_SONNET_LATEST)
                    NovaAiEngine.activeProvider = claudeProvider
                }
                7 -> {
                    claudeProvider.updateConfig(apiKey, ClaudeModels.CLAUDE_3_5_HAIKU_LATEST)
                    NovaAiEngine.activeProvider = claudeProvider
                }
            }
        }
    }

    private fun getActiveProviderDetails(index: Int): Triple<String, String, String> {
        return when (index) {
            1, 2, 3 -> Triple("google_gemini", "Gemini API", SecureKeyStorage.getProviderApiKey(this, "google_gemini"))
            4, 5 -> Triple("openai_gpt", "OpenAI API", SecureKeyStorage.getProviderApiKey(this, "openai_gpt"))
            6, 7 -> Triple("anthropic_claude", "Claude API", SecureKeyStorage.getProviderApiKey(this, "anthropic_claude"))
            else -> Triple("local_agent", "Local Agent", "")
        }
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btnChangeModel).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select Multi-Provider Intelligence Engine")
                .setSingleChoiceItems(modelChoices, activeModelIndex) { dialog, which ->
                    activeModelIndex = which
                    prefs.edit().putInt("key_active_model_index", which).apply()
                    updateStatusUI()
                    Toast.makeText(this, "Switched to ${modelChoices[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnConfigureApiKey).setOnClickListener {
            showApiKeyInputDialog()
        }

        findViewById<Button>(R.id.btnTestConnection).setOnClickListener {
            testActiveProviderConnection()
        }

        findViewById<SwitchCompat>(R.id.switchEnableAi).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "Nova AI Assistant Enabled" else "Nova AI Assistant Disabled", Toast.LENGTH_SHORT).show()
        }

        findViewById<SwitchCompat>(R.id.switchBranch).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "Branch.io ingestion active" else "Branch.io ingestion paused", Toast.LENGTH_SHORT).show()
        }

        findViewById<SwitchCompat>(R.id.switchRawLog).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "TeslaCoil raw data logging active" else "Raw logging paused", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnClearData).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Analytics & Data")
                .setMessage("This resets your random User ID and clears all conversation history & raw data batches.")
                .setPositiveButton("Reset & Delete") { _, _ ->
                    lifecycleScope.launch {
                        com.example.launcherai.ai.repository.AiRepository(this@NovaAiSettingsActivity).clearHistory()
                        Toast.makeText(this@NovaAiSettingsActivity, "Local analytics and conversation cache purged successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnViewEndpoints).setOnClickListener {
            startActivity(Intent(this, EndpointsActivity::class.java))
        }
    }

    private fun showApiKeyInputDialog() {
        val (providerId, providerName, currentKey) = getActiveProviderDetails(activeModelIndex)

        if (activeModelIndex == 0 || activeModelIndex == 8) {
            Toast.makeText(this, "Local agent does not require an API key", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply {
            hint = "Enter API Key for $providerName..."
            setText(currentKey)
        }

        AlertDialog.Builder(this)
            .setTitle("Configure $providerName Key")
            .setMessage("Enter your API Key for $providerName. Keys are obfuscated locally and transmitted only to official $providerName endpoints.\n\nNOTE: For production deployment, route requests through a secure backend proxy server.")
            .setView(input)
            .setPositiveButton("Save Key") { _, _ ->
                val newKey = input.text.toString().trim()
                SecureKeyStorage.saveProviderApiKey(this, providerId, newKey)
                updateStatusUI()
                Toast.makeText(this, if (newKey.isNotBlank()) "API Key saved successfully" else "API Key cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testActiveProviderConnection() {
        val (providerId, providerName, apiKey) = getActiveProviderDetails(activeModelIndex)

        if (activeModelIndex == 0 || activeModelIndex == 8) {
            Toast.makeText(this, "Local Agent engine active (100% Offline)", Toast.LENGTH_SHORT).show()
            return
        }

        if (apiKey.isBlank()) {
            Toast.makeText(this, "Please configure an API key for $providerName first", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Testing $providerName connection...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val provider = NovaAiEngine.activeProvider
            val event = provider.streamPrompt("Hi").first()
            if (event is com.example.launcherai.ai.provider.AiEvent.Error) {
                Toast.makeText(this@NovaAiSettingsActivity, "Connection Failed: ${event.errorMessage}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@NovaAiSettingsActivity, "✓ $providerName Connection Successful!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
