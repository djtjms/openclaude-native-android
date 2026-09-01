package com.openclaude.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.openclaude.ChatEvent
import com.openclaude.EngineClient
import com.openclaude.EngineMode
import com.openclaude.EngineNative
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainViewModel(
    private val context: Context,
    private val client: EngineClient,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("openclaude", Context.MODE_PRIVATE)

    var messages by mutableStateOf(listOf<ChatMessage>())
        private set
    var sessionId by mutableStateOf("")
        private set
    var connected by mutableStateOf(false)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var engineInfo by mutableStateOf("")
        private set
    var engineMode by mutableStateOf(EngineMode.AUTO)
        private set
    var history by mutableStateOf(listOf<HistoryEntry>())
        private set

    init {
        val saved = prefs.getString("history", null)
        if (saved != null) {
            try {
                history = json.decodeFromString<HistoryList>(saved).items
            } catch (_: Exception) {
                history = emptyList()
            }
        }
    }

    fun probeEngine() {
        connected = client.probe()
        engineMode = client.mode
        engineInfo = try {
            EngineNative.nativeEngineInfo()
        } catch (_: Exception) {
            if (connected) "Exec mode ready" else "HTTP fallback (bridge $bridgeUrl)"
        }
    }

    private val bridgeUrl: String get() = client.getBridgeUrl()

    fun setEngineMode(mode: EngineMode) {
        engineMode = mode
        client.setMode(mode)
        probeEngine()
    }

    fun newChat(resumeSessionId: String? = null) {
        messages = if (resumeSessionId != null) {
            listOf(
                ChatMessage(
                    role = "assistant",
                    text = "Resumed previous session. Continue from here.",
                    isStreaming = false
                )
            )
        } else {
            emptyList()
        }
        sessionId = resumeSessionId ?: ""
    }

    fun send(message: String) {
        val msg = message.trim()
        if (msg.isBlank() || isProcessing) return

        isProcessing = true
        val userMsg = ChatMessage(role = "user", text = msg)
        val aiMsg = ChatMessage(role = "assistant", isStreaming = true)
        messages = messages + userMsg + aiMsg
        val idx = messages.size - 1

        scope.launch {
            var resolvedSessionId = sessionId
            client.chat(msg, sessionId).collect { event ->
                when (event) {
                    is ChatEvent.Text -> {
                        val updated = messages.toMutableList()
                        updated[idx] = updated[idx].copy(text = updated[idx].text + event.text)
                        messages = updated
                    }
                    is ChatEvent.Thinking -> {
                        val updated = messages.toMutableList()
                        updated[idx] = updated[idx].copy(thinking = updated[idx].thinking + event.text)
                        messages = updated
                    }
                    is ChatEvent.ToolUse -> {
                        val updated = messages.toMutableList()
                        updated[idx] = updated[idx].copy(toolUse = "**${event.name}**: ${event.input}")
                        messages = updated
                    }
                    is ChatEvent.Done -> {
                        resolvedSessionId = event.sessionId
                        sessionId = event.sessionId
                        val updated = messages.toMutableList()
                        updated[idx] = updated[idx].copy(isStreaming = false)
                        messages = updated
                        isProcessing = false
                        saveToHistory(resolvedSessionId)
                    }
                    is ChatEvent.Error -> {
                        val updated = messages.toMutableList()
                        updated[idx] = updated[idx].copy(
                            text = updated[idx].text + "\n\n*Error: ${event.message}*",
                            isStreaming = false
                        )
                        messages = updated
                        isProcessing = false
                    }
                }
            }
        }
    }

    private fun saveToHistory(sessionId: String) {
        val title = messages.firstOrNull { it.role == "user" }?.text?.take(60) ?: "Chat"
        val entry = HistoryEntry(title, sessionId, System.currentTimeMillis())
        history = listOf(entry) + history.filter { it.sessionId != sessionId }
        saveHistory()
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        history = history.filter { it.sessionId != entry.sessionId }
        saveHistory()
    }

    private fun saveHistory() {
        try {
            val s = json.encodeToString(HistoryList(history))
            prefs.edit().putString("history", s).apply()
        } catch (_: Exception) {
            // ignore persistence failures
        }
    }
}