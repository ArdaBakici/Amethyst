package com.example.amethyst.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FileService actual constructor() {

    // We need Context to work with DocumentFile
    // For now, this will be set from MainActivity
    companion object {
        lateinit var applicationContext: Context
    }

    actual suspend fun listTaskFiles(directoryPath: String): List<String> = withContext(Dispatchers.IO) {
        try {
            if (directoryPath.startsWith("content://")) {
                // Check if this is a subdirectory path
                if (directoryPath.contains("/") && !directoryPath.endsWith("/tree/primary:")) {
                    // Extract vault root and subdirectory
                    val parts = directoryPath.split("/tree/")
                    if (parts.size == 2) {
                        val baseUri = parts[0] + "/tree/" + parts[1].substringBefore('/')
                        val subdirPath = parts[1].substringAfter('/', "")
                        if (subdirPath.isNotEmpty()) {
                            listTaskFilesFromSubdirectory(baseUri, subdirPath)
                        } else {
                            listTaskFilesFromContentUri(directoryPath)
                        }
                    } else {
                        listTaskFilesFromContentUri(directoryPath)
                    }
                } else {
                    listTaskFilesFromContentUri(directoryPath)
                }
            } else {
                listTaskFilesFromPath(directoryPath)
            }
        } catch (e: Exception) {
            println("Error listing files: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun readFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://")) {
                // Check if this is a subdirectory file path (contains /. pattern)
                if (filePath.contains("/.")) {
                    val vaultRoot = filePath.substringBefore("/.")
                    val relativePath = filePath.substringAfter(vaultRoot + "/")
                    readFileFromSubdirectory(vaultRoot, relativePath)
                } else {
                    readFileFromContentUri(filePath)
                }
            } else {
                File(filePath).takeIf { it.exists() }?.readText()
            }
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            null
        }
    }

    actual suspend fun writeFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://")) {
                writeFileToContentUri(filePath, content)
            } else {
                File(filePath).writeText(content)
                true
            }
        } catch (e: Exception) {
            println("Error writing file: ${e.message}")
            false
        }
    }

    actual suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://")) {
                deleteFileFromContentUri(filePath)
            } else {
                File(filePath).delete()
            }
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
            false
        }
    }

    actual suspend fun directoryExists(directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        if (directoryPath.startsWith("content://")) {
            // For content URIs, check if the directory exists
            // If it's the root URI, check directly
            if (!directoryPath.contains("/.")) {
                val uri = Uri.parse(directoryPath)
                val docFile = DocumentFile.fromTreeUri(applicationContext, uri)
                docFile?.exists() ?: false
            } else {
                // For subdirectories like .obsidian, we need to navigate
                findSubdirectory(directoryPath) != null
            }
        } else {
            File(directoryPath).exists() && File(directoryPath).isDirectory
        }
    }

    actual suspend fun createDirectory(directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (directoryPath.startsWith("content://")) {
                // Can't create arbitrary directories in content URIs
                false
            } else {
                File(directoryPath).mkdirs()
            }
        } catch (e: Exception) {
            println("Error creating directory: ${e.message}")
            false
        }
    }

    // Content URI helper methods
    private fun listTaskFilesFromContentUri(uriString: String): List<String> {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromTreeUri(applicationContext, uri) ?: return emptyList()

        return docFile.listFiles()
            .filter { it.isFile && it.name?.endsWith(".md") == true }
            .mapNotNull { it.uri.toString() }
            .sorted()
    }

    private fun listTaskFilesFromPath(directoryPath: String): List<String> {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { file -> file.extension == "md" }
            ?.map { it.absolutePath }
            ?.sorted()
            ?: emptyList()
    }

    private fun readFileFromContentUri(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    }

    private fun writeFileToContentUri(uriString: String, content: String): Boolean {
        return try {
            // Check if this is a directory URI + filename pattern (for new files)
            if (uriString.contains("/tree/") && uriString.substringAfterLast('/').endsWith(".md")) {
                // Extract directory URI and filename
                val parts = uriString.split("/tree/")
                if (parts.size == 2) {
                    val baseUri = parts[0] + "/tree/" + parts[1].substringBefore('/')
                    val filename = parts[1].substringAfterLast('/')

                    val dirUri = Uri.parse(baseUri)
                    val dirDocFile = DocumentFile.fromTreeUri(applicationContext, dirUri)

                    if (dirDocFile != null) {
                        // Check if file already exists
                        val existingFile = dirDocFile.findFile(filename)
                        val fileUri = if (existingFile != null) {
                            existingFile.uri
                        } else {
                            // Create new file
                            dirDocFile.createFile("text/markdown", filename)?.uri
                        }

                        fileUri?.let { uri ->
                            applicationContext.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                                outputStream.bufferedWriter().use { it.write(content) }
                            }
                            return true
                        }
                    }
                }
                false
            } else {
                // Existing file URI
                val uri = Uri.parse(uriString)
                applicationContext.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.bufferedWriter().use { it.write(content) }
                }
                true
            }
        } catch (e: Exception) {
            println("Error writing to content URI: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun deleteFileFromContentUri(uriString: String): Boolean {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromSingleUri(applicationContext, uri)
        return docFile?.delete() ?: false
    }

    /**
     * Finds a subdirectory within a content URI path
     * For paths like "content://.../.obsidian/plugins/tasknotes"
     */
    private fun findSubdirectory(fullPath: String): DocumentFile? {
        // Extract base URI and subdirectory path
        val baseUri = fullPath.substringBefore("/.").let { Uri.parse(it) }
        val subdirPath = fullPath.substringAfter("/.")
        if (!subdirPath.startsWith(".")) return null

        // subdirPath already starts with ".", so just split it
        val pathParts = subdirPath.split("/")

        var currentDir = DocumentFile.fromTreeUri(applicationContext, baseUri) ?: return null

        for (part in pathParts) {
            if (part.isEmpty()) continue
            currentDir = currentDir.findFile(part) ?: return null
            if (!currentDir.isDirectory) return null
        }

        return currentDir
    }

    /**
     * Reads a file from a subdirectory path like ".obsidian/plugins/tasknotes/data.json"
     */
    private fun readFileFromSubdirectory(vaultRootUri: String, relativePath: String): String? {
        val pathParts = relativePath.split("/")
        val fileName = pathParts.last()
        val dirPath = pathParts.dropLast(1).joinToString("/")

        val baseUri = Uri.parse(vaultRootUri)
        var currentDir = DocumentFile.fromTreeUri(applicationContext, baseUri) ?: return null

        // Navigate to the directory
        for (part in dirPath.split("/")) {
            if (part.isEmpty()) continue
            currentDir = currentDir.findFile(part) ?: return null
            if (!currentDir.isDirectory) return null
        }

        // Find and read the file
        val file = currentDir.findFile(fileName) ?: return null
        return applicationContext.contentResolver.openInputStream(file.uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    }

    /**
     * Lists task files from a subdirectory within a content URI
     */
    private fun listTaskFilesFromSubdirectory(vaultRootUri: String, relativePath: String): List<String> {
        val baseUri = Uri.parse(vaultRootUri)
        var currentDir = DocumentFile.fromTreeUri(applicationContext, baseUri) ?: return emptyList()

        // Navigate to the subdirectory
        for (part in relativePath.split("/")) {
            if (part.isEmpty()) continue
            currentDir = currentDir.findFile(part) ?: return emptyList()
            if (!currentDir.isDirectory) return emptyList()
        }

        // List markdown files in the subdirectory
        return currentDir.listFiles()
            .filter { it.isFile && it.name?.endsWith(".md") == true }
            .mapNotNull { it.uri.toString() }
            .sorted()
    }
}
