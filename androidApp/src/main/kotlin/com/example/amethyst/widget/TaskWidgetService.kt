package com.example.amethyst.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.amethyst.R
import com.example.amethyst.data.FileService
import com.example.amethyst.data.Preferences
import com.example.amethyst.data.TaskSerializer
import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
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
        loadTasks()
    }

    override fun onDataSetChanged() {
        loadTasks()
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        if (position < tasks.size) {
            val task = tasks[position]

            // Set task title
            views.setTextViewText(R.id.task_title, task.title)

            // Set checkbox state
            views.setBoolean(R.id.task_checkbox, "setChecked", task.status == TaskStatus.DONE)

            // Set task details
            val details = buildString {
                task.priority?.let { append("${it.displayName} • ") }
                task.due?.let { append("Due: $it • ") }
                if (task.contexts.isNotEmpty()) {
                    append(task.contexts.joinToString(", ") { "@$it" })
                }
            }.trimEnd('•', ' ')

            views.setTextViewText(R.id.task_details, details.ifBlank { task.status.value })

            // Set click intent
            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.task_checkbox, fillInIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadTasks() {
        runBlocking {
            try {
                // Initialize FileService context if not already set
                FileService.applicationContext = context

                val vaultPath = Preferences.instance.vaultPath.value
                if (vaultPath.isNotBlank()) {
                    val fileService = FileService()
                    val taskFiles = fileService.listTaskFiles(vaultPath)

                    tasks = taskFiles.mapNotNull { filePath ->
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tasks = emptyList()
            }
        }
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
