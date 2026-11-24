package com.example.launcherai.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.ai.model.ChatMessage

class ChatAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onConfirmAction: ((ChatMessage) -> Unit)? = null,
    private val onCancelAction: ((ChatMessage) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    fun addMessage(message: ChatMessage) {
        messages.removeIf { it.id == message.id }
        messages.add(message)
        notifyDataSetChanged()
    }

    fun clearMessages() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutUser: LinearLayout = itemView.findViewById(R.id.layoutUserMessage)
        private val tvUserText: TextView = itemView.findViewById(R.id.tvUserText)
        private val tvUserTime: TextView = itemView.findViewById(R.id.tvUserTime)

        private val layoutAi: LinearLayout = itemView.findViewById(R.id.layoutAiMessage)
        private val tvAiText: TextView = itemView.findViewById(R.id.tvAiText)
        private val tvAiTime: TextView = itemView.findViewById(R.id.tvAiTime)
        private val btnCopyAi: ImageView = itemView.findViewById(R.id.btnCopyAi)

        private val layoutThinking: LinearLayout = itemView.findViewById(R.id.layoutThinking)
        private val layoutThinkingHeader: LinearLayout = itemView.findViewById(R.id.layoutThinkingHeader)
        private val tvThinkingContent: TextView = itemView.findViewById(R.id.tvThinkingContent)
        private val ivThinkingChevron: ImageView = itemView.findViewById(R.id.ivThinkingChevron)

        private val layoutToolCall: LinearLayout = itemView.findViewById(R.id.layoutToolCall)
        private val tvToolCallName: TextView = itemView.findViewById(R.id.tvToolCallName)
        private val tvToolCallResult: TextView = itemView.findViewById(R.id.tvToolCallResult)

        private val layoutConfirmation: LinearLayout = itemView.findViewById(R.id.layoutConfirmation)
        private val tvConfirmationPrompt: TextView = itemView.findViewById(R.id.tvConfirmationPrompt)
        private val btnConfirmAction: TextView = itemView.findViewById(R.id.btnConfirmAction)
        private val btnCancelAction: TextView = itemView.findViewById(R.id.btnCancelAction)

        private var isThinkingExpanded = false

        fun bind(message: ChatMessage) {
            if (message.isUser) {
                layoutUser.visibility = View.VISIBLE
                layoutAi.visibility = View.GONE
                tvUserText.text = message.text
                tvUserTime.text = message.timestamp
            } else {
                layoutUser.visibility = View.VISIBLE
                layoutAi.visibility = View.VISIBLE
                layoutUser.visibility = View.GONE
                tvAiText.text = message.text
                tvAiTime.text = message.timestamp

                // Collapsible reasoning
                if (!message.reasoning.isNullOrBlank()) {
                    layoutThinking.visibility = View.VISIBLE
                    tvThinkingContent.text = message.reasoning
                    layoutThinkingHeader.setOnClickListener {
                        isThinkingExpanded = !isThinkingExpanded
                        tvThinkingContent.visibility = if (isThinkingExpanded) View.VISIBLE else View.GONE
                        ivThinkingChevron.setImageResource(
                            if (isThinkingExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
                        )
                    }
                } else {
                    layoutThinking.visibility = View.GONE
                }

                // Tool call card
                if (!message.toolCallName.isNullOrBlank() && !message.requiresConfirmation) {
                    layoutToolCall.visibility = View.VISIBLE
                    tvToolCallName.text = "⚡ Tool Executed: ${message.toolCallName}"
                    tvToolCallResult.text = message.toolCallResult ?: "Tool execution completed."
                } else {
                    layoutToolCall.visibility = View.GONE
                }

                // HITL Confirmation card
                if (message.requiresConfirmation && !message.isConfirmed) {
                    layoutConfirmation.visibility = View.VISIBLE
                    tvConfirmationPrompt.text = message.toolCallResult ?: "Action requires confirmation."
                    btnConfirmAction.setOnClickListener {
                        onConfirmAction?.invoke(message)
                    }
                    btnCancelAction.setOnClickListener {
                        onCancelAction?.invoke(message)
                    }
                } else {
                    layoutConfirmation.visibility = View.GONE
                }

                // Copy response
                btnCopyAi.setOnClickListener {
                    val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Nova AI Response", message.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(itemView.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
