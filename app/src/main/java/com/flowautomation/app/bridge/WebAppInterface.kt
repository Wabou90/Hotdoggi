package com.flowautomation.app.bridge

import android.util.Log
import android.webkit.JavascriptInterface
import com.flowautomation.app.automation.AutomationState
import com.flowautomation.app.download.DownloadHandler
import com.flowautomation.app.model.PromptProgress
import com.flowautomation.app.model.PromptStatus
import com.flowautomation.app.storage.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WebAppInterface(
    private val state: AutomationState,
    private val downloadHandler: DownloadHandler,
    private val prefs: AppPreferences
) {
    private val gson = Gson()

    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val msg = gson.fromJson(json, Map::class.java)
            val type = msg["type"] as? String ?: return

            when (type) {
                "GET_REMOTE_CONFIG" -> {
                    // handled internally before injection
                }
                "DOWNLOAD_VIDEO" -> {
                    val url = msg["url"] as? String ?: return
                    val filename = msg["filename"] as? String ?: "flow_output.mp4"
                    val folder = msg["folder"] as? String ?: prefs.folderName
                    val id = downloadHandler.download(url, filename, folder)
                    Log.i("WebAppInterface", "Download queued: $id - $filename")
                }
                "PROMPT_GROUP_STATUS" -> {
                    val data = msg["data"] as? Map<*, *>
                    val groupId = data?.get("groupId") as? String ?: return
                    val status = data["status"] as? String ?: ""
                    val promptIndex = (data["promptIndex"] as? Double)?.toInt() ?: -1
                    val percentage = (data["percentage"] as? Double)?.toInt() ?: 0
                    val tileId = data["tileId"] as? String
                    val downloadUrl = data["downloadUrl"] as? String

                    if (promptIndex >= 0) {
                        val ps = when (status) {
                            "generating" -> PromptStatus.GENERATING
                            "completed" -> PromptStatus.COMPLETED
                            "failed" -> PromptStatus.FAILED
                            else -> PromptStatus.PENDING
                        }
                        state.updateProgress(groupId, promptIndex, PromptProgress(
                            index = promptIndex,
                            prompt = data["prompt"] as? String ?: "",
                            status = ps,
                            percentage = percentage,
                            tileId = tileId,
                            downloadUrl = downloadUrl
                        ))
                    }
                }
                "ACTION_LOG" -> {
                    val data = msg["data"] as? Map<*, *>
                    val level = data?.get("level") as? String ?: "info"
                    val message = data?.get("message") as? String ?: ""
                    state.addLog(level, message)
                }
                "AUTO_FILL_FLOW" -> {
                    state.addLog("info", "Automation completed a cycle")
                }
                "CANCEL_PROMPT_GROUP" -> {
                    val data = msg["data"] as? Map<*, *>
                    val groupId = data?.get("groupId") as? String ?: ""
                    state.updateGroupStatus(groupId, com.flowautomation.app.model.GroupStatus.CANCELLED)
                }
            }
        } catch (e: Exception) {
            Log.e("WebAppInterface", "Error parsing message: $json", e)
        }
    }

    @JavascriptInterface
    fun getConfig(): String {
        val config = state.config.value
        val map = mapOf(
            "folderName" to prefs.folderName,
            "prefix" to prefs.prefix,
            "autoChangeFileName" to prefs.autoChangeFileName.toString(),
            "mode" to config.mode.name.lowercase(),
            "videoMode" to config.videoMode.name,
            "imageMode" to config.imageMode.name,
            "outputCount" to config.outputCount.toString(),
            "aspectRatio" to config.aspectRatio,
            "quality" to config.quality,
            "duration" to config.duration,
            "model" to config.model,
            "maxConcurrent" to config.maxConcurrent.toString()
        )
        return gson.toJson(map)
    }

    @JavascriptInterface
    fun getPrompts(): String {
        val groups = state.promptGroups.value
        val active = groups.filter { it.status == com.flowautomation.app.model.GroupStatus.RUNNING }
            .ifEmpty { groups.filter { it.status == com.flowautomation.app.model.GroupStatus.IDLE } }
        val prompts = active.firstOrNull()?.prompts ?: emptyList()
        return gson.toJson(mapOf("groupId" to (active.firstOrNull()?.id ?: ""), "prompts" to prompts))
    }
}
