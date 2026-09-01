package com.openclaude.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.ui.ChatMessage
import com.openclaude.ui.theme.BrandGradient
import com.openclaude.ui.theme.BrandPurple

@Composable
fun MessageBubble(msg: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        if (msg.role == "user") {
            UserBubble(msg)
        } else {
            AssistantBubble(msg)
        }
    }
}

@Composable
private fun UserBubble(msg: ChatMessage) {
    Column(
        modifier = Modifier.align(Alignment.End),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
        ) {
            Text(
                "You",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "U",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = msg.text,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AssistantBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier
                .padding(top = 4.dp, end = 8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "C",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Claude",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (msg.isStreaming) {
                    Spacer(Modifier.width(6.dp))
                    StreamingIndicator()
                }
            }
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AnimatedVisibility(visible = msg.thinking.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Thinking",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    msg.thinking,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = msg.toolUse.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandPurple.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                msg.toolUse,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Text(
                        text = msg.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}