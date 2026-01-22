package com.example.amethyst.data

/**
 * Platform-specific folder picker interface
 */
expect class FolderPicker() {
    suspend fun pickFolder(): String?
}
