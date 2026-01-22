package com.example.amethyst.model

enum class Priority(val value: String, val displayName: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High"),
    URGENT("urgent", "Urgent");

    companion object {
        fun fromString(value: String): Priority {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
