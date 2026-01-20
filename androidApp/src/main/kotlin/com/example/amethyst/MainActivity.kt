package com.example.amethyst

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.amethyst.data.FileService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FileService with application context
        FileService.applicationContext = applicationContext

        setContent {
            App()
        }
    }
}
