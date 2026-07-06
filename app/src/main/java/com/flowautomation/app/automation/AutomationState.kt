package com.flowautomation.app.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowautomation.app.model.*
import com.flowautomation.app.storage.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AutomationState : ViewModel() {
    private val _promptGroups = MutableStateFlow<List<PromptGroup>>(emptyList())
    val promptGroups: StateFlow<List<PromptGroup>> = _promptGroups.asStateFlow()

    private val _config = MutableStateFlow(FlowConfig())
    val config: StateFlow<FlowConfig> = _config.asStateFlow()

    private val _logs = MutableStateFlow<List<ActionLog>>(emptyList())
    val logs: StateFlow<List<ActionLog>> = _logs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _zoom = MutableStateFlow(1.0f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    fun init(prefs: AppPreferences) {
        _config.value = prefs.lastConfig
        val saved = prefs.savedPrompts
        if (saved.isNotEmpty() && _promptGroups.value.isEmpty()) {
            addPrompts(saved)
        }
    }

    fun setCurrentUrl(url: String) { _currentUrl.value = url }
    fun setZoom(zoom: Float) { _zoom.value = zoom }

    fun addPrompts(prompts: List<String>) {
        if (prompts.isEmpty()) return
        val group = PromptGroup(
            id = UUID.randomUUID().toString(),
            prompts = prompts,
            config = _config.value,
            status = GroupStatus.IDLE,
            progress = prompts.mapIndexed { i, p ->
                PromptProgress(index = i, prompt = p)
            }
        )
        _promptGroups.value = _promptGroups.value + group
    }

    fun removeGroup(index: Int) {
        val list = _promptGroups.value.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _promptGroups.value = list
    }

    fun clearAllGroups() { _promptGroups.value = emptyList() }

    fun updateConfig(config: FlowConfig) { _config.value = config }

    fun updateProgress(groupId: String, promptIndex: Int, progress: PromptProgress) {
        _promptGroups.value = _promptGroups.value.map { group ->
            if (group.id != groupId) return@map group
            val newProgress = group.progress.toMutableList().also {
                if (promptIndex in it.indices) it[promptIndex] = progress
            }
            group.copy(progress = newProgress)
        }
    }

    fun updateGroupStatus(groupId: String, status: GroupStatus) {
        _promptGroups.value = _promptGroups.value.map { group ->
            if (group.id == groupId) group.copy(status = status) else group
        }
    }

    fun setRunning(running: Boolean) { _isRunning.value = running }

    fun addLog(level: String, message: String) {
        val log = ActionLog(level = level, message = message)
        _logs.value = (_logs.value + log).take(500)
    }

    fun savePrompts(prefs: AppPreferences) {
        val all = _promptGroups.value.flatMap { it.prompts }
        prefs.savedPrompts = all
    }

    fun saveConfig(prefs: AppPreferences) {
        prefs.lastConfig = _config.value
    }
}
