package com.example.amethyst.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.amethyst.data.FolderPicker
import kotlinx.coroutines.launch

@Composable
actual fun FolderPickerButton(onFolderSelected: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    IconButton(onClick = {
        scope.launch {
            val folderPicker = FolderPicker()
            folderPicker.pickFolder()?.let { path ->
                onFolderSelected(path)
            }
        }
    }) {
        Icon(Icons.Default.Folder, "Browse")
    }
}
