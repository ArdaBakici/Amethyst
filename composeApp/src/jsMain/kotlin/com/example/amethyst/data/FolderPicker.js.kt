package com.example.amethyst.data

actual class FolderPicker actual constructor() {
    actual suspend fun pickFolder(): String? {
        // Web doesn't support folder picking in the same way
        return null
    }
}
