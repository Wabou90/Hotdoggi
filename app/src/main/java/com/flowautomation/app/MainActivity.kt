package com.flowautomation.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.flowautomation.app.automation.AutomationState
import com.flowautomation.app.ui.MainScreen
import com.flowautomation.app.ui.theme.FlowAutomationTheme

class MainActivity : ComponentActivity() {
    private lateinit var state: AutomationState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        state = ViewModelProvider(this)[AutomationState::class.java]
        val prefs = (application as FlowApp).preferences
        state.init(prefs)

        setContent {
            FlowAutomationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        state = state,
                        prefs = prefs,
                        context = this@MainActivity
                    )
                }
            }
        }
    }
}
