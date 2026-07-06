package com.flowautomation.app.ui.sidepanel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowautomation.app.model.FlowConfig
import com.flowautomation.app.model.MediaMode
import com.flowautomation.app.model.VideoMode
import com.flowautomation.app.model.ImageMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    config: FlowConfig,
    folderName: String,
    prefix: String,
    autoRename: Boolean,
    onConfigChange: (FlowConfig) -> Unit,
    onFolderChange: (String) -> Unit,
    onPrefixChange: (String) -> Unit,
    onAutoRenameChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    @Composable
    fun SectionHeader(title: String) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
    }

    @Composable
    fun <T> SelectorRow(
        label: String,
        options: List<T>,
        selected: T,
        onSelect: (T) -> Unit,
        labelFn: (T) -> String = { it.toString() }
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { opt ->
                SegmentedButton(
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = options.indexOf(opt),
                        count = options.size
                    )
                ) { Text(labelFn(opt), style = MaterialTheme.typography.labelSmall) }
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SectionHeader("Media Mode")
        SelectorRow("", MediaMode.entries, config.mode, { onConfigChange(config.copy(mode = it)) },
            labelFn = { it.name.lowercase().replaceFirstChar(Char::uppercase) })

        when (config.mode) {
            MediaMode.VIDEO -> {
                SectionHeader("Video Mode")
                SelectorRow("", VideoMode.entries, config.videoMode, { onConfigChange(config.copy(videoMode = it)) },
                    labelFn = { it.name.replace("_", " ").lowercase().replaceFirstChar(Char::uppercase) })
            }
            MediaMode.IMAGE -> {
                SectionHeader("Image Mode")
                SelectorRow("", ImageMode.entries, config.imageMode, { onConfigChange(config.copy(imageMode = it)) },
                    labelFn = { it.name.replace("_", " ").lowercase().replaceFirstChar(Char::uppercase) })
            }
        }

        SectionHeader("Output")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Count:", modifier = Modifier.width(56.dp))
            listOf(1, 2, 4).forEach { n ->
                FilterChip(
                    selected = config.outputCount == n,
                    onClick = { onConfigChange(config.copy(outputCount = n)) },
                    label = { Text("$n") }
                )
            }
        }

        SectionHeader("Quality")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("1080p", "2K", "4K").forEach { q ->
                FilterChip(
                    selected = config.quality == q,
                    onClick = { onConfigChange(config.copy(quality = q)) },
                    label = { Text(q) }
                )
            }
        }

        SectionHeader("Duration")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("4s", "6s", "8s", "10s").forEach { d ->
                FilterChip(
                    selected = config.duration == d,
                    onClick = { onConfigChange(config.copy(duration = d)) },
                    label = { Text(d) }
                )
            }
        }

        SectionHeader("Aspect Ratio")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("16:9", "9:16", "1:1", "4:3").forEach { r ->
                FilterChip(
                    selected = config.aspectRatio == r,
                    onClick = { onConfigChange(config.copy(aspectRatio = r)) },
                    label = { Text(r) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("Downloads")
        OutlinedTextField(
            value = folderName,
            onValueChange = onFolderChange,
            label = { Text("Folder name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = prefix,
            onValueChange = onPrefixChange,
            label = { Text("Filename prefix") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-rename files")
            Switch(checked = autoRename, onCheckedChange = onAutoRenameChange)
        }

        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
