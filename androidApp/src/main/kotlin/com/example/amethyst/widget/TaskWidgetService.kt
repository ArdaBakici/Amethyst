package com.example.amethyst.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.amethyst.R
import com.example.amethyst.data.FileService
import com.example.amethyst.data.Preferences
import com.example.amethyst.data.PreferencesStore
import com.example.amethyst.data.TaskSerializer
import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.URLDecoder

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetViewsFactory(this.applicationContext)
    }
}

class TaskWidgetViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var tasks = listOf<Task>()
    private var isDarkMode: Boolean = false

    override fun onCreate() {
        println("TaskWidgetService.onCreate: Creating widget factory and loading initial tasks")
        updateThemeMode()
        // Load tasks immediately so they're available when getCount() is called
        // This ensures the widget shows tasks on first load
        loadTasks()
        println("TaskWidgetService.onCreate: Loaded ${tasks.size} tasks in onCreate")
    }

    override fun onDataSetChanged() {
        println("TaskWidgetService.onDataSetChanged: Loading/refreshing data")
        updateThemeMode()
        loadTasks()
    }

    override fun onDestroy() {
        println("TaskWidgetService.onDestroy: Destroying widget factory")
        tasks = emptyList()
    }

    override fun getCount(): Int {
        println("TaskWidgetService.getCount: Returning ${tasks.size} tasks")
        return tasks.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        try {
            if (position < 0 || position >= tasks.size) {
                // Return empty view with default text
                views.setTextViewText(R.id.task_title, "")
                views.setTextViewText(R.id.task_details, "")
                return views
            }

            val task = tasks[position]

            // Set task title
            views.setTextViewText(R.id.task_title, task.title)

            // Set task details
            val details = buildString {
                task.priority?.let { append("${it.displayName} • ") }
                task.due?.let { append("Due: $it • ") }
                if (task.contexts.isNotEmpty()) {
                    append(task.contexts.joinToString(", ") { "@$it" })
                }
            }.trimEnd('•', ' ')

            views.setTextViewText(R.id.task_details, details.ifBlank { task.status.value })

            // Set checkbox state using ImageView (CheckBox not supported in RemoteViews)
            val checkboxDrawable = if (task.status == TaskStatus.DONE) {
                android.R.drawable.checkbox_on_background
            } else {
                android.R.drawable.checkbox_off_background
            }
            views.setImageViewResource(R.id.task_checkbox, checkboxDrawable)

            // Apply theme colors
            val backgroundColor = if (isDarkMode) 0xFF2A2A2A.toInt() else 0xFFFFFFFF.toInt()
            val titleColor = if (isDarkMode) 0xFFE0E0E0.toInt() else 0xFF000000.toInt()
            val detailsColor = if (isDarkMode) 0xFF999999.toInt() else 0xFF666666.toInt()

            views.setInt(R.id.widget_task_item_background, "setBackgroundColor", backgroundColor)
            views.setTextColor(R.id.task_title, titleColor)
            views.setTextColor(R.id.task_details, detailsColor)

            // Set click intent
            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.widget_task_item_background, fillInIntent)
        } catch (e: Exception) {
            println("TaskWidgetService.getViewAt: ERROR creating view: ${e.message}")
            e.printStackTrace()
            views.setTextViewText(R.id.task_title, "Error loading task")
            views.setTextViewText(R.id.task_details, "")
        }

        return views
    }

    override fun getLoadingView(): RemoteViews {
        println("TaskWidgetService.getLoadingView: Returning custom loading view")
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        views.setTextViewText(R.id.task_title, "Loading tasks...")
        views.setTextViewText(R.id.task_details, "")
        views.setImageViewResource(R.id.task_checkbox, android.R.drawable.checkbox_off_background)

        // Apply theme colors
        val backgroundColor = if (isDarkMode) 0xFF2A2A2A.toInt() else 0xFFFFFFFF.toInt()
        val titleColor = if (isDarkMode) 0xFFE0E0E0.toInt() else 0xFF000000.toInt()

        views.setInt(R.id.widget_task_item_background, "setBackgroundColor", backgroundColor)
        views.setTextColor(R.id.task_title, titleColor)

        return views
    }

    override fun getViewTypeCount(): Int {
        println("TaskWidgetService.getViewTypeCount: Returning 1")
        return 1
    }

    override fun getItemId(position: Int): Long {
        println("TaskWidgetService.getItemId: Returning $position")
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        println("TaskWidgetService.hasStableIds: Returning true")
        return true
    }

    private fun loadTasks() {
        println("TaskWidgetService.loadTasks: Starting to load tasks")
        try {
            // Initialize FileService context if not already set
            FileService.applicationContext = context
            println("TaskWidgetService.loadTasks: FileService context set")

            // Initialize Preferences if not already set
            if (!Preferences.isInitialized) {
                println("TaskWidgetService.loadTasks: Initializing Preferences")
                Preferences.initialize(PreferencesStore(context))
            } else {
                println("TaskWidgetService.loadTasks: Preferences already initialized")
            }

            val fullTasksPath = Preferences.instance.getFullTasksPath()
            println("TaskWidgetService.loadTasks: fullTasksPath = '$fullTasksPath'")

            if (fullTasksPath.isNotBlank()) {
                // Use runBlocking with IO dispatcher since onDataSetChanged is on a background thread
                // IO dispatcher is needed to match FileService's withContext(Dispatchers.IO)
                runBlocking(Dispatchers.IO) {
                    val fileService = FileService()
                    val taskFiles = fileService.listTaskFiles(fullTasksPath)
                    println("TaskWidgetService.loadTasks: Found ${taskFiles.size} task files")

                    tasks = taskFiles.mapNotNull { filePath ->
                        println("TaskWidgetService.loadTasks: Processing file: $filePath")
                        val filename = extractFilename(filePath)
                        fileService.readFile(filePath)?.let { content ->
                            println("TaskWidgetService.loadTasks: Read content for $filename (${content.length} bytes)")
                            TaskSerializer.parseTask(filename, content)?.also { task ->
                                println("TaskWidgetService.loadTasks: Parsed task: ${task.title}")
                            }
                        } ?: run {
                            println("TaskWidgetService.loadTasks: Failed to read file: $filePath")
                            null
                        }
                    }.filter { it.status != TaskStatus.DONE }
                        .sortedWith(compareBy(
                            { it.priority?.ordinal ?: Int.MAX_VALUE },
                            { it.due },
                            { it.title }
                        ))

                    println("TaskWidgetService.loadTasks: Loaded ${tasks.size} active tasks")
                }
            } else {
                println("TaskWidgetService.loadTasks: fullTasksPath is blank, no tasks to load")
                tasks = emptyList()
            }
        } catch (e: Exception) {
            println("TaskWidgetService.loadTasks: Exception occurred: ${e.message}")
            e.printStackTrace()
            tasks = emptyList()
        }
        println("TaskWidgetService.loadTasks: Finished loading, tasks count = ${tasks.size}")
    }

    private fun updateThemeMode() {
        // Read theme mode directly from SharedPreferences (fast, non-blocking)
        val sharedPrefs = context.getSharedPreferences("amethyst_preferences", Context.MODE_PRIVATE)
        val themeModeOrdinal = sharedPrefs.getInt("theme_mode", 2) // 2 = SYSTEM default

        isDarkMode = when (themeModeOrdinal) {
            0 -> false // LIGHT
            1 -> true  // DARK
            2 -> {     // SYSTEM
                val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            else -> false
        }
        println("TaskWidgetService.updateThemeMode: isDarkMode = $isDarkMode")
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
}
