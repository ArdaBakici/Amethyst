package com.example.amethyst.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FileService {
    actual suspend fun listTaskFiles(directoryPath: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(directoryPath)
            if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

            dir.listFiles { file -> file.extension == "md" }
                ?.map { it.absolutePath }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            println("Error listing files: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun readFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            File(filePath).takeIf { it.exists() }?.readText()
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            null
        }
    }

    actual suspend fun writeFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).writeText(content)
            true
        } catch (e: Exception) {
            println("Error writing file: ${e.message}")
            false
        }
    }

    actual suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
            false
        }
    }

    actual suspend fun directoryExists(directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        File(directoryPath).exists() && File(directoryPath).isDirectory
    }

    actual suspend fun createDirectory(directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(directoryPath).mkdirs()
        } catch (e: Exception) {
            println("Error creating directory: ${e.message}")
            false
        }
    }
}
