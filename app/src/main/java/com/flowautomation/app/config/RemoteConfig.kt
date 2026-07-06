package com.flowautomation.app.config

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.flowautomation.app.storage.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class RemoteConfig(
    val version: String = "3.1.9",
    val selectors: Map<String, String> = emptyMap()
) {
    companion object {
        private const val CONFIG_URL = "https://kylenguyen.me/api/flow-automation-config"
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        private val gson = Gson()

        suspend fun fetch(prefs: AppPreferences): RemoteConfig {
            return try {
                val request = Request.Builder().url(CONFIG_URL).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return loadLocal(prefs)
                    prefs.remoteConfigJson = body
                    gson.fromJson(body, RemoteConfig::class.java)
                } else {
                    loadLocal(prefs)
                }
            } catch (_: Exception) {
                loadLocal(prefs)
            }
        }

        private fun loadLocal(prefs: AppPreferences): RemoteConfig {
            val json = prefs.remoteConfigJson
            return if (json != null) {
                try { gson.fromJson(json, RemoteConfig::class.java) }
                catch (_: Exception) { fallback() }
            } else fallback()
        }

        private fun fallback() = RemoteConfig(
            version = "3.1.9",
            selectors = mapOf(
                "createProjectButton" to "button:has(i:contains(\"add_2\")):first()",
                "disableAgentModeButton" to "div:has(div[data-scroll-state=\"START\"]) button[aria-pressed=\"true\"]",
                "enableAgentModeButton" to "div:has(div[data-scroll-state=\"START\"]) button[aria-pressed=\"false\"]",
                "configureUIModeButton" to "button:has(i:contains(\"settings_2\"))",
                "selectVideoMode" to "div[data-state=\"open\"] div[role=\"tablist\"]:eq(0) button:eq(1)",
                "selectImageMode" to "div[data-state=\"open\"] div[role=\"tablist\"]:eq(0) button:eq(0)",
                "textToVideoModeOption" to "div[data-state=\"open\"] div[role=\"tablist\"]:eq(1) button:eq(1)",
                "imageToVideoModeOption" to "div[data-state=\"open\"] div[role=\"tablist\"]:eq(1) button:eq(0)",
                "outputCountTemplate" to "div[data-state=\"open\"] div[role=\"tablist\"] button:contains(\"{outputCount}\")",
                "aspectRatioTemplate" to "div[data-state=\"open\"] div[role=\"tablist\"] button:has(i:contains(\"{aspectRatio}\"))",
                "modelSelectButton" to "div[data-state=\"open\"] button:has(i:contains(\"arrow_drop_down\"))",
                "modelTemplate" to "div[role=\"menu\"] button:has(span:contains(\"{model}\"))",
                "videoLengthTemplate" to "div[data-orientation=\"horizontal\"] > div > button:contains(\"{videoLength}\")",
                "promptTextarea" to "div[role=\"textbox\"]",
                "submitButton" to "button:has(i:contains(\"arrow_forward\"))",
                "stopButton" to "button:has(i:contains(\"stop\"))",
                "downloadDoneButton" to "button:has(i:contains(\"check\")), header button:last()",
                "outputItems" to "div > div > div[data-tile-id]:has(div)",
                "tileByIdTemplate" to "div[data-tile-id=\"{tileId}\"]:has(div)",
                "quality1080Option" to "button:has(span:contains(\"1080p\"))",
                "quality2KOption" to "button:has(span:contains(\"2K\"))",
                "quality4KOption" to "button:has(span:contains(\"4K\"))"
            )
        )
    }

    fun supportsVersion(version: String): Boolean {
        return version.split(",").map { it.trim() }.filter { it.isNotEmpty() }.contains(version)
    }
}
