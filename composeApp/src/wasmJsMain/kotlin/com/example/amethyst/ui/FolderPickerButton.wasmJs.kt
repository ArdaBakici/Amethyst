package com.example.amethyst.ui

import androidx.compose.runtime.Composable

@Composable
actual fun FolderPickerButton(onFolderSelected: (String) -> Unit) {
    // Web doesn't support folder picking - use manual text input
}
