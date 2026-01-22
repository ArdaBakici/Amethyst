package com.example.amethyst.data

import com.example.amethyst.model.Priority
import com.example.amethyst.model.Task
import com.example.amethyst.model.TaskStatus
import com.example.amethyst.model.TimeEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

object TaskSerializer {

    /**
     * Parses a TaskNotes compatible markdown file into a Task object
     */
    fun parseTask(filename: String, content: String): Task? {
        try {
            val (frontmatter, body) = parseFrontmatter(content)

            val id = filename.removeSuffix(".md")
            val title = frontmatter["title"] as? String ?: id
            val status = (frontmatter["status"] as? String)?.let { TaskStatus.fromString(it) } ?: TaskStatus.TODO
            val priority = (frontmatter["priority"] as? String)?.let { Priority.fromString(it) }
            val due = (frontmatter["due"] as? String)?.let { parseDate(it) }
            val scheduled = (frontmatter["scheduled"] as? String)?.let { parseDate(it) }
            val contexts = parseList(frontmatter["contexts"])
            val projects = parseList(frontmatter["projects"])
            val tags = parseList(frontmatter["tags"])
            val timeEstimate = (frontmatter["timeEstimate"] as? Number)?.toInt()
            val timeEntries = parseTimeEntries(frontmatter["timeEntries"])
            val completedDate = (frontmatter["completedDate"] as? String)?.let { parseDate(it) }
            val recurrence = frontmatter["recurrence"] as? String
            val completeInstances = parseList(frontmatter["completeInstances"]).mapNotNull { parseDate(it) }
            val createdAt = (frontmatter["createdAt"] as? String)?.let { Instant.parse(it) }
            val modifiedAt = (frontmatter["modifiedAt"] as? String)?.let { Instant.parse(it) }

            return Task(
                id = id,
                title = title,
                status = status,
                priority = priority,
                due = due,
                scheduled = scheduled,
                contexts = contexts,
                projects = projects,
                tags = tags,
                timeEstimate = timeEstimate,
                timeEntries = timeEntries,
                completedDate = completedDate,
                recurrence = recurrence,
                completeInstances = completeInstances,
                content = body,
                createdAt = createdAt,
                modifiedAt = modifiedAt
            )
        } catch (e: Exception) {
            println("Error parsing task: ${e.message}")
            return null
        }
    }

    /**
     * Serializes a Task object to TaskNotes compatible markdown format
     */
    fun serializeTask(task: Task): String {
        val yaml = buildString {
            appendLine("---")
            appendLine("title: ${escapeYamlString(task.title)}")
            appendLine("status: ${task.status.value}")

            task.priority?.let { appendLine("priority: ${it.value}") }
            task.due?.let { appendLine("due: $it") }
            task.scheduled?.let { appendLine("scheduled: $it") }

            if (task.contexts.isNotEmpty()) {
                appendLine("contexts:")
                task.contexts.forEach { appendLine("  - $it") }
            }

            if (task.projects.isNotEmpty()) {
                appendLine("projects:")
                task.projects.forEach { appendLine("  - $it") }
            }

            if (task.tags.isNotEmpty()) {
                appendLine("tags:")
                task.tags.forEach { appendLine("  - $it") }
            }

            task.timeEstimate?.let { appendLine("timeEstimate: $it") }

            if (task.timeEntries.isNotEmpty()) {
                appendLine("timeEntries:")
                task.timeEntries.forEach { entry ->
                    appendLine("  - startTime: ${entry.startTime}")
                    entry.endTime?.let { appendLine("    endTime: $it") }
                }
            }

            task.completedDate?.let { appendLine("completedDate: $it") }
            task.recurrence?.let { appendLine("recurrence: ${escapeYamlString(it)}") }

            if (task.completeInstances.isNotEmpty()) {
                appendLine("completeInstances:")
                task.completeInstances.forEach { appendLine("  - $it") }
            }

            task.createdAt?.let { appendLine("createdAt: $it") }
            task.modifiedAt?.let { appendLine("modifiedAt: $it") }

            appendLine("---")
        }

        return yaml + "\n" + task.content
    }

