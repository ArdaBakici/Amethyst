package com.example.amethyst.data

import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import com.example.amethyst.model.TimeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TaskRepository(
    private val fileService: FileService,
    private val vaultPath: String
) {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadTasks() {
        _isLoading.value = true
        try {
            val taskFiles = fileService.listTaskFiles(vaultPath)
            val loadedTasks = taskFiles.mapNotNull { filePath ->
                val filename = filePath.substringAfterLast('/')
                    .substringAfterLast('\\')
                fileService.readFile(filePath)?.let { content ->
                    TaskSerializer.parseTask(filename, content)
                }
            }
            _tasks.value = loadedTasks
        } catch (e: Exception) {
            println("Error loading tasks: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getTask(id: String): Task? {
        return _tasks.value.find { it.id == id }
    }

    suspend fun createTask(task: Task): Boolean {
        val now = Clock.System.now()
        val newTask = task.copy(
            createdAt = task.createdAt ?: now,
            modifiedAt = now
        )

        val filePath = "$vaultPath/${newTask.id}.md"
        val content = TaskSerializer.serializeTask(newTask)

        return if (fileService.writeFile(filePath, content)) {
            _tasks.value = _tasks.value + newTask
            true
        } else {
            false
        }
    }

    suspend fun updateTask(task: Task): Boolean {
        val updatedTask = task.copy(modifiedAt = Clock.System.now())
        val filePath = "$vaultPath/${updatedTask.id}.md"
        val content = TaskSerializer.serializeTask(updatedTask)

        return if (fileService.writeFile(filePath, content)) {
            _tasks.value = _tasks.value.map { if (it.id == updatedTask.id) updatedTask else it }
            true
        } else {
            false
        }
    }

    suspend fun deleteTask(id: String): Boolean {
        val filePath = "$vaultPath/$id.md"
        return if (fileService.deleteFile(filePath)) {
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
}
