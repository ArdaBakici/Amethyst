package com.example.amethyst.data

/**
 * Platform-specific file operations interface
 */
expect class FileService {
    /**
     * Lists all .md files in the specified directory
     */
    suspend fun listTaskFiles(directoryPath: String): List<String>

    /**
     * Reads the content of a file
     */
    suspend fun readFile(filePath: String): String?

    /**
     * Writes content to a file
     */
    suspend fun writeFile(filePath: String, content: String): Boolean

    /**
     * Deletes a file
     */
    suspend fun deleteFile(filePath: String): Boolean

    /**
     * Checks if a directory exists
     */
    suspend fun directoryExists(directoryPath: String): Boolean

    /**
     * Creates a directory if it doesn't exist
     */
    suspend fun createDirectory(directoryPath: String): Boolean
}
