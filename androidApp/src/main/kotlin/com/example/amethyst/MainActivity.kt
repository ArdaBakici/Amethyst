package com.example.amethyst

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.amethyst.data.FileService
import com.example.amethyst.data.Preferences
import com.example.amethyst.data.PreferencesStore
import com.example.amethyst.data.TaskRepository
import com.example.amethyst.widget.TaskWidgetProvider
import com.example.amethyst.worker.FileObserverManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var taskIdState = mutableStateOf<String?>(null)
    private var refreshTrigger = mutableStateOf(0)
    
    private val tasksChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println("MainActivity: Received TASKS_CHANGED broadcast")
            // Trigger refresh by incrementing the counter
            refreshTrigger.value++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FileService with application context
        FileService.applicationContext = applicationContext

        // Initialize Preferences with PreferencesStore
        if (!Preferences.isInitialized) {
            Preferences.initialize(PreferencesStore(applicationContext))
        }

        // Set up file observer to watch for changes to task files
        FileObserverManager.setupFileObserver(applicationContext)

        // Register broadcast receiver for file changes
        val filter = IntentFilter("com.example.amethyst.TASKS_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tasksChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(tasksChangedReceiver, filter)
        }

        // Handle intent from widget
        handleWidgetIntent(intent)

        setContent {
            val taskId by taskIdState
            val refresh by refreshTrigger
            App(
                initialTaskId = taskId,
                refreshTrigger = refresh
            )
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(tasksChangedReceiver)
        } catch (e: Exception) {
            println("MainActivity: Error unregistering receiver: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val action = intent?.getStringExtra("action")
        val taskId = intent?.getStringExtra("task_id")
        
        println("MainActivity.handleWidgetIntent: action=$action, taskId=$taskId")
        
        when (action) {
            "complete_task" -> {
                // Handle task completion in background without showing UI
                if (taskId != null) {
                    completeTaskFromWidget(taskId)
                }
                // Finish immediately so the app doesn't come to foreground
                finish()
            }
            "open_task" -> {
                // Navigate to task detail
                taskIdState.value = taskId
            }
            else -> {
                // Legacy support - if no action but taskId exists, open task
                if (taskId != null) {
                    taskIdState.value = taskId
                }
            }
        }
    }

    private fun completeTaskFromWidget(taskId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("MainActivity.completeTaskFromWidget: Completing task $taskId")
                
                val fullTasksPath = Preferences.instance.getFullTasksPath()
                if (fullTasksPath.isNotBlank()) {
                    val fileService = FileService()
                    val repository = TaskRepository(fileService, fullTasksPath)
                    
                    repository.loadTasks()
                    val success = repository.completeTask(taskId)
                    println("MainActivity.completeTaskFromWidget: Task completion ${if (success) "successful" else "failed"}")
                    
                    if (success) {
                        withContext(Dispatchers.Main) {
                            // Force immediate widget refresh with data reload
                            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this@MainActivity)
                            val widgetIds = appWidgetManager.getAppWidgetIds(
                                android.content.ComponentName(this@MainActivity, TaskWidgetProvider::class.java)
                            )
                            
                            // First notify data changed to reload the task list
                            @Suppress("DEPRECATION")
                            appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_task_list)
                            
                            println("MainActivity.completeTaskFromWidget: Widget refresh triggered for ${widgetIds.size} widgets")
                        }
                    }
                }
            } catch (e: Exception) {
                println("MainActivity.completeTaskFromWidget: Error - ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        println("MainActivity.onResume: Refreshing widgets and triggering app refresh")
        // Refresh widgets when app is opened
        TaskWidgetProvider.updateAllWidgets(this)
        // Trigger app refresh
        refreshTrigger.value++
    }
}
