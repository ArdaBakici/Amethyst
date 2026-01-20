package com.example.amethyst.model

enum class TaskStatus(val value: String) {
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    DONE("done"),
    CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: TODO
        }
    }
}
