package com.example.amethyst.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TaskNotesConfig(
    val tasksFolder: String = ""
)

data class VaultValidationResult(
    val isValid: Boolean,
    val tasksFolder: String = "",
    val errorMessage: String? = null
)

class VaultValidator(private val fileService: FileService) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Validates if the given path is a valid Obsidian vault and extracts TaskNotes configuration
     */
    suspend fun validateVault(vaultRootPath: String): VaultValidationResult {
        if (vaultRootPath.isBlank()) {
            return VaultValidationResult(
                isValid = false,
                errorMessage = "Vault path cannot be empty"
            )
        }

        // Check if .obsidian folder exists
        val obsidianFolderPath = "$vaultRootPath/.obsidian"

        val obsidianExists = fileService.directoryExists(obsidianFolderPath)
        if (!obsidianExists) {
            return VaultValidationResult(
                isValid = false,
                errorMessage = "Not a valid Obsidian vault (.obsidian folder not found)"
            )
        }

        // Try to read TaskNotes configuration
        val dataJsonPath = "$vaultRootPath/.obsidian/plugins/tasknotes/data.json"

        val configContent = fileService.readFile(dataJsonPath)
        if (configContent == null) {
            return VaultValidationResult(
                isValid = true,
                tasksFolder = "",
                errorMessage = "TaskNotes plugin not configured. Using vault root for tasks."
            )
        }

        return try {
            val config = json.decodeFromString<TaskNotesConfig>(configContent)
            VaultValidationResult(
                isValid = true,
                tasksFolder = config.tasksFolder.ifBlank { "" }
            )
        } catch (e: Exception) {
            println("Error parsing TaskNotes config: ${e.message}")
            VaultValidationResult(
                isValid = true,
                tasksFolder = "",
                errorMessage = "Could not parse TaskNotes config. Using vault root for tasks."
            )
        }
    }
}
