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
                // Parse content URI to extract base and subdirectory path
                val pathParts = filePath.split("/")
                val treeIndex = pathParts.indexOf("tree")

                if (treeIndex == -1 || treeIndex + 1 >= pathParts.size) {
                    return@withContext null
                }

                // Get everything after tree/documentId as the file path
                val filePathParts = pathParts.drop(treeIndex + 2)

                if (filePathParts.isEmpty()) {
                    // Direct file URI, not a subdirectory path
                    return@withContext readFileFromContentUri(filePath)
                }

                // Reconstruct base URI
                val baseUriString = pathParts.take(treeIndex + 2).joinToString("/")
                val relativePath = filePathParts.joinToString("/")

                println("FileService.readFile: filePath = $filePath")
                println("FileService.readFile: baseUri = $baseUriString")
                println("FileService.readFile: relativePath = $relativePath")

                readFileFromSubdirectory(baseUriString, relativePath)
            } else {
                File(filePath).takeIf { it.exists() }?.readText()
            }
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            e.printStackTrace()
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
        try {
            if (directoryPath.startsWith("content://")) {
                // For content URIs, we need to navigate using DocumentFile
                // Check if this is a root URI or has a subdirectory path
                val pathParts = directoryPath.split("/")
                val treeIndex = pathParts.indexOf("tree")

                if (treeIndex == -1 || treeIndex + 1 >= pathParts.size) {
                    return@withContext false
                }

                // Get the document ID part (e.g., "primary%3ASync%2FWayfinder")
                val encodedDocId = pathParts[treeIndex + 1]
                val decodedDocId = java.net.URLDecoder.decode(encodedDocId, "UTF-8")

                // Check if there's a subdirectory path after the tree portion
                val subdirPath = pathParts.drop(treeIndex + 2).joinToString("/")

                // Reconstruct base URI
                val baseUriString = pathParts.take(treeIndex + 2).joinToString("/")
                val baseUri = Uri.parse(baseUriString)

                println("FileService.directoryExists: directoryPath = $directoryPath")
                println("FileService.directoryExists: decodedDocId = $decodedDocId")
                println("FileService.directoryExists: subdirPath = $subdirPath")
                println("FileService.directoryExists: baseUri = $baseUri")

                var currentDir = DocumentFile.fromTreeUri(applicationContext, baseUri) ?: return@withContext false

                // Navigate through subdirectory if specified
                if (subdirPath.isNotEmpty()) {
                    for (part in subdirPath.split("/")) {
                        if (part.isEmpty()) continue
                        println("FileService.directoryExists: Looking for: $part")
                        currentDir = currentDir.findFile(part) ?: run {
                            println("FileService.directoryExists: Not found: $part")
                            currentDir.listFiles().forEach { file ->
                                println("  Available: ${file.name} (isDir: ${file.isDirectory})")
                            }
                            return@withContext false
                        }
                        if (!currentDir.isDirectory) return@withContext false
                    }
                }

                currentDir.exists()
            } else {
                File(directoryPath).exists() && File(directoryPath).isDirectory
            }
        } catch (e: Exception) {
            println("FileService.directoryExists: Error - ${e.message}")
            e.printStackTrace()
            false
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

    /**
     * Helper function to construct proper subdirectory path for content URIs
     * For content URIs, we append to the path, not concatenate with /
     */
    fun getSubdirectoryPath(basePath: String, subdirectory: String): String {
        return if (basePath.startsWith("content://")) {
            // For content URIs, append the subdirectory to the path
            "$basePath/$subdirectory"
        } else {
            // For file paths, use standard concatenation
            "$basePath/$subdirectory"
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
        println("FileService.findSubdirectory: fullPath = $fullPath")

        // Extract base URI and subdirectory path
        val baseUri = fullPath.substringBefore("/.").let { Uri.parse(it) }
        val subdirPath = fullPath.substringAfter("/.")

        println("FileService.findSubdirectory: baseUri = $baseUri")
        println("FileService.findSubdirectory: subdirPath = $subdirPath")

        if (!subdirPath.startsWith(".")) {
            println("FileService.findSubdirectory: subdirPath doesn't start with '.'")
            return null
        }

        // subdirPath already starts with ".", so just split it
        val pathParts = subdirPath.split("/")
        println("FileService.findSubdirectory: pathParts = $pathParts")

        var currentDir = DocumentFile.fromTreeUri(applicationContext, baseUri)
        if (currentDir == null) {
            println("FileService.findSubdirectory: Failed to get DocumentFile from baseUri")
            return null
        }

        println("FileService.findSubdirectory: Starting directory = ${currentDir.name}")

        for (part in pathParts) {
            if (part.isEmpty()) continue
            println("FileService.findSubdirectory: Looking for part: '$part'")

            // Ensure currentDir is non-null (should always be the case here)
            val current = currentDir ?: return null

            val foundDir = current.findFile(part)
            if (foundDir == null) {
                println("FileService.findSubdirectory: Part '$part' not found")
                // List what files actually exist
                current.listFiles().forEach { file ->
                    println("  Available: ${file.name} (isDir: ${file.isDirectory})")
                }
                return null
            }

            if (!foundDir.isDirectory) {
                println("FileService.findSubdirectory: Part '$part' is not a directory")
                return null
            }

            currentDir = foundDir
            println("FileService.findSubdirectory: Found directory: ${currentDir.name}")
        }

        println("FileService.findSubdirectory: Successfully found all subdirectories")
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
