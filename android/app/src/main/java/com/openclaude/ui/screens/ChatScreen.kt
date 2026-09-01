package com.openclaude.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.ui.MainViewModel
import com.openclaude.ui.components.AnimatedLogo
import com.openclaude.ui.components.MessageBubble
import com.openclaude.ui.theme.BrandGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    var inputText by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val visibleMessages = if (searchQuery.isBlank()) {
        viewModel.messages
    } else {
        viewModel.messages.filter {
            it.text.contains(searchQuery, ignoreCase = true) ||
                it.thinking.contains(searchQuery, ignoreCase = true) ||
                it.toolUse.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(viewModel.messages.size, viewModel.messages.lastOrNull()?.text) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (showSearch) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search messages...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedLogo(modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("OpenClaude", fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            actions = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (viewModel.connected) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = if (viewModel.connected) "Exec" else "HTTP",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = {
                    showSearch = !showSearch
                    if (!showSearch) searchQuery = ""
                }) {
                    Icon(
                        if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { viewModel.newChat() }) {
                    Text("New", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                viewModel.probeEngine()
                refreshing = false
            },
            modifier = Modifier.weight(1f)
        ) {
            if (visibleMessages.isEmpty()) {
                if (showSearch) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    EmptyState { inputText = it }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleMessages, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }
        }

        Text(
            text = viewModel.engineInfo,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

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
                    enabled = !viewModel.isProcessing,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank() && !viewModel.isProcessing) {
                            viewModel.send(inputText)
                            inputText = ""
                        }
                    })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !viewModel.isProcessing) {
                            viewModel.send(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !viewModel.isProcessing,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (viewModel.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "OpenClaude",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(brush = BrandGradient)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your AI coding assistant",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Write a poem", "Explain quantum computing", "Debug my code").forEach { s ->
                SuggestionChip(
                    onClick = { onSuggestion(s) },
                    label = { Text(s, fontSize = 12.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}