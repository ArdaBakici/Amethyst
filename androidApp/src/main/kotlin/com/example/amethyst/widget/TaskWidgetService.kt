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

    override fun onCreate() {
        println("TaskWidgetService.onCreate: Creating widget factory - doing minimal work")
        // Don't load tasks here - let onDataSetChanged do it
    }

    override fun onDataSetChanged() {
        println("TaskWidgetService.onDataSetChanged: Loading/refreshing data")
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
        println("TaskWidgetService.getViewAt: Creating view for position $position, tasks.size=${tasks.size}")

        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        if (position >= 0 && position < tasks.size) {
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

            // Set checkbox state
            views.setBoolean(R.id.task_checkbox, "setChecked", task.status == TaskStatus.DONE)

            // Set click intent to open the app when task is tapped
            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.widget_task_item_background, fillInIntent)

            println("TaskWidgetService.getViewAt: Set task '${task.title}' at position $position")
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        println("TaskWidgetService.getLoadingView: Returning null (using default)")
        return null
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
