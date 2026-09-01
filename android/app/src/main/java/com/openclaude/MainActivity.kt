package com.openclaude

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val text: String = "",
    val thinking: String = "",
    val toolUse: String = "",
    val isStreaming: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = EngineClient()
        val engineInfo = try {
            EngineNative.nativeEngineInfo()
        } catch (_: Exception) {
            "JNI engine unavailable"
        }

        setContent {
            OpenClaudeTheme {
                ChatScreen(client = client, engineInfo = engineInfo)
            }
        }
    }
}

@Composable
fun OpenClaudeTheme(content: @Composable () -> Unit) {
    val darkScheme = darkColorScheme(
        surface = Color(0xFF0E0E11),
        background = Color(0xFF0E0E11),
        primary = Color(0xFF5B8CFF),
        secondary = Color(0xFF7C5CFF),
        onSurface = Color(0xFFE8E8ED),
        onBackground = Color(0xFFE8E8ED),
        surfaceVariant = Color(0xFF1A1A24),
        onSurfaceVariant = Color(0xFFA0A0B0),
        outline = Color(0xFF2A2A35)
    )
    MaterialTheme(
        colorScheme = darkScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(client: EngineClient, engineInfo: String) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Probe for binary on launch
    LaunchedEffect(Unit) {
        connected = client.probe()
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenClaude", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Connection status chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (connected) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (connected) "Exec" else "HTTP",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    // New chat button
                    TextButton(onClick = {
                        messages = emptyList()
                        sessionId = ""
                    }) {
                        Text("New", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message OpenClaude...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isProcessing,
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val msg = inputText.trim()
                            if (msg.isBlank() || isProcessing) return@FilledIconButton
                            inputText = ""
                            isProcessing = true

                            val userMsg = ChatMessage(role = "user", text = msg)
                            val aiMsg = ChatMessage(role = "assistant", isStreaming = true)
                            messages = messages + userMsg + aiMsg

                            scope.launch {
                                val idx = messages.size - 1
                                client.chat(msg, sessionId).collect { event ->
                                    when (event) {
                                        is ChatEvent.Text -> {
                                            val updated = messages.toMutableList()
                                            val prev = updated[idx]
                                            updated[idx] = prev.copy(text = prev.text + event.text)
                                            messages = updated
                                        }
                                        is ChatEvent.Thinking -> {
                                            val updated = messages.toMutableList()
                                            val prev = updated[idx]
                                            updated[idx] = prev.copy(thinking = prev.thinking + event.text)
                                            messages = updated
                                        }
                                        is ChatEvent.ToolUse -> {
                                            val updated = messages.toMutableList()
                                            val prev = updated[idx]
                                            val tu = "**${event.name}**: ${event.input}"
                                            updated[idx] = prev.copy(toolUse = tu)
                                            messages = updated
                                        }
                                        is ChatEvent.Done -> {
                                            sessionId = event.sessionId
                                            val updated = messages.toMutableList()
                                            val prev = updated[idx]
                                            updated[idx] = prev.copy(isStreaming = false)
                                            messages = updated
                                            isProcessing = false
                                        }
                                        is ChatEvent.Error -> {
                                            val updated = messages.toMutableList()
                                            val prev = updated[idx]
                                            updated[idx] = prev.copy(
                                                text = prev.text + "\n\n*Error: ${event.message}*",
                                                isStreaming = false
                                            )
                                            messages = updated
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isProcessing,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(">", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp)
        ) {
            // Engine info chip
            Text(
                text = engineInfo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
            }

            // Suggestion chips when empty
            if (messages.isEmpty()) {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Write a poem",
                        "Explain quantum computing",
                        "Debug my code"
                    ).forEach { suggestion ->
                        SuggestionChip(
                            onClick = { inputText = suggestion },
                            label = { Text(suggestion, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        if (msg.role == "user") {
            // User message: right-aligned gradient bubble
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = msg.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        } else {
            // AI message: left-aligned card
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Thinking block (collapsible)
                    AnimatedVisibility(visible = msg.thinking.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Thinking",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = msg.thinking,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Tool use block
                    AnimatedVisibility(visible = msg.toolUse.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A237E).copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = msg.toolUse,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Main text
                    Text(
                        text = msg.text + if (msg.isStreaming) " ▌" else "",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}