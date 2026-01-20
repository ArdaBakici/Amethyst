package com.example.amethyst.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amethyst.data.TaskRepository
import com.example.amethyst.model.Priority
import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.tasks
    val isLoading: StateFlow<Boolean> = repository.isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)
    val selectedStatus: StateFlow<TaskStatus?> = _selectedStatus.asStateFlow()

    private val _selectedContext = MutableStateFlow<String?>(null)
    val selectedContext: StateFlow<String?> = _selectedContext.asStateFlow()

    private val _selectedProject = MutableStateFlow<String?>(null)
    val selectedProject: StateFlow<String?> = _selectedProject.asStateFlow()

    private val _showOverdueOnly = MutableStateFlow(false)
    val showOverdueOnly: StateFlow<Boolean> = _showOverdueOnly.asStateFlow()

    private val _filteredTasks = MutableStateFlow<List<Task>>(emptyList())
    val filteredTasks: StateFlow<List<Task>> = _filteredTasks.asStateFlow()

    init {
        loadTasks()
        viewModelScope.launch {
            tasks.collect { allTasks ->
                applyFilters(allTasks)
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            repository.loadTasks()
        }
    }

    fun createTask(
        title: String,
        content: String = "",
        status: TaskStatus = TaskStatus.TODO,
        priority: Priority? = null,
        due: LocalDate? = null,
        scheduled: LocalDate? = null,
        contexts: List<String> = emptyList(),
        projects: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        timeEstimate: Int? = null,
        recurrence: String? = null
    ) {
        viewModelScope.launch {
            val id = generateTaskId(title)
            val task = Task(
                id = id,
                title = title,
                content = content,
                status = status,
                priority = priority,
                due = due,
                scheduled = scheduled,
                contexts = contexts,
                projects = projects,
                tags = tags,
                timeEstimate = timeEstimate,
                recurrence = recurrence
            )
            repository.createTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun startTimer(taskId: String) {
        viewModelScope.launch {
            repository.startTimer(taskId)
        }
    }

    fun stopTimer(taskId: String) {
        viewModelScope.launch {
            repository.stopTimer(taskId)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.completeTask(taskId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters(tasks.value)
    }

    fun setStatusFilter(status: TaskStatus?) {
        _selectedStatus.value = status
        applyFilters(tasks.value)
    }

    fun setContextFilter(context: String?) {
        _selectedContext.value = context
        applyFilters(tasks.value)
    }

    fun setProjectFilter(project: String?) {
        _selectedProject.value = project
        applyFilters(tasks.value)
    }

    fun setShowOverdueOnly(show: Boolean) {
        _showOverdueOnly.value = show
        applyFilters(tasks.value)
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedStatus.value = null
        _selectedContext.value = null
        _selectedProject.value = null
        _showOverdueOnly.value = false
        applyFilters(tasks.value)
    }

    fun getAllContexts(): List<String> {
        return repository.getAllContexts()
    }

    fun getAllProjects(): List<String> {
        return repository.getAllProjects()
    }

    private fun applyFilters(allTasks: List<Task>) {
        var filtered = allTasks

        // Apply search query
        if (_searchQuery.value.isNotBlank()) {
            filtered = repository.searchTasks(_searchQuery.value)
        }

        // Apply status filter
        _selectedStatus.value?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        // Apply context filter
        _selectedContext.value?.let { context ->
            filtered = filtered.filter { it.contexts.contains(context) }
        }

        // Apply project filter
        _selectedProject.value?.let { project ->
            filtered = filtered.filter { it.projects.contains(project) }
        }

        // Apply overdue filter
        if (_showOverdueOnly.value) {
            filtered = filtered.filter { it.isOverdue }
        }

        _filteredTasks.value = filtered
    }

    private fun generateTaskId(title: String): String {
        val sanitized = title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "$sanitized-$timestamp"
    }
}
