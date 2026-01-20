package com.example.amethyst.ui

import androidx.compose.runtime.Composable

/**
 * Platform-specific folder picker button
 */
@Composable
expect fun FolderPickerButton(onFolderSelected: (String) -> Unit)
