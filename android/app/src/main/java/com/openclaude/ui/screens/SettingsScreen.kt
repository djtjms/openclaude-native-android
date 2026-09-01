package com.openclaude.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.EngineMode
import com.openclaude.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Engine Mode",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                EngineMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewModel.engineMode == mode,
                        onClick = { viewModel.setEngineMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = EngineMode.entries.size
                        )
                    ) {
                        Text(mode.name, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Connection",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (viewModel.connected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = if (viewModel.connected) "● Connected" else "○ Disconnected",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (viewModel.connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Text(
                "Engine Info",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                viewModel.engineInfo.ifBlank { "Not probed yet" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { viewModel.probeEngine() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Re-probe Engine")
            }

            Spacer(Modifier.weight(1f))
            Text(
                "OpenClaude v0.1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}