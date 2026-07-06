package com.flowautomation.app.model

data class PromptGroup(
    val id: String,
    val prompts: List<String>,
    val config: FlowConfig,
    val status: GroupStatus = GroupStatus.IDLE,
    val progress: List<PromptProgress> = emptyList()
)

data class FlowConfig(
    val mode: MediaMode = MediaMode.VIDEO,
    val videoMode: VideoMode = VideoMode.TEXT_TO_VIDEO,
    val imageMode: ImageMode = ImageMode.CREATE_NEW,
    val outputCount: Int = 1,
    val aspectRatio: String = "16:9",
    val quality: String = "1080p",
    val duration: String = "8s",
    val model: String = "",
    val maxConcurrent: Int = 3
)

enum class MediaMode { VIDEO, IMAGE }
enum class VideoMode { TEXT_TO_VIDEO, IMAGE_TO_VIDEO, COMPONENT_TO_VIDEO }
enum class ImageMode { CREATE_NEW, UPLOAD_IMAGE, UPLOAD_CHARACTER }

enum class GroupStatus { IDLE, RUNNING, COMPLETED, CANCELLED, ERROR }
enum class PromptStatus { PENDING, GENERATING, COMPLETED, FAILED }

data class PromptProgress(
    val index: Int,
    val prompt: String,
    val status: PromptStatus = PromptStatus.PENDING,
    val percentage: Int = 0,
    val tileId: String? = null,
    val downloadUrl: String? = null
)

data class ActionLog(
    val level: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AutomationCommand {
    data object Start : AutomationCommand()
    data object Stop : AutomationCommand()
    data class SetConfig(val config: FlowConfig) : AutomationCommand()
    data class AddPrompts(val prompts: List<String>) : AutomationCommand()
    data class ClearPrompts(val groupIndex: Int) : AutomationCommand()
    data object ClearCache : AutomationCommand()
    data class SetZoom(val factor: Float) : AutomationCommand()
}
