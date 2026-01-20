package com.example.amethyst.data

actual class FolderPicker actual constructor() {
    actual suspend fun pickFolder(): String? {
        // Android implementation would use Intent.ACTION_OPEN_DOCUMENT_TREE
        // For now, returning null to allow manual input
        // Full implementation requires Activity context
        return null
    }
}
