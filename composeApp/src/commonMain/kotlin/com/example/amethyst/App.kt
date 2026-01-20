package com.example.amethyst

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amethyst.data.*
import com.example.amethyst.ui.screens.SettingsScreen
import com.example.amethyst.ui.screens.TaskDetailScreen
import com.example.amethyst.ui.screens.TaskListScreen
import com.example.amethyst.ui.theme.AmethystTheme
import com.example.amethyst.ui.theme.ThemeState
import com.example.amethyst.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun App() {
    val isDarkMode by ThemeState.isDarkMode
    val preferences = remember { Preferences.instance }
    val vaultPath by preferences.vaultPath.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Navigation state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TaskList) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    // Initialize repository and ViewModel
    val repository = remember(vaultPath) {
        if (vaultPath.isNotBlank()) {
            val fileService = FileService()
            val repo = TaskRepository(fileService, vaultPath)
            coroutineScope.launch {
                repo.loadTasks()
            }
            repo
        } else {
            null
        }
    }

    val viewModel: TaskViewModel? = repository?.let { repo ->
        remember(repo) {
            TaskViewModel(repo)
        }
    }

    AmethystTheme(darkTheme = isDarkMode) {
        when {
            vaultPath.isBlank() -> {
                // Show settings screen if vault path not configured
                SettingsScreen(
                    vaultPath = vaultPath,
                    onVaultPathChange = { preferences.setVaultPath(it) },
                    onNavigateBack = { /* No back navigation from initial setup */ },
                    onPickFolder = {
                        coroutineScope.launch {
                            val folderPicker = FolderPicker()
                            folderPicker.pickFolder()?.let { path ->
                                preferences.setVaultPath(path)
                            }
                        }
                    }
                )
            }
            viewModel != null -> {
                when (val screen = currentScreen) {
                    is Screen.TaskList -> {
                        TaskListScreen(
                            viewModel = viewModel,
                            onTaskClick = { task ->
                                selectedTaskId = task.id
                                currentScreen = Screen.TaskDetail
                            },
                            onAddTask = {
                                selectedTaskId = null
                                currentScreen = Screen.TaskDetail
                            },
                            onToggleTheme = {
                                ThemeState.toggleTheme()
                            }
                        )
                    }
                    is Screen.TaskDetail -> {
                        TaskDetailScreen(
                            viewModel = viewModel,
                            taskId = selectedTaskId,
                            onNavigateBack = {
                                currentScreen = Screen.TaskList
                                selectedTaskId = null
                            }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            vaultPath = vaultPath,
                            onVaultPathChange = { preferences.setVaultPath(it) },
                            onNavigateBack = {
                                currentScreen = Screen.TaskList
                            },
                            onPickFolder = {
                                coroutineScope.launch {
                                    val folderPicker = FolderPicker()
                                    folderPicker.pickFolder()?.let { path ->
                                        preferences.setVaultPath(path)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object TaskList : Screen()
    object TaskDetail : Screen()
    object Settings : Screen()
}
