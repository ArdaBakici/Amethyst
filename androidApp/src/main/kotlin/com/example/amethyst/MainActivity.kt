package com.example.amethyst

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.amethyst.data.FileService
import com.example.amethyst.data.Preferences
import com.example.amethyst.data.PreferencesStore
import com.example.amethyst.widget.TaskWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FileService with application context
        FileService.applicationContext = applicationContext

        // Initialize Preferences with PreferencesStore
        if (!Preferences.Companion::instance.isInitialized) {
            Preferences.initialize(PreferencesStore(applicationContext))
        }

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh widgets when app is opened
        TaskWidgetProvider.updateAllWidgets(this)
    }
}
