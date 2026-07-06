package com.flowautomation.app.ui.sidepanel

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowautomation.app.automation.AutomationState
import com.flowautomation.app.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidePanelSheet(
    state: AutomationState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearCache: () -> Unit,
    onAddPrompts: (List<String>) -> Unit,
    onClearGroup: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val isRunning by state.isRunning.collectAsState()
    val groups by state.promptGroups.collectAsState()
    val logs by state.logs.collectAsState()
    var expandedGroup by remember { mutableIntStateOf(-1) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .animateContentSize()
    ) {
        HandleBar()
        TopControls(isRunning, onStart, onStop, onClearCache, onOpenSettings)

        TabRow(selectedTabIndex = 0) {
            Tab(selected = true, onClick = {}, text = { Text("Prompts (${groups.size})") })
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(40.dp))
                            Text("No prompt groups yet", style = MaterialTheme.typography.bodyLarge)
                            FilledTonalButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Add Prompts")
                            }
                        }
                    }
                }
            }

            items(groups.withIndex().toList(), key = { it.index }) { (idx, group) ->
                PromptGroupCard(
                    group = group,
                    isExpanded = expandedGroup == idx,
                    onToggle = { expandedGroup = if (expandedGroup == idx) -1 else idx },
                    onRemove = { onClearGroup(idx) }
                )
            }

            if (groups.isNotEmpty()) {
                item {
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Prompt Group")
                    }
                }
            }

            if (logs.isNotEmpty()) {
                item {
                    Text(
                        "Activity Log",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(logs.takeLast(50)) { log ->
                    LogEntry(log)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        AddPromptsDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { prompts ->
                onAddPrompts(prompts)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HandleBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun TopControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearCache: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRunning) {
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Stop")
            }
        } else {
            Button(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Start")
            }
        }
        FilledTonalIconButton(onClick = onClearCache) {
            Icon(Icons.Default.CleaningServices, contentDescription = "Clear cache")
        }
        FilledTonalIconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun PromptGroupCard(
    group: PromptGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${group.prompts.size} prompts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        group.config.mode.name + " | " + group.status.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(group.status)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }

            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                group.progress.forEach { progress ->
                    PromptProgressRow(progress)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: GroupStatus) {
    val (color, text) = when (status) {
        GroupStatus.IDLE -> MaterialTheme.colorScheme.outline to "Idle"
        GroupStatus.RUNNING -> MaterialTheme.colorScheme.primary to "Running"
        GroupStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Done"
        GroupStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
        GroupStatus.ERROR -> MaterialTheme.colorScheme.error to "Error"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun PromptProgressRow(progress: PromptProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${progress.index + 1}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = progress.prompt,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        when (progress.status) {
            PromptStatus.PENDING -> Text("...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            PromptStatus.GENERATING -> LinearProgressIndicator(
                progress = { progress.percentage / 100f },
                modifier = Modifier.width(60.dp)
            )
            PromptStatus.COMPLETED -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            PromptStatus.FAILED -> Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LogEntry(log: ActionLog) {
    val color = when (log.level) {
        "error" -> MaterialTheme.colorScheme.error
        "warn" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = log.message,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
