package com.openclaude.ui

import kotlinx.serialization.Serializable
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val text: String = "",
    val thinking: String = "",
    val toolUse: String = "",
    val isStreaming: Boolean = false
)

@Serializable
data class HistoryEntry(
    val title: String,
    val sessionId: String,
    val timestamp: Long
)

@Serializable
data class HistoryList(val items: List<HistoryEntry>)