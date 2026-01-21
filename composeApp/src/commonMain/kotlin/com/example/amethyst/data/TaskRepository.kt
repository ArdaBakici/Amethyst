package com.example.amethyst.data

import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import com.example.amethyst.model.TimeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.net.URLDecoder

class TaskRepository(
    private val fileService: FileService,
    private val vaultPath: String
) {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Map task IDs to their actual file URIs/paths
    private val taskFileMap = mutableMapOf<String, String>()

    suspend fun loadTasks() {
        _isLoading.value = true
        try {
            println("TaskRepository.loadTasks: Loading tasks from directory: $vaultPath")
            val taskFiles = fileService.listTaskFiles(vaultPath)
            println("TaskRepository.loadTasks: Found ${taskFiles.size} task files")

            taskFiles.forEachIndexed { index, filePath ->
                println("TaskRepository.loadTasks: Task file ${index + 1}: $filePath")
            }

            val loadedTasks = taskFiles.mapNotNull { filePath ->
                val filename = extractFilename(filePath)
                println("TaskRepository.loadTasks: Reading file: $filename from path: $filePath")
                fileService.readFile(filePath)?.let { content ->
                    println("TaskRepository.loadTasks: Successfully read ${content.length} bytes from $filename")
                    TaskSerializer.parseTask(filename, content)?.also { task ->
                        // Store the mapping between task ID and actual file URI
                        taskFileMap[task.id] = filePath
                        println("TaskRepository.loadTasks: Parsed task: ${task.title} (id: ${task.id})")
                    }
                } ?: run {
                    println("TaskRepository.loadTasks: Failed to read file: $filePath")
                    null
                }
            }
            _tasks.value = loadedTasks
            println("TaskRepository.loadTasks: Loaded ${loadedTasks.size} tasks successfully")
        } catch (e: Exception) {
            println("TaskRepository.loadTasks: Error loading tasks: ${e.message}")
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getTask(id: String): Task? {
        return _tasks.value.find { it.id == id }
    }

    suspend fun createTask(task: Task): Boolean {
        println("TaskRepository.createTask: Creating task: ${task.title}")
        println("TaskRepository.createTask: Vault path: $vaultPath")

        val now = Clock.System.now()
        val newTask = task.copy(
            createdAt = task.createdAt ?: now,
            modifiedAt = now
        )

        val filePath = constructFilePath(newTask.id)
        println("TaskRepository.createTask: Constructed file path: $filePath")
        println("TaskRepository.createTask: Task ID: ${newTask.id}")

        val content = TaskSerializer.serializeTask(newTask)
        println("TaskRepository.createTask: Serialized content length: ${content.length} bytes")

        val success = fileService.writeFile(filePath, content)
        return if (success) {
            println("TaskRepository.createTask: Successfully wrote task to: $filePath")
            taskFileMap[newTask.id] = filePath
            _tasks.value = _tasks.value + newTask
            println("TaskRepository.createTask: Task added to repository. Total tasks: ${_tasks.value.size}")
            true
        } else {
            println("TaskRepository.createTask: Failed to write task to: $filePath")
            false
        }
    }

    suspend fun updateTask(task: Task): Boolean {
        println("TaskRepository.updateTask: Updating task: ${task.title}")
        println("TaskRepository.updateTask: Task ID: ${task.id}")

        val updatedTask = task.copy(modifiedAt = Clock.System.now())

        // Use the stored file path/URI for this task
        val filePath = taskFileMap[updatedTask.id] ?: constructFilePath(updatedTask.id)
        val useStoredPath = taskFileMap.containsKey(updatedTask.id)

        println("TaskRepository.updateTask: Using ${if (useStoredPath) "stored" else "constructed"} file path: $filePath")
        if (useStoredPath) {
            println("TaskRepository.updateTask: Original file path from map: ${taskFileMap[updatedTask.id]}")
        }

        val content = TaskSerializer.serializeTask(updatedTask)
        println("TaskRepository.updateTask: Serialized content length: ${content.length} bytes")

        val success = fileService.writeFile(filePath, content)
        return if (success) {
            println("TaskRepository.updateTask: Successfully updated task at: $filePath")
            _tasks.value = _tasks.value.map { if (it.id == updatedTask.id) updatedTask else it }
            println("TaskRepository.updateTask: Task updated in repository")
            true
        } else {
            println("TaskRepository.updateTask: Failed to update task at: $filePath")
            false
        }
    }

    suspend fun deleteTask(id: String): Boolean {
        val filePath = taskFileMap[id] ?: constructFilePath(id)
        return if (fileService.deleteFile(filePath)) {
            taskFileMap.remove(id)
            _tasks.value = _tasks.value.filter { it.id != id }
            true
        } else {
            false
        }
    }

    suspend fun startTimer(taskId: String): Boolean {
        val task = getTask(taskId) ?: return false
        if (task.isTimerRunning) return false

        val newEntry = TimeEntry(startTime = Clock.System.now())
        val updatedTask = task.copy(
            timeEntries = task.timeEntries + newEntry
        )

        return updateTask(updatedTask)
    }

    suspend fun stopTimer(taskId: String): Boolean {
        val task = getTask(taskId) ?: return false
        val activeEntry = task.activeTimeEntry ?: return false

        val updatedEntries = task.timeEntries.dropLast(1) + activeEntry.copy(
            endTime = Clock.System.now()
        )

        val updatedTask = task.copy(timeEntries = updatedEntries)
        return updateTask(updatedTask)
    }

    suspend fun completeTask(taskId: String): Boolean {
        val task = getTask(taskId) ?: return false
        val updatedTask = task.copy(
            status = TaskStatus.DONE,
            completedDate = kotlinx.datetime.Clock.System.now().toString().take(10)
                .let { kotlinx.datetime.LocalDate.parse(it) }
        )
        return updateTask(updatedTask)
    }

    fun searchTasks(query: String): List<Task> {
        if (query.isBlank()) return _tasks.value

        return _tasks.value.filter { task ->
            task.title.contains(query, ignoreCase = true) ||
                    task.content.contains(query, ignoreCase = true) ||
                    task.contexts.any { it.contains(query, ignoreCase = true) } ||
                    task.projects.any { it.contains(query, ignoreCase = true) } ||
                    task.tags.any { it.contains(query, ignoreCase = true) }
        }
    }

    fun filterByStatus(status: TaskStatus): List<Task> {
        return _tasks.value.filter { it.status == status }
    }

    fun filterOverdue(): List<Task> {
        return _tasks.value.filter { it.isOverdue }
    }

    fun filterByContext(context: String): List<Task> {
        return _tasks.value.filter { it.contexts.contains(context) }
    }

    fun filterByProject(project: String): List<Task> {
        return _tasks.value.filter { it.projects.contains(project) }
    }

    fun getAllContexts(): List<String> {
        return _tasks.value.flatMap { it.contexts }.distinct().sorted()
    }

    fun getAllProjects(): List<String> {
        return _tasks.value.flatMap { it.projects }.distinct().sorted()
    }

    /**
     * Extracts filename from a file path or content URI
     */
    private fun extractFilename(path: String): String {
        // For content URIs, the filename might be URL-encoded
        val rawFilename = path.substringAfterLast('/')
            .substringAfterLast('\\')

        // Decode URL-encoded characters (like %3A for :)
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

    /**
     * Constructs the file path/URI for a task
     */
    private fun constructFilePath(taskId: String): String {
        return if (vaultPath.startsWith("content://")) {
            // For content URIs, we need to use the FileService to create the file
            // This is a fallback - normally we should have the URI in taskFileMap
            "$vaultPath/${taskId}.md"
        } else {
            "$vaultPath/${taskId}.md"
        }
    }
}
