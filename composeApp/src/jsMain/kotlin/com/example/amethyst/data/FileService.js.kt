package com.example.amethyst.data

// Web/JS doesn't have direct file system access
// Tasks will be stored in localStorage or IndexedDB
actual class FileService {
    actual suspend fun listTaskFiles(directoryPath: String): List<String> {
        // Not supported in browser environment
        return emptyList()
    }

    actual suspend fun readFile(filePath: String): String? {
        // Not supported in browser environment
        return null
    }

    actual suspend fun writeFile(filePath: String, content: String): Boolean {
        // Not supported in browser environment
        return false
    }

    actual suspend fun deleteFile(filePath: String): Boolean {
        // Not supported in browser environment
        return false
    }

    actual suspend fun directoryExists(directoryPath: String): Boolean {
        // Not supported in browser environment
        return false
    }

    actual suspend fun createDirectory(directoryPath: String): Boolean {
        // Not supported in browser environment
        return false
    }
}
