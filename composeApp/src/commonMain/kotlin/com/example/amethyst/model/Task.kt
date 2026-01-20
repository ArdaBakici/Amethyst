package com.example.amethyst.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Task(
    val id: String, // Filename without extension
    val title: String,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: Priority? = null,
    val due: LocalDate? = null,
    val scheduled: LocalDate? = null,
    val contexts: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val timeEstimate: Int? = null, // in minutes
    val timeEntries: List<TimeEntry> = emptyList(),
    val completedDate: LocalDate? = null,
    val recurrence: String? = null, // RRULE format
    val completeInstances: List<LocalDate> = emptyList(),
    val content: String = "", // Markdown body content
    val createdAt: Instant? = null,
    val modifiedAt: Instant? = null
) {
    val totalTimeSpent: Long
        get() = timeEntries.mapNotNull { it.duration }.sum()

    val isOverdue: Boolean
        get() = due?.let { dueDate ->
            val today = kotlinx.datetime.Clock.System.now().toString().take(10)
            dueDate.toString() < today && status != TaskStatus.DONE
        } ?: false

    val activeTimeEntry: TimeEntry?
        get() = timeEntries.lastOrNull { it.endTime == null }

    val isTimerRunning: Boolean
        get() = activeTimeEntry != null
}
