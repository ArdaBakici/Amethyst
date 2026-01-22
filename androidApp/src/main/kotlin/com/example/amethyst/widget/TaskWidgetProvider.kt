package com.example.amethyst.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import com.example.amethyst.MainActivity
import com.example.amethyst.R
import com.example.amethyst.data.FileService
import com.example.amethyst.data.Preferences
import com.example.amethyst.data.PreferencesStore
import com.example.amethyst.data.TaskRepository
import com.example.amethyst.data.ThemeMode
import com.example.amethyst.worker.FileObserverManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget instance created
        println("TaskWidgetProvider: First widget added, setting up file observer")
        
        // Initialize Preferences if needed
        if (!Preferences.isInitialized) {
            Preferences.initialize(PreferencesStore(context))
        }
        
        // Set up file observer to monitor changes
        FileObserverManager.setupFileObserver(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed
        println("TaskWidgetProvider: Last widget removed, canceling file observer")
        FileObserverManager.cancelFileObserver(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            "com.example.amethyst.THEME_CHANGED" -> {
                // Handle theme change broadcast
                println("TaskWidgetProvider: Received THEME_CHANGED broadcast")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, TaskWidgetProvider::class.java)
                )
                if (appWidgetIds.isNotEmpty()) {
                    println("TaskWidgetProvider: Updating ${appWidgetIds.size} widgets for theme change")
                    onUpdate(context, appWidgetManager, appWidgetIds)
                    // Refresh widget data to apply new theme colors to task items
                    @Suppress("DEPRECATION")
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list)
                }
            }
            "com.example.amethyst.COMPLETE_TASK" -> {
                // Handle task completion from widget
                val taskId = intent.getStringExtra("task_id")
                println("TaskWidgetProvider: Received COMPLETE_TASK for taskId: $taskId")
                if (taskId != null) {
                    completeTaskFromWidget(context, taskId)
                }
            }
            "com.example.amethyst.REFRESH_WIDGET" -> {
                // Handle manual refresh request
                println("TaskWidgetProvider: Received REFRESH_WIDGET broadcast")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, TaskWidgetProvider::class.java)
                )
                if (appWidgetIds.isNotEmpty()) {
                    println("TaskWidgetProvider: Refreshing ${appWidgetIds.size} widgets")
                    @Suppress("DEPRECATION")
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list)
                }
            }
        }
    }

    companion object {
        private fun isDarkMode(context: Context): Boolean {
            // Initialize Preferences if needed
            if (!Preferences.isInitialized) {
                Preferences.initialize(PreferencesStore(context))
            }

            val themeMode = Preferences.instance.themeMode.value

            return when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> {
                    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    uiMode == Configuration.UI_MODE_NIGHT_YES
                }
            }
        }

        /**
         * Sets up widget using service-based RemoteViewsService approach.
         * Uses service for reliable widget updates across all Android versions.
         */
        @Suppress("DEPRECATION")
        private fun setupServiceBasedAdapter(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            // IMPORTANT: Intent must be unique per widget instance
            val listIntent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                // Set unique data to ensure Android creates separate service instances for each widget
                data = android.net.Uri.parse("content://widget/$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_task_list, listIntent)
            println("TaskWidgetProvider.setupServiceBasedAdapter: Set remote adapter with unique intent for widget $appWidgetId")
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            println("TaskWidgetProvider.updateAppWidget: Updating widget $appWidgetId")

            val isDark = isDarkMode(context)
            println("TaskWidgetProvider.updateAppWidget: Dark mode = $isDark")

            val views = RemoteViews(context.packageName, R.layout.widget_task_list)
            println("TaskWidgetProvider.updateAppWidget: Created RemoteViews for ${context.packageName}")

            // Apply theme colors
            val backgroundColor = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
            val titleColor = if (isDark) 0xFFE0E0E0.toInt() else 0xFF000000.toInt()
            val emptyTextColor = if (isDark) 0xFF888888.toInt() else 0xFF666666.toInt()
            val dividerColor = if (isDark) 0xFF444444.toInt() else 0xFFCCCCCC.toInt()

            views.setInt(R.id.widget_background, "setBackgroundColor", backgroundColor)
            views.setTextColor(R.id.widget_title, titleColor)
            views.setTextColor(R.id.widget_empty_view, emptyTextColor)
            println("TaskWidgetProvider.updateAppWidget: Applied theme colors")

            // Set up the intent to launch MainActivity when widget is clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            println("TaskWidgetProvider.updateAppWidget: Set title click listener")

            // Set up refresh button click listener
            val refreshIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = "com.example.amethyst.REFRESH_WIDGET"
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
            println("TaskWidgetProvider.updateAppWidget: Set refresh button click listener")

            // Set up the intent for the task list using service-based approach
            // Note: Using service-based adapter for all Android versions for reliability
            // RemoteCollectionItems API (Android 12+) has compatibility issues
            println("TaskWidgetProvider.updateAppWidget: Using service-based adapter")
            setupServiceBasedAdapter(context, appWidgetManager, appWidgetId, views)

            // Service-based adapter: set up click listener template for list items
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, clickPendingIntent)
            println("TaskWidgetProvider.updateAppWidget: Set pending intent template")

            // Set empty view
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)
            println("TaskWidgetProvider.updateAppWidget: Set empty view")

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            println("TaskWidgetProvider.updateAppWidget: Called updateAppWidget")

            // Notify the widget to refresh its data
            // This triggers onDataSetChanged() in the RemoteViewsFactory
            @Suppress("DEPRECATION")
            appWidgetManager.notifyAppWidgetViewDataChanged(intArrayOf(appWidgetId), R.id.widget_task_list)
            println("TaskWidgetProvider.updateAppWidget: Notified widget data changed")
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TaskWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun completeTaskFromWidget(context: Context, taskId: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    println("TaskWidgetProvider.completeTaskFromWidget: Starting task completion for $taskId")
                    
                    // Initialize FileService and Preferences if needed
                    FileService.applicationContext = context
                    if (!Preferences.isInitialized) {
                        Preferences.initialize(PreferencesStore(context))
                    }

                    val fullTasksPath = Preferences.instance.getFullTasksPath()
                    println("TaskWidgetProvider.completeTaskFromWidget: fullTasksPath = $fullTasksPath")
                    
                    if (fullTasksPath.isNotBlank()) {
                        val fileService = FileService()
                        val repository = TaskRepository(fileService, fullTasksPath)
                        
                        // Load tasks first to ensure repository has current data
                        repository.loadTasks()
                        println("TaskWidgetProvider.completeTaskFromWidget: Tasks loaded")
                        
                        // Complete the task
                        val success = repository.completeTask(taskId)
                        println("TaskWidgetProvider.completeTaskFromWidget: Task completion ${if (success) "successful" else "failed"} for $taskId")
                        
                        if (success) {
                            // Refresh all widgets
                            withContext(Dispatchers.Main) {
                                println("TaskWidgetProvider.completeTaskFromWidget: Refreshing widgets")
                                updateAllWidgets(context)
                            }
                        } else {
                            println("TaskWidgetProvider.completeTaskFromWidget: Task completion failed, not refreshing widgets")
                        }
                    } else {
                        println("TaskWidgetProvider.completeTaskFromWidget: fullTasksPath is blank")
                    }
                } catch (e: Exception) {
                    println("TaskWidgetProvider.completeTaskFromWidget: Error completing task: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}
