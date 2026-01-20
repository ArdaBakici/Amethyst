package com.example.amethyst.ui

import androidx.compose.runtime.Composable

@Composable
actual fun FolderPickerButton(onFolderSelected: (String) -> Unit) {
    // iOS folder picker not implemented - use manual text input
    // Could be implemented with UIDocumentPickerViewController
}
