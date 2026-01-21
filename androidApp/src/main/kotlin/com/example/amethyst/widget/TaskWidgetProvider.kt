package com.example.amethyst.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.amethyst.MainActivity
import com.example.amethyst.R
import com.example.amethyst.data.FileService
import com.example.amethyst.data.Preferences
import com.example.amethyst.data.PreferencesStore
import com.example.amethyst.data.TaskSerializer
import com.example.amethyst.data.ThemeMode
import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import kotlinx.coroutines.runBlocking
import java.net.URLDecoder

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
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed
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
         * Sets up widget using RemoteCollectionItems API (Android 12+).
         * Loads tasks and builds RemoteViews collection directly.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        private fun setupRemoteCollectionItems(
            context: Context,
            views: RemoteViews,
            isDark: Boolean
        ) {
            try {
                // Initialize services
                FileService.applicationContext = context
                if (!Preferences.isInitialized) {
                    Preferences.initialize(PreferencesStore(context))
                }

                // Load tasks
                val tasks = loadTasksForWidget(context)
                println("TaskWidgetProvider.setupRemoteCollectionItems: Loaded ${tasks.size} tasks")

                // Build RemoteViews for each task
                val itemBuilder = RemoteViews.RemoteCollectionItems.Builder()

                tasks.forEachIndexed { index, task ->
                    try {
                        val itemView = createTaskItemView(context, task, isDark)
                        itemBuilder.addItem(index.toLong(), itemView)
                        println("TaskWidgetProvider.setupRemoteCollectionItems: Added item $index: ${task.title}")
                    } catch (e: Exception) {
                        println("TaskWidgetProvider.setupRemoteCollectionItems: Error creating item $index: ${e.message}")
                        e.printStackTrace()
                    }
                }

                val collectionItems = itemBuilder
                    .setHasStableIds(true)
                    .setViewTypeCount(1)
                    .build()

                println("TaskWidgetProvider.setupRemoteCollectionItems: Built collection with ${tasks.size} items")
                views.setRemoteAdapter(R.id.widget_task_list, collectionItems)
                println("TaskWidgetProvider.setupRemoteCollectionItems: Set collection to widget")
            } catch (e: Exception) {
                println("TaskWidgetProvider.setupRemoteCollectionItems: Fatal error: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }

        /**
         * Sets up widget using service-based RemoteViewsService approach.
         * Used for Android versions prior to 12 where RemoteCollectionItems is not available.
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

            // Notify that data has changed (triggers onDataSetChanged)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
            println("TaskWidgetProvider.setupServiceBasedAdapter: Called notifyAppWidgetViewDataChanged")
        }

        private fun loadTasksForWidget(context: Context): List<Task> {
            return runBlocking {
                try {
                    val fullTasksPath = Preferences.instance.getFullTasksPath()
                    if (fullTasksPath.isBlank()) {
                        println("TaskWidgetProvider.loadTasksForWidget: No tasks path configured")
                        return@runBlocking emptyList()
                    }

                    val fileService = FileService()
                    val taskFiles = fileService.listTaskFiles(fullTasksPath)
                    println("TaskWidgetProvider.loadTasksForWidget: Found ${taskFiles.size} task files")

                    taskFiles.mapNotNull { filePath ->
                        val filename = extractFilename(filePath)
                        fileService.readFile(filePath)?.let { content ->
                            TaskSerializer.parseTask(filename, content)
                        }
                    }.filter { it.status != TaskStatus.DONE }
                        .sortedWith(compareBy(
                            { it.priority?.ordinal ?: Int.MAX_VALUE },
                            { it.due },
                            { it.title }
                        ))
                } catch (e: Exception) {
                    println("TaskWidgetProvider.loadTasksForWidget: Error loading tasks: ${e.message}")
                    e.printStackTrace()
                    emptyList()
                }
            }
        }

        private fun createTaskItemView(context: Context, task: Task, isDark: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_task_item)

            // Apply theme colors
            val itemBackgroundColor = if (isDark) 0xFF2A2A2A.toInt() else 0xFFFFFFFF.toInt()
            val titleColor = if (isDark) 0xFFE0E0E0.toInt() else 0xFF000000.toInt()
            val detailsColor = if (isDark) 0xFF999999.toInt() else 0xFF666666.toInt()

            views.setInt(R.id.widget_task_item_background, "setBackgroundColor", itemBackgroundColor)

            // Set task title
            views.setTextViewText(R.id.task_title, task.title)
            views.setTextColor(R.id.task_title, titleColor)

            // Set task details
            val details = buildString {
                task.priority?.let { append("${it.displayName} • ") }
                task.due?.let { append("Due: $it • ") }
                if (task.contexts.isNotEmpty()) {
                    append(task.contexts.joinToString(", ") { "@$it" })
                }
            }.trimEnd('•', ' ')

            views.setTextViewText(R.id.task_details, details.ifBlank { task.status.value })
            views.setTextColor(R.id.task_details, detailsColor)

            // Set checkbox state
            views.setBoolean(R.id.task_checkbox, "setChecked", task.status == TaskStatus.DONE)

            // Don't set click handlers - RemoteCollectionItems doesn't support item-level clicks
            // Clicks will be handled by tapping the widget title which opens the app

            return views
        }

        private fun extractFilename(path: String): String {
            val rawFilename = path.substringAfterLast('/')
                .substringAfterLast('\\')

            val decoded = try {
                URLDecoder.decode(rawFilename, "UTF-8")
            } catch (e: Exception) {
                rawFilename
            }

            // For Android content URI document IDs like "primary:Sync/Vault/task.md"
            // extract just the filename from the full document path
            return if (decoded.contains('/') || decoded.contains('\\')) {
                decoded.substringAfterLast('/').substringAfterLast('\\')
            } else {
                decoded
            }
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

            // Set up the intent for the task list
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+): Use new RemoteCollectionItems API
                println("TaskWidgetProvider.updateAppWidget: Using RemoteCollectionItems API for Android 12+")
                setupRemoteCollectionItems(context, views, isDark)
                // RemoteCollectionItems: each item has its own PendingIntent, no template needed
            } else {
                // Pre-Android 12: Use service-based approach (deprecated but necessary for older versions)
                println("TaskWidgetProvider.updateAppWidget: Using service-based adapter for pre-Android 12")
                setupServiceBasedAdapter(context, appWidgetManager, appWidgetId, views)

                // Service-based adapter: set up click listener template for list items
                val clickIntent = Intent(context, MainActivity::class.java)
                val clickPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setPendingIntentTemplate(R.id.widget_task_list, clickPendingIntent)
                println("TaskWidgetProvider.updateAppWidget: Set pending intent template")
            }

            // Set empty view
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)
            println("TaskWidgetProvider.updateAppWidget: Set empty view")

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            println("TaskWidgetProvider.updateAppWidget: Called updateAppWidget")
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
    }
}
