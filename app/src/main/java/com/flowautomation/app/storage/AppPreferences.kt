package com.flowautomation.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.flowautomation.app.model.FlowConfig
import com.flowautomation.app.model.MediaMode
import com.flowautomation.app.model.VideoMode
import com.flowautomation.app.model.ImageMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flow_automation", Context.MODE_PRIVATE)
    private val gson = Gson()

    var folderName: String
        get() = prefs.getString("folder_name", "") ?: ""
        set(v) = prefs.edit().putString("folder_name", v).apply()

    var prefix: String
        get() = prefs.getString("prefix", "") ?: ""
        set(v) = prefs.edit().putString("prefix", v).apply()

    var autoChangeFileName: Boolean
        get() = prefs.getBoolean("auto_rename", true)
        set(v) = prefs.edit().putBoolean("auto_rename", v).apply()

    var remoteConfigJson: String?
        get() = prefs.getString("remote_config", null)
        set(v) = prefs.edit().putString("remote_config", v).apply()

    var lastConfig: FlowConfig
        get() {
            val json = prefs.getString("flow_config", null) ?: return FlowConfig()
            return try {
                gson.fromJson(json, FlowConfig::class.java)
            } catch (_: Exception) { FlowConfig() }
        }
        set(v) = prefs.edit().putString("flow_config", gson.toJson(v)).apply()

    var savedPrompts: List<String>
        get() {
            val json = prefs.getString("saved_prompts", null) ?: return emptyList()
            return try {
                gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) { emptyList() }
        }
        set(v) = prefs.edit().putString("saved_prompts", gson.toJson(v)).apply()

    fun clear() = prefs.edit().clear().apply()
}
