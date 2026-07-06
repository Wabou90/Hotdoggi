package com.flowautomation.app.ui.sidepanel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun AddPromptsDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var prompts by remember { mutableStateOf(listOf("")) }
    var pasteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Prompts") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Paste prompts (one per line):",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 160.dp),
                    placeholder = { Text("Prompt 1\nPrompt 2\n...") },
                    maxLines = 8
                )
                if (pasteText.isNotBlank()) {
                    FilledTonalButton(
                        onClick = {
                            val lines = pasteText.lines()
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            prompts = lines
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Parse ${pasteText.lines().count { it.trim().isNotEmpty() }} prompts")
                    }
                }

                HorizontalDivider()

                Text(
                    "Or add individually (${prompts.size}):",
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(prompts.withIndex().toList()) { (i, p) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${i + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(28.dp)
                            )
                            OutlinedTextField(
                                value = p,
                                onValueChange = { v ->
                                    prompts = prompts.toMutableList().also { it[i] = v }
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                placeholder = { Text("Prompt text...") }
                            )
                            IconButton(
                                onClick = { prompts = prompts.toMutableList().also { it.removeAt(i) } }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = { prompts = prompts + "" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add row")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(prompts.filter { it.isNotBlank() }) },
                enabled = prompts.any { it.isNotBlank() }
            ) {
                Text("Start Automation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
