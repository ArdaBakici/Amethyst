package com.example.amethyst.data

actual class FolderPicker {
    actual suspend fun pickFolder(): String? {
        // iOS implementation would use UIDocumentPickerViewController
        // For now, returning null to allow manual input
        return null
    }
}
