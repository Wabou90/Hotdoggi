package com.flowautomation.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flowautomation.app.automation.AutomationState
import com.flowautomation.app.storage.AppPreferences
import com.flowautomation.app.ui.components.FlowWebView
import com.flowautomation.app.ui.components.WebViewController
import com.flowautomation.app.ui.sidepanel.SettingsPanel
import com.flowautomation.app.ui.sidepanel.SidePanelSheet
import com.flowautomation.app.bridge.WebAppInterface
import com.flowautomation.app.download.DownloadHandler


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: AutomationState,
    prefs: AppPreferences,
    context: android.content.Context
) {
    var showSidePanel by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val config by state.config.collectAsState()
    val isRunning by state.isRunning.collectAsState()

    val bridge = remember {
        WebAppInterface(state, DownloadHandler(context), prefs)
    }

    var webViewController by remember { mutableStateOf<WebViewController?>(null) }

    Scaffold(
        snackbarHost = {
            if (isLoading) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        if (loadingProgress > 0) {
                            Text("$loadingProgress%")
                        }
                    }
                ) {
                    Text(if (loadingProgress < 100) "Loading Google Flow..." else "Ready")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSidePanel = !showSidePanel },
                containerColor = if (isRunning) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (showSidePanel) Icons.Default.Close else Icons.Default.SettingsSuggest,
                    contentDescription = "Toggle controls"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FlowWebView(
                bridge = bridge,
                onControllerReady = { webViewController = it },
                onPageStarted = { isLoading = true },
                onPageFinished = { url ->
                    isLoading = false
                    state.setCurrentUrl(url)
                },
                onProgress = { loadingProgress = it },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = showSidePanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SidePanelSheet(
                        state = state,
                        onStart = {
                            state.setRunning(true)
                            state.addLog("info", "Starting automation...")
                            webViewController?.startAutomation()
                            showSidePanel = false
                        },
                        onStop = {
                            state.setRunning(false)
                            state.savePrompts(prefs)
                            state.addLog("info", "Automation stopped")
                            webViewController?.stopAutomation()
                        },
                        onClearCache = {
                            state.addLog("info", "Cache clear requested")
                            webViewController?.clearCache()
                        },
                        onAddPrompts = { prompts ->
                            state.addPrompts(prompts)
                            state.addLog("info", "Added ${prompts.size} prompts")
                            state.savePrompts(prefs)
                        },
                        onClearGroup = { index ->
                            state.removeGroup(index)
                            state.savePrompts(prefs)
                        },
                        onOpenSettings = { showSettings = !showSettings }
                    )
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {},
            text = {
                SettingsPanel(
                    config = config,
                    folderName = prefs.folderName,
                    prefix = prefs.prefix,
                    autoRename = prefs.autoChangeFileName,
                    onConfigChange = { newConfig ->
                        state.updateConfig(newConfig)
                        state.saveConfig(prefs)
                    },
                    onFolderChange = { prefs.folderName = it },
                    onPrefixChange = { prefs.prefix = it },
                    onAutoRenameChange = { prefs.autoChangeFileName = it },
                    onDismiss = { showSettings = false }
                )
            }
        )
    }
}
