package com.example.amethyst.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class FileService {
    actual suspend fun listTaskFiles(directoryPath: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val fileManager = NSFileManager.defaultManager
            val dirURL = NSURL.fileURLWithPath(directoryPath)

            val contents = fileManager.contentsOfDirectoryAtURL(
                dirURL,
                includingPropertiesForKeys = null,
                options = NSDirectoryEnumerationSkipsHiddenFiles,
                error = null
            ) as? List<NSURL> ?: return@withContext emptyList()

            contents
                .mapNotNull { it.path }
                .filter { it.endsWith(".md") }
                .sorted()
        } catch (e: Exception) {
            println("Error listing files: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun readFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            NSString.stringWithContentsOfFile(
                filePath,
                encoding = NSUTF8StringEncoding,
                error = null
            )
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            null
        }
    }

    actual suspend fun writeFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            (content as NSString).writeToFile(
                filePath,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
        } catch (e: Exception) {
            println("Error writing file: ${e.message}")
            false
        }
    }

    actual suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            NSFileManager.defaultManager.removeItemAtPath(filePath, error = null)
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
            false
        }
    }

    actual suspend fun directoryExists(directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        var isDirectory = false
        val exists = NSFileManager.defaultManager.fileExistsAtPath(directoryPath, isDirectory = null)
        exists && isDirectory
    }

    actual suspend fun createDirectory(directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            NSFileManager.defaultManager.createDirectoryAtPath(
                directoryPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        } catch (e: Exception) {
            println("Error creating directory: ${e.message}")
            false
        }
    }
}