    private fun parseFrontmatter(content: String): Pair<Map<String, Any>, String> {
        val lines = content.lines()
        if (lines.isEmpty() || !lines[0].trim().startsWith("---")) {
            return emptyMap<String, Any>() to content
        }

        val endIndex = lines.drop(1).indexOfFirst { it.trim().startsWith("---") }
        if (endIndex == -1) {
            return emptyMap<String, Any>() to content
        }

        val yamlLines = lines.subList(1, endIndex + 1)
        val bodyStartIndex = endIndex + 2
        val body = if (bodyStartIndex < lines.size) {
            lines.subList(bodyStartIndex, lines.size).joinToString("\n").trim()
        } else {
            ""
        }

        val frontmatter = parseYaml(yamlLines)
        return frontmatter to body
    }

    private fun parseYaml(lines: List<String>): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        var currentKey: String? = null
        var currentList = mutableListOf<Any>()
        var inListContext = false
        var currentMap = mutableMapOf<String, String>()
        var inMapContext = false

        for (line in lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue

            when {
                line.startsWith("  - ") && inMapContext -> {
                    // Map item in list
                    val parts = line.substring(4).split(":", limit = 2)
                    if (parts.size == 2) {
                        currentMap[parts[0].trim()] = parts[1].trim()
                    }
                }
                line.startsWith("    ") && inMapContext -> {
                    // Continuation of map
                    val parts = line.trim().split(":", limit = 2)
                    if (parts.size == 2) {
                        currentMap[parts[0].trim()] = parts[1].trim()
                    }
                }
                line.startsWith("  - ") -> {
                    if (inMapContext) {
                        currentList.add(currentMap.toMap())
                        currentMap = mutableMapOf()
                    }
                    // List item
                    val value = line.substring(4).trim()
                    if (value.contains(":")) {
                        // Start of a map in list
                        inMapContext = true
                        val parts = value.split(":", limit = 2)
                        currentMap[parts[0].trim()] = parts[1].trim()
                    } else {
                        currentList.add(value)
                    }
                }
                line.contains(":") && !line.startsWith(" ") -> {
                    // Save previous list if exists
                    if (inListContext && currentKey != null) {
                        if (inMapContext && currentMap.isNotEmpty()) {
                            currentList.add(currentMap.toMap())
                            currentMap = mutableMapOf()
                        }
                        map[currentKey] = currentList.toList()
                        currentList = mutableListOf()
                        inListContext = false
                        inMapContext = false
                    }

                    // New key-value pair
                    val parts = line.split(":", limit = 2)
                    currentKey = parts[0].trim()
                    val value = if (parts.size > 1) parts[1].trim() else ""

                    if (value.isEmpty()) {
                        inListContext = true
                    } else {
                        map[currentKey] = parseValue(value)
                        currentKey = null
                    }
                }
            }
        }

        // Save last list if exists
        if (inListContext && currentKey != null) {
            if (inMapContext && currentMap.isNotEmpty()) {
                currentList.add(currentMap.toMap())
            }
            map[currentKey] = currentList.toList()
        }

        return map
    }

    private fun parseValue(value: String): Any {
        return when {
            value == "true" -> true
            value == "false" -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            else -> value.removeSurrounding("\"").removeSurrounding("'")
        }
    }

    private fun parseList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> listOf(value)
            else -> emptyList()
        }
    }

    private fun parseTimeEntries(value: Any?): List<TimeEntry> {
        return when (value) {
            is List<*> -> value.mapNotNull {
                (it as? Map<*, *>)?.let { map ->
                    val stringMap = map
                        .mapKeys { e -> e.key.toString() }
                        .filterValues { v -> v != null }
                        .mapValues { e -> e.value!! }
                    TimeEntry.fromMap(stringMap)
                }
            }
            else -> emptyList()
        }
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value)
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeYamlString(value: String): String {
        return if (value.contains(":") || value.contains("#") || value.contains("\"")) {
            "\"${value.replace("\"", "\\\"")}\""
        } else {
            value
        }
    }
}
