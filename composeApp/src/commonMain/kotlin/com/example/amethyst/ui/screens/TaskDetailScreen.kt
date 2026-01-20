package com.example.amethyst.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.amethyst.model.Priority
import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import com.example.amethyst.viewmodel.TaskViewModel
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskViewModel,
    taskId: String?,
    onNavigateBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val existingTask = remember(tasks, taskId) {
        taskId?.let { id -> tasks.find { it.id == id } }
    }

    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var content by remember(existingTask) { mutableStateOf(existingTask?.content ?: "") }
    var status by remember(existingTask) { mutableStateOf(existingTask?.status ?: TaskStatus.TODO) }
    var priority by remember(existingTask) { mutableStateOf(existingTask?.priority) }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.due?.toString() ?: "") }
    var scheduledDate by remember(existingTask) { mutableStateOf(existingTask?.scheduled?.toString() ?: "") }
    var contexts by remember(existingTask) { mutableStateOf(existingTask?.contexts?.joinToString(", ") ?: "") }
    var projects by remember(existingTask) { mutableStateOf(existingTask?.projects?.joinToString(", ") ?: "") }
    var tags by remember(existingTask) { mutableStateOf(existingTask?.tags?.joinToString(", ") ?: "") }
    var timeEstimate by remember(existingTask) { mutableStateOf(existingTask?.timeEstimate?.toString() ?: "") }
    var recurrence by remember(existingTask) { mutableStateOf(existingTask?.recurrence ?: "") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingTask != null) "Edit Task" else "New Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (existingTask != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                    IconButton(onClick = {
                        if (title.isNotBlank()) {
                            if (existingTask != null) {
                                viewModel.updateTask(
                                    existingTask.copy(
                                        title = title,
                                        content = content,
                                        status = status,
                                        priority = priority,
                                        due = parseDateOrNull(dueDate),
                                        scheduled = parseDateOrNull(scheduledDate),
                                        contexts = parseCommaSeparated(contexts),
                                        projects = parseCommaSeparated(projects),
                                        tags = parseCommaSeparated(tags),
                                        timeEstimate = timeEstimate.toIntOrNull(),
                                        recurrence = recurrence.ifBlank { null }
                                    )
                                )
                            } else {
                                viewModel.createTask(
                                    title = title,
                                    content = content,
                                    status = status,
                                    priority = priority,
                                    due = parseDateOrNull(dueDate),
                                    scheduled = parseDateOrNull(scheduledDate),
                                    contexts = parseCommaSeparated(contexts),
                                    projects = parseCommaSeparated(projects),
                                    tags = parseCommaSeparated(tags),
                                    timeEstimate = timeEstimate.toIntOrNull(),
                                    recurrence = recurrence.ifBlank { null }
                                )
                            }
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status and Priority Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showStatusMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(status.value)
                    }
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        TaskStatus.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.value) },
                                onClick = {
                                    status = s
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                // Priority
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showPriorityMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(priority?.displayName ?: "None")
                    }
                    DropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                priority = null
                                showPriorityMenu = false
                            }
                        )
                        Priority.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.displayName) },
                                onClick = {
                                    priority = p
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Dates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text("Scheduled") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.Schedule, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Contexts
            OutlinedTextField(
                value = contexts,
                onValueChange = { contexts = it },
                label = { Text("Contexts") },
                placeholder = { Text("work, personal") },
                leadingIcon = { Icon(Icons.Default.Tag, null) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Comma-separated: @work, @home") }
            )

            // Projects
            OutlinedTextField(
                value = projects,
                onValueChange = { projects = it },
                label = { Text("Projects") },
                placeholder = { Text("Project A, Project B") },
                leadingIcon = { Icon(Icons.Default.Folder, null) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Comma-separated project names") }
            )

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags") },
                placeholder = { Text("important, urgent") },
                leadingIcon = { Icon(Icons.Default.Label, null) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Comma-separated tags") }
            )

            // Time Estimate
            OutlinedTextField(
                value = timeEstimate,
                onValueChange = { timeEstimate = it.filter { char -> char.isDigit() } },
                label = { Text("Time Estimate (minutes)") },
                leadingIcon = { Icon(Icons.Default.Timer, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Recurrence
            OutlinedTextField(
                value = recurrence,
                onValueChange = { recurrence = it },
                label = { Text("Recurrence") },
                placeholder = { Text("FREQ=WEEKLY;BYDAY=MO") },
                leadingIcon = { Icon(Icons.Default.Repeat, null) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("RRULE format (optional)") }
            )

            Divider()

            // Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                maxLines = 15
            )

            // Time tracking section
            if (existingTask != null) {
                Divider()
                TimeTrackingSection(
                    task = existingTask,
                    onStartTimer = { viewModel.startTimer(existingTask.id) },
                    onStopTimer = { viewModel.stopTimer(existingTask.id) }
                )
            }
        }
    }

    if (showDeleteDialog && existingTask != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${existingTask.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(existingTask.id)
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TimeTrackingSection(
    task: Task,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Time Tracking",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total time: ${formatDuration(task.totalTimeSpent)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                task.timeEstimate?.let { estimate ->
                    Text(
                        text = "Estimate: ${formatDuration(estimate * 60L)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (task.isTimerRunning) {
                FilledTonalButton(onClick = onStopTimer) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Timer")
                }
            } else {
                FilledTonalButton(onClick = onStartTimer) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Timer")
                }
            }
        }

        if (task.timeEntries.isNotEmpty()) {
            Text(
                text = "Time Entries: ${task.timeEntries.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseDateOrNull(dateStr: String): LocalDate? {
    return try {
        if (dateStr.isBlank()) null else LocalDate.parse(dateStr.trim())
    } catch (e: Exception) {
        null
    }
}

private fun parseCommaSeparated(input: String): List<String> {
    return input.split(",")
        .map { it.trim().removePrefix("@") }
        .filter { it.isNotBlank() }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
