package com.example.launcherai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.ai.execution.NovaAiEngine
import com.example.launcherai.ai.repository.AiRepository
import com.example.launcherai.data.db.ChatMessageEntity
import com.example.launcherai.data.repository.WorkspaceRepository
import kotlinx.coroutines.launch

class NovaAiChatActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var layoutChips: LinearLayout

    private lateinit var aiRepository: AiRepository
    private lateinit var workspaceRepository: WorkspaceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        aiRepository = AiRepository(this)
        workspaceRepository = WorkspaceRepository(this)

        rvChat = findViewById(R.id.rvChatMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        layoutChips = findViewById(R.id.layoutChips)

        chatAdapter = ChatAdapter(
            messages = mutableListOf(),
            onConfirmAction = { chatMsg ->
                lifecycleScope.launch {
                    val msgEntity = ChatMessageEntity(
                        id = chatMsg.id,
                        text = chatMsg.text,
                        isUser = false,
                        timestamp = chatMsg.timestamp,
                        reasoning = chatMsg.reasoning,
                        toolCallName = chatMsg.toolCallName,
                        toolCallResult = chatMsg.toolCallResult,
                        requiresConfirmation = true,
                        isConfirmed = false
                    )
                    val params = chatMsg.toolCallParams ?: emptyMap()
                    NovaAiEngine.confirmAndExecuteTool(
                        msgEntity,
                        params,
                        this@NovaAiChatActivity,
                        workspaceRepository,
                        aiRepository
                    )
                    Toast.makeText(this@NovaAiChatActivity, "Action confirmed & executed", Toast.LENGTH_SHORT).show()
                }
            },
            onCancelAction = { chatMsg ->
                lifecycleScope.launch {
                    val cancelledEntity = ChatMessageEntity(
                        id = chatMsg.id,
                        text = chatMsg.text,
                        isUser = false,
                        timestamp = chatMsg.timestamp,
                        reasoning = chatMsg.reasoning,
                        toolCallName = chatMsg.toolCallName,
                        toolCallResult = "❌ Action cancelled by user",
                        requiresConfirmation = false,
                        isConfirmed = false
                    )
                    aiRepository.saveMessage(cancelledEntity)
                }
            }
        )

        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = chatAdapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnClearChat).setOnClickListener {
            lifecycleScope.launch {
                aiRepository.clearHistory()
                sendAiWelcomeMessage()
            }
        }

        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, NovaAiSettingsActivity::class.java))
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                submitUserQuery(text)
                etMessage.setText("")
            }
        }

        setupSuggestionChips()
        observeConversationHistory()

        // Handle incoming query extra if launched from search bar
        val query = intent.getStringExtra("query")
        if (!query.isNullOrBlank()) {
            submitUserQuery(query)
        }
    }

    private fun observeConversationHistory() {
        lifecycleScope.launch {
            aiRepository.conversationHistoryFlow.collect { entities ->
                if (entities.isEmpty()) {
                    sendAiWelcomeMessage()
                } else {
                    val chatMessages = entities.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            text = entity.text,
                            isUser = entity.isUser,
                            timestamp = entity.timestamp,
                            reasoning = entity.reasoning,
                            toolCallName = entity.toolCallName,
                            toolCallResult = entity.toolCallResult,
                            requiresConfirmation = entity.requiresConfirmation,
                            isConfirmed = entity.isConfirmed
                        )
                    }
                    chatAdapter.clearMessages()
                    chatMessages.forEach { chatAdapter.addMessage(it) }
                    rvChat.post {
                        rvChat.smoothScrollToPosition(chatAdapter.itemCount.coerceAtLeast(1) - 1)
                    }
                }
            }
        }
    }

    private fun setupSuggestionChips() {
        val suggestions = listOf(
            "What data sources does Nova use?",
            "Search my installed apps",
            "Explain Nova AI architecture",
            "Modify workspace grid layout",
            "Hide camera from app drawer"
        )

        val inflater = LayoutInflater.from(this)
        layoutChips.removeAllViews()
        for (s in suggestions) {
            val chip = inflater.inflate(R.layout.item_suggestion_chip, layoutChips, false) as TextView
            chip.text = s
            chip.setOnClickListener {
                submitUserQuery(s)
            }
            layoutChips.addView(chip)
        }
    }

    private fun sendAiWelcomeMessage() {
        lifecycleScope.launch {
            aiRepository.saveMessage(
                ChatMessageEntity(
                    text = "Hello! I am your Nova AI Assistant, powered by local launcher agent engine. I can search installed apps, launch apps, modify grid layouts, or create workspace folders with human-in-the-loop confirmation.",
                    isUser = false,
                    reasoning = "Initialized local agent context. Session authenticated with AiToolBridge native tools.",
                    toolCallName = "init_session",
                    toolCallResult = "Session initialized (Model: Local Agent Engine v8A / AiToolBridge)"
                )
            )
        }
    }

    private fun submitUserQuery(query: String) {
        lifecycleScope.launch {
            NovaAiEngine.submitPrompt(
                query,
                this@NovaAiChatActivity,
                workspaceRepository,
                aiRepository
            ).collect {
                // Flow updates conversationHistoryFlow reactively!
            }
        }
    }
}
