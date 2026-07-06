package com.flowautomation.app

import android.app.Application
import com.flowautomation.app.storage.AppPreferences

class FlowApp : Application() {
    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
    }
}
